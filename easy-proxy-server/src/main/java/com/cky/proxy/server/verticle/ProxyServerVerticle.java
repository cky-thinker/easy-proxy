package com.cky.proxy.server.verticle;

import com.cky.proxy.server.config.ConfigProperty;
import com.cky.proxy.server.config.ServerProperty;
import com.cky.proxy.server.domain.entity.ProxyClient;
import com.cky.proxy.server.domain.entity.ProxyClientRule;
import com.cky.proxy.server.service.ProxyClientRuleService;
import com.cky.proxy.server.service.ProxyClientService;
import com.cky.proxy.server.util.BeanContext;
import com.cky.proxy.server.socket.ServerMngSocketHandler;
import com.cky.proxy.server.socket.UserProxySocketHandler;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ProxyServerVerticle extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(ProxyServerVerticle.class);
    private ProxyClientService proxyClientService;
    private ProxyClientRuleService proxyClientRuleService;

    @Override
    public void start(Promise<Void> startPromise) {
        // 初始化数据库与默认数据，确保 BeanContext 就绪
        BeanContext initService = BeanContext.getInstance();
        initService.initializeDatabase();

        // 延后初始化依赖 BeanContext 的服务
        proxyClientService = new ProxyClientService();
        proxyClientRuleService = new ProxyClientRuleService();

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
    }

    private void flushServerProxySocket() {
        List<ProxyClient> proxyClients = proxyClientService.getProxyClients();
        for (ProxyClient proxyClient : proxyClients) {
            if (proxyClient.getEnableFlag()) {
                log.debug("EP>> Init client {} ", proxyClient.getName());
                List<ProxyClientRule> proxyClientRules = proxyClientRuleService
                        .getProxyClientRules(proxyClient.getId());
                for (ProxyClientRule proxyRule : proxyClientRules) {
                    if (proxyRule.getEnableFlag()) {
                        vertx.createNetServer()
                                .connectHandler(new UserProxySocketHandler(proxyClient, proxyRule))
                                .listen(proxyRule.getServerPort())
                                .onFailure(t -> log.error("sMngServer 启动失败", t));
                        log.debug("EP>> Init rule {} {} -> {}", proxyRule.getName(), proxyRule.getServerPort(),
                                proxyRule.getClientAddress());
                    }
                }
            }
        }
    }
}
