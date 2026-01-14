package com.cky.proxy.server;

import com.cky.proxy.server.util.BeanContext;
import com.cky.proxy.server.util.EventBusUtil;
import com.cky.proxy.server.util.SelfSignedCertGenerator;
import com.cky.proxy.server.verticle.ProxyServerVerticle;
import com.cky.proxy.server.verticle.WebManageVerticle;

import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyServer {
    private static final Logger log = LoggerFactory.getLogger(ProxyServer.class);

    public static void main(String[] args) {
        // 初始化数据库表和数据
        BeanContext beanContext = BeanContext.getInstance();
        beanContext.init();
        // 检查并生成证书：不存在则生成JKS证书+导出PEM公钥
        try {
            SelfSignedCertGenerator.generateIfNotExists();
        } catch (Exception e) {
            log.error("generate self signed cert fail!", e);
        }

        Vertx vertx = Vertx.vertx();
        EventBusUtil.setup(vertx);
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
        vertx.deployVerticle(WebManageVerticle.class.getCanonicalName(), res -> {
            if (res.succeeded()) {
                log.info("WebManage start success!");
            } else {
                log.error("WebManage start fail!", res.cause());
            }
        });
    }
}
