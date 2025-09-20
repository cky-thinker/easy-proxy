package com.cky.proxy.server;

import com.cky.proxy.server.dao.DaoManager;
import com.cky.proxy.server.verticle.ProxyServerVerticle;
import com.cky.proxy.server.verticle.WebManageVerticle;
import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProxyServer {
    public static void main(String[] args) {
        // 初始化数据库表和数据
        DaoManager initService = DaoManager.getInstance();
        initService.initializeDatabase();

        Vertx vertx = Vertx.vertx();
        vertx.exceptionHandler(t -> {
            log.error(t.getMessage(), t);
        });
        log.info("ProxyServer start...");
        vertx.deployVerticle(ProxyServerVerticle.class.getCanonicalName(), res -> {
            if (res.succeeded()) {
                log.info("ProxyServer start success!");
            } else {
                log.error("ProxyServer start fail!", res.cause());
            }
        });
        log.info("WebManage start...");
        vertx.deployVerticle(new WebManageVerticle(), res -> {
            if (res.succeeded()) {
                log.info("WebManage start success!");
            } else {
                log.error("WebManage start fail!", res.cause());
            }
        });
    }
}
