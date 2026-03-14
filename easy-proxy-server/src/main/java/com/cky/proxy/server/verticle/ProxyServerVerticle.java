package com.cky.proxy.server.verticle;

import java.util.Calendar;
import java.util.List;
import java.util.Set;

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
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.NetSocket;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProxyServerVerticle extends AbstractVerticle {
    private ProxyClientService proxyClientService = BeanContext.getProxyClientService();
    private ProxyClientRuleService proxyClientRuleService = BeanContext.getProxyClientRuleService();

    @Override
    public void start(Promise<Void> startPromise) {
        // 注册事件订阅
        eventBusSubscribe();

        ServerProperty server = ConfigProperty.getInstance().getServer();
        Integer proxyPort = server.getProxyPort();
        log.info("Init client server {}", proxyPort);
        NetServerOptions options = new NetServerOptions()
                .setPort(proxyPort)
                .setSsl(true)
                .setUseAlpn(true)
                .setKeyCertOptions(new JksOptions().setPath(CertGenerator.getJksCertPath())
                        .setPassword(CertGenerator.getCertPassword()));
        vertx.createNetServer(options)
                .connectHandler(new ClientSocketHandler(vertx))
                .listen(proxyPort)
                .onFailure(t -> log.error("Server start failed", t))
                .onSuccess(v -> log.info("Server started on port {}", proxyPort));

        initRuleServers();

        // 启动流量统计定时任务 (每小时的59分59秒执行一次)
        startTrafficStatisticsTask();
    }

    @Override
    public void stop() {
        log.info("Server stopping, flush traffic stats...");
        TrafficStatisticManager.flush();
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
        // 关闭规则端口监听
        NetServer server = RuleListenSocketManager.removeRuleListenSocket(ruleId);
        if (server == null) {
            return;
        }
        server.close(res -> {
            if (res.succeeded()) {
                log.info("Stopped server for rule {}", ruleId);
            } else {
                log.error("Failed to stop server for rule {}", ruleId, res.cause());
            }
            // TODO 根据规则关闭用户连接通道 Repeat
            Set<String> userIds = RuleListenSocketManager.getOnlineUsers(ruleId);
            if (userIds != null) {
                for (String userId : userIds) {
                    NetSocket dataSocket = ClientDataSocketManager.getDataSocket(userId);
                    if (dataSocket != null) {
                        dataSocket.close();
                    }
                    ClientDataSocketManager.closeDataSocket(userId);
                    NetSocket proxySocket = RuleListenSocketManager.getProxySocket(userId);
                    if (proxySocket != null) {
                        proxySocket.close();
                    }
                    RuleListenSocketManager.userConnectionClose(userId);
                }
            }
            if (completionHandler != null) {
                completionHandler.run();
            }
        });
        
        
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
        vertx.createNetServer()
                .connectHandler(new UserProxySocketHandler(client, rule))
                .exceptionHandler(e -> {
                    log.error("Failed listening for rule {} on port {}", rule.getName(), rule.getServerPort(), e);
                }).listen(rule.getServerPort(), res -> {
                    NetServer serverSocket = res.result();
                    if (res.succeeded()) {
                        log.info("Listening for rule {} on port {}", rule.getName(), rule.getServerPort());
                        RuleListenSocketManager.addRuleListenSocket(rule.getId(), serverSocket);
                    } else {
                        log.error("Failed listening for rule {}", rule.getName(), res.cause());
                    }
                });
        log.debug("EP>> Init rule {} {} -> {}", rule.getName(), rule.getServerPort(), rule.getClientAddress());
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
