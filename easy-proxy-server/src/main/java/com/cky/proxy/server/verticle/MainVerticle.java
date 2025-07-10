package com.cky.proxy.server.verticle;

import cn.hutool.setting.dialect.Props;
import com.cky.proxy.common.domain.ProxyClientConfig;
import com.cky.proxy.common.domain.ProxyRule;
import com.cky.proxy.common.util.ConfigUtil;
import com.cky.proxy.server.handler.ServerMngSocketHandler;
import com.cky.proxy.server.handler.UserProxySocketHandler;
import com.cky.proxy.server.util.DbUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;

@Slf4j
public class MainVerticle extends AbstractVerticle {
    @Override
    public void start(Promise<Void> startPromise) {
        Props config = ConfigUtil.getConfig();
        Integer serverMngPort = config.getInt("server.port");
        log.error("Server starting {}", serverMngPort);
        vertx.createNetServer()
            .connectHandler(new ServerMngSocketHandler())
            .listen(serverMngPort)
            .onFailure(t -> log.error("Server start failed", t));
        // TODO SSL https://vertx.io/docs/vertx-core/java/#ssl
        flushServerProxySocket();
        
        // 部署WebVerticle以提供HTTP API
        vertx.deployVerticle(new WebVerticle(), res -> {
            if (res.succeeded()) {
                log.info("deploy web server success!");
            } else {
                log.error("deploy web server fail!", res.cause());
            }
        });
    }

    private void flushServerProxySocket() {
        Collection<ProxyClientConfig> proxyClientConfigs = DbUtil.getProxyClients();
        for (ProxyClientConfig proxyClientConfig : proxyClientConfigs) {
            log.debug("EP>> Init client {} ", proxyClientConfig.getName());
            for (ProxyRule proxyRule : proxyClientConfig.getProxyRules()) {
                vertx.createNetServer()
                    .connectHandler(new UserProxySocketHandler(proxyClientConfig, proxyRule))
                    .listen(proxyRule.getServerPort())
                    .onFailure(t -> log.error("sMngServer 启动失败", t));
                log.debug("EP>> Init rule {} {} -> {}", proxyRule.getName(), proxyRule.getServerPort(), proxyRule.getClientAddress());
            }
        }
    }
}
