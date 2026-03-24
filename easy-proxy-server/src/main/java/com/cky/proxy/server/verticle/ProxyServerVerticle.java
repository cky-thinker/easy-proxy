package com.cky.proxy.server.verticle;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import com.cky.proxy.server.config.ConfigProperty;
import com.cky.proxy.server.config.ServerProperty;
import com.cky.proxy.server.domain.entity.ProxyClient;
import com.cky.proxy.server.domain.entity.ProxyClientRule;
import com.cky.proxy.server.service.ProxyClientRuleService;
import com.cky.proxy.server.service.ProxyClientService;
import com.cky.proxy.server.socket.ClientSocketHandler;
import com.cky.proxy.server.socket.UserProxySocketHandler;
import com.cky.proxy.server.socket.manager.ClientDataSocketManager;
import com.cky.proxy.server.socket.manager.RuleListenSocketManager;
import com.cky.proxy.server.socket.manager.TrafficStatisticManager;
import com.cky.proxy.server.util.BeanContext;
import com.cky.proxy.server.util.CertGenerator;
import com.cky.proxy.server.util.EventBusUtil;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProxyServerVerticle extends AbstractVerticle {
    private ProxyClientService proxyClientService = BeanContext.getProxyClientService();
    private ProxyClientRuleService proxyClientRuleService = BeanContext.getProxyClientRuleService();

    private ServerSocket mainServerSocket;

    @Override
    public void start(Promise<Void> startPromise) {
        // 注册事件订阅
        eventBusSubscribe();

        ServerProperty server = ConfigProperty.getInstance().getServer();
        Integer proxyPort = server.getProxyPort();
        log.info("Init client server {}", proxyPort);
        
        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            char[] password = CertGenerator.getCertPassword().toCharArray();
            try (FileInputStream fis = new FileInputStream(CertGenerator.getJksCertPath())) {
                ks.load(fis, password);
            }
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, password);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), null, null);
            SSLServerSocketFactory ssf = sslContext.getServerSocketFactory();
            
            mainServerSocket = ssf.createServerSocket(proxyPort);
            
            Thread.ofVirtual().start(() -> {
                log.info("Server started on port {}", proxyPort);
                while (!mainServerSocket.isClosed()) {
                    try {
                        Socket clientSocket = mainServerSocket.accept();
                        Thread.ofVirtual().start(new ClientSocketHandler(clientSocket));
                    } catch (IOException e) {
                        if (!mainServerSocket.isClosed()) {
                            log.error("Server accept failed", e);
                        }
                    }
                }
            });
            startPromise.complete();
        } catch (Exception e) {
            log.error("Server start failed", e);
            startPromise.fail(e);
            return;
        }

        initRuleServers();

        // 启动流量统计定时任务 (每小时的59分59秒执行一次)
        startTrafficStatisticsTask();
    }

    @Override
    public void stop() {
        log.info("Server stopping, flush traffic stats...");
        TrafficStatisticManager.flush();
        if (mainServerSocket != null) {
            try {
                mainServerSocket.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private void startTrafficStatisticsTask() {
        Calendar calendar = Calendar.getInstance();
        long now = System.currentTimeMillis();
        calendar.setTimeInMillis(now);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 0);

        long nextTime = calendar.getTimeInMillis();
        if (nextTime <= now) {
            nextTime += 3600000L;
        }
        long delay = nextTime - now;

        log.info("Traffic statistics task will start in {} ms", delay);
        vertx.setTimer(delay, id -> {
            TrafficStatisticManager.flush();
            // 之后每小时执行一次
            vertx.setPeriodic(3600000L, timerId -> {
                TrafficStatisticManager.flush();
            });
        });
    }

    private void initRuleServers() {
        List<ProxyClient> proxyClients = proxyClientService.getProxyClients();
        for (ProxyClient proxyClient : proxyClients) {
            if (Boolean.TRUE.equals(proxyClient.getEnableFlag())) {
                log.debug("EP>> Init client {} ", proxyClient.getName());
                List<ProxyClientRule> proxyClientRules = proxyClientRuleService
                        .getProxyClientRules(proxyClient.getId());
                for (ProxyClientRule proxyRule : proxyClientRules) {
                    if (Boolean.TRUE.equals(proxyRule.getEnableFlag())) {
                        startServerForRule(proxyClient, proxyRule);
                    }
                }
            }
        }
    }

    private void eventBusSubscribe() {
        EventBusUtil.subscribe(EventBusUtil.DB_RULE_UPDATE, msg -> {
            Integer ruleId = (Integer) msg.body();
            updateRuleServer(ruleId);
        });

        EventBusUtil.subscribe(EventBusUtil.DB_RULE_ADD, msg -> {
            Integer ruleId = (Integer) msg.body();
            updateRuleServer(ruleId);
        });
        EventBusUtil.subscribe(EventBusUtil.DB_RULE_DISABLE, msg -> {
            Integer ruleId = (Integer) msg.body();
            stopRuleServer(ruleId, null);
        });
        EventBusUtil.subscribe(EventBusUtil.DB_RULE_DELETE, msg -> {
            Integer ruleId = (Integer) msg.body();
            stopRuleServer(ruleId, null);
        });
        EventBusUtil.subscribe(EventBusUtil.DB_CLIENT_UPDATE, msg -> {
            Integer clientId = (Integer) msg.body();
            updateClientServers(clientId);
        });
        EventBusUtil.subscribe(EventBusUtil.DB_CLIENT_ADD, msg -> {
            Integer clientId = (Integer) msg.body();
            updateClientServers(clientId);
        });
        EventBusUtil.subscribe(EventBusUtil.DB_CLIENT_DISABLE, msg -> {
            Integer clientId = (Integer) msg.body();
            stopClientServers(clientId);
        });
        EventBusUtil.subscribe(EventBusUtil.DB_CLIENT_DELETE, msg -> {
            Integer clientId = (Integer) msg.body();
            stopClientServers(clientId);
        });
    }

    private void stopRuleServer(Integer ruleId, Runnable completionHandler) {
        // Delete bandwidth limit
        TrafficStatisticManager.deleteRateLimit(ruleId);
        // TODO 根据规则关闭用户连接通道 Repeat
        Set<String> userIds = RuleListenSocketManager.getOnlineUsers(ruleId);
        if (userIds != null) {
            for (String userId : userIds) {
                Socket dataSocket = ClientDataSocketManager.getDataSocket(userId);
                if (dataSocket != null) {
                    try {
                        dataSocket.close();
                    } catch (IOException e) {}
                }
                ClientDataSocketManager.closeDataSocket(userId);
                Socket proxySocket = RuleListenSocketManager.getProxySocket(userId);
                if (proxySocket != null) {
                    try {
                        proxySocket.close();
                    } catch (IOException e) {}
                }
                RuleListenSocketManager.userConnectionClose(userId);
            }
        }
        // 关闭规则端口监听
        ServerSocket server = RuleListenSocketManager.removeRuleListenSocket(ruleId);
        if (server == null) {
            if (completionHandler != null) {
                completionHandler.run();
            }
            return;
        }
        try {
            server.close();
            log.info("Stopped server for rule {}", ruleId);
        } catch (IOException e) {
            log.error("Failed to stop server for rule {}", ruleId, e);
        }
        if (completionHandler != null) {
            completionHandler.run();
        }
    }

    private void updateRuleServer(Integer ruleId) {
        stopRuleServer(ruleId, () -> {
            ProxyClientRule rule = proxyClientRuleService.getProxyClientRuleById(ruleId);
            if (rule == null || !Boolean.TRUE.equals(rule.getEnableFlag())) {
                return;
            }
            ProxyClient client = proxyClientService.getProxyClientById(rule.getProxyClientId());
            if (client == null || !Boolean.TRUE.equals(client.getEnableFlag())) {
                return;
            }
            startServerForRule(client, rule);
        });
    }

    private void startServerForRule(ProxyClient client, ProxyClientRule rule) {
        try {
            ServerSocket serverSocket = new ServerSocket(rule.getServerPort());
            log.info("Listening for rule {} on port {}", rule.getName(), rule.getServerPort());
            RuleListenSocketManager.addRuleListenSocket(rule.getId(), serverSocket);
            // Update bandwidth limit
            TrafficStatisticManager.initRateLimit(client.getId(), rule.getId(), rule.getLimitRate());

            Thread.ofVirtual().start(() -> {
                while (!serverSocket.isClosed()) {
                    try {
                        Socket userSocket = serverSocket.accept();
                        Thread.ofVirtual().start(new UserProxySocketHandler(client, rule, userSocket));
                    } catch (IOException e) {
                        if (!serverSocket.isClosed()) {
                            log.error("Failed listening for rule {} on port {}", rule.getName(), rule.getServerPort(), e);
                        }
                    }
                }
            });
            log.debug("EP>> Init rule {} {} -> {}", rule.getName(), rule.getServerPort(), rule.getClientAddress());
        } catch (IOException e) {
            log.error("Failed listening for rule {}", rule.getName(), e);
        }
    }

    private void updateClientServers(Integer clientId) {
        ProxyClient client = proxyClientService.getProxyClientById(clientId);
        if (client == null) {
            return;
        }
        List<ProxyClientRule> rules = proxyClientRuleService.getProxyClientRules(clientId);
        if (!Boolean.TRUE.equals(client.getEnableFlag())) {
            for (ProxyClientRule rule : rules) {
                stopRuleServer(rule.getId(), null);
            }
        } else {
            for (ProxyClientRule rule : rules) {
                updateRuleServer(rule.getId());
            }
        }
    }

    private void stopClientServers(Integer clientId) {
        proxyClientRuleService.getProxyClientRules(clientId).forEach(rule -> {
            stopRuleServer(rule.getId(), null);
        });
    }

}
