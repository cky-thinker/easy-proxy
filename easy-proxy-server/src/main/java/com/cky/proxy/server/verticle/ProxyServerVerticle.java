package com.cky.proxy.server.verticle;

import com.cky.proxy.server.config.ConfigProperty;
import com.cky.proxy.server.config.ServerProperty;
import com.cky.proxy.server.domain.entity.ProxyClient;
import com.cky.proxy.server.domain.entity.ProxyClientRule;
import com.cky.proxy.server.service.ProxyClientRuleService;
import com.cky.proxy.server.service.ProxyClientService;
import com.cky.proxy.server.util.EventBusUtil;
import com.cky.proxy.server.manager.TrafficStatisticManager;
import com.cky.proxy.server.socket.ServerMngSocketHandler;
import com.cky.proxy.server.socket.UserProxySocketHandler;
import com.cky.proxy.server.socket.manager.UserSocketManager;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import io.vertx.core.net.NetServer;

public class ProxyServerVerticle extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(ProxyServerVerticle.class);
    private ProxyClientService proxyClientService;
    private ProxyClientRuleService proxyClientRuleService;

    @Override
    public void start(Promise<Void> startPromise) {
        // 注册事件监听
        registerEventConsumers();

        ServerProperty server = ConfigProperty.getInstance().getServer();
        Integer proxyPort = server.getProxyPort();
        log.info("Server starting {}", proxyPort);
        vertx.createNetServer()
                .connectHandler(new ServerMngSocketHandler(vertx))
                .listen(proxyPort)
                .onFailure(t -> log.error("Server start failed", t));
        // TODO SSL https://vertx.io/docs/vertx-core/java/#ssl
        initServerProxySocket();

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

    private void initServerProxySocket() {
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

    private void registerEventConsumers() {
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
        NetServer server = UserSocketManager.removeRuleListenSocket(ruleId);
        if (server == null) {
            return;
        }
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
                .listen(rule.getServerPort(), res -> {
                    if (res.succeeded()) {
                        log.info("Listening for rule {} on port {}", rule.getName(), rule.getServerPort());
                        UserSocketManager.addRuleListenSocket(rule.getId(), res.result());
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
