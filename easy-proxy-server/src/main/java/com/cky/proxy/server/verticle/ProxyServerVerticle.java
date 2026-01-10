package com.cky.proxy.server.verticle;

import com.cky.proxy.server.config.ConfigProperty;
import com.cky.proxy.server.config.ServerProperty;
import com.cky.proxy.server.domain.entity.ProxyClient;
import com.cky.proxy.server.domain.entity.ProxyClientRule;
import com.cky.proxy.server.service.ProxyClientRuleService;
import com.cky.proxy.server.service.ProxyClientService;
import com.cky.proxy.server.util.BeanContext;
import com.cky.proxy.server.manager.TrafficStatisticManager;
import com.cky.proxy.server.socket.ServerMngSocketHandler;
import com.cky.proxy.server.socket.UserProxySocketHandler;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import io.vertx.core.net.NetServer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProxyServerVerticle extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(ProxyServerVerticle.class);
    private ProxyClientService proxyClientService;
    private ProxyClientRuleService proxyClientRuleService;
    private final Map<Integer, NetServer> netServerMap = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> ruleToClientMap = new ConcurrentHashMap<>();

    @Override
    public void start(Promise<Void> startPromise) {
        // 初始化数据库与默认数据，确保 BeanContext 就绪
        BeanContext initService = BeanContext.getInstance();
        initService.initializeDatabase();

        // 延后初始化依赖 BeanContext 的服务
        proxyClientService = new ProxyClientService();
        proxyClientRuleService = new ProxyClientRuleService();

        // 注册事件监听
        registerEventConsumers();

        ServerProperty server = ConfigProperty.getInstance().getServer();
        Integer proxyPort = server.getProxyPort();
        log.info("Server starting {}", proxyPort);
        vertx.createNetServer()
                .connectHandler(new ServerMngSocketHandler())
                .listen(proxyPort)
                .onFailure(t -> log.error("Server start failed", t));
        // TODO SSL https://vertx.io/docs/vertx-core/java/#ssl
        flushServerProxySocket();

        // 部署 Web 管理端 Verticle（提供 /health 与 /api/*）
        vertx.deployVerticle(WebManageVerticle.class.getCanonicalName(), res -> {
            if (res.succeeded()) {
                log.info("WebManage start success!");
            } else {
                log.error("WebManage start fail!", res.cause());
            }
        });

        // 启动流量统计定时任务 (每小时执行一次)
        vertx.setPeriodic(3600000L, id -> {
            TrafficStatisticManager.flush();
        });
    }

    @Override
    public void stop() {
        log.info("Server stopping, flush traffic stats...");
        TrafficStatisticManager.flush();
    }

    private void registerEventConsumers() {
        vertx.eventBus().consumer("proxy.rule.updated", msg -> {
            Integer ruleId = (Integer) msg.body();
            updateRuleServer(ruleId);
        });
        vertx.eventBus().consumer("proxy.rule.deleted", msg -> {
            Integer ruleId = (Integer) msg.body();
            stopRuleServer(ruleId, null);
        });
        vertx.eventBus().consumer("proxy.client.updated", msg -> {
            Integer clientId = (Integer) msg.body();
            updateClientServers(clientId);
        });
        vertx.eventBus().consumer("proxy.client.deleted", msg -> {
            Integer clientId = (Integer) msg.body();
            stopClientServers(clientId);
        });
    }

    private void stopRuleServer(Integer ruleId, Runnable completionHandler) {
        NetServer server = netServerMap.remove(ruleId);
        ruleToClientMap.remove(ruleId);
        if (server != null) {
            server.close(res -> {
                if (res.succeeded()) {
                    log.info("Stopped server for rule {}", ruleId);
                } else {
                    log.error("Failed to stop server for rule {}", ruleId, res.cause());
                }
                if (completionHandler != null) {
                    completionHandler.run();
                }
            });
        } else {
            if (completionHandler != null) {
                completionHandler.run();
            }
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
        NetServer server = vertx.createNetServer()
                .connectHandler(new UserProxySocketHandler(client, rule))
                .listen(rule.getServerPort(), res -> {
                    if (res.succeeded()) {
                        log.info("Started server for rule {} on port {}", rule.getName(), rule.getServerPort());
                        netServerMap.put(rule.getId(), res.result());
                        ruleToClientMap.put(rule.getId(), client.getId());
                    } else {
                        log.error("Failed to start server for rule {}", rule.getName(), res.cause());
                    }
                });
        log.debug("EP>> Init rule {} {} -> {}", rule.getName(), rule.getServerPort(), rule.getClientAddress());
    }

    private void updateClientServers(Integer clientId) {
        ProxyClient client = proxyClientService.getProxyClientById(clientId);
        if (client == null) return;
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
        for (Map.Entry<Integer, Integer> entry : ruleToClientMap.entrySet()) {
            if (entry.getValue().equals(clientId)) {
                stopRuleServer(entry.getKey(), null);
            }
        }
    }

    private void flushServerProxySocket() {
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
}
