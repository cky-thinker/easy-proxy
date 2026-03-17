package com.cky.proxy.client;

import com.cky.proxy.client.config.ConfigProperty;
import com.cky.proxy.client.util.CertDownloader;
import com.cky.proxy.client.verticle.MainVerticle;
import com.cky.proxy.common.util.ConfigBootstrap;

import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProxyClient {
    public static void main(String[] args) {
        // 检查并复制默认配置文件
        ConfigBootstrap.initConfigs(ConfigProperty.CONFIG_FILE);
        // 检查并下载证书：不存在则下载证书
        try {
            CertDownloader.downloadIfNotExists();
        } catch (Exception e) {
            log.error("download cert fail!", e);
        }
        Vertx vertx = Vertx.vertx();
        vertx.exceptionHandler(t -> {
            log.error(t.getMessage(), t);
        });
        vertx.deployVerticle(MainVerticle.class.getName(), res -> {
            if (!res.succeeded()) {
                log.info("deploy fail", res.cause());
            }
        });

    }
}
