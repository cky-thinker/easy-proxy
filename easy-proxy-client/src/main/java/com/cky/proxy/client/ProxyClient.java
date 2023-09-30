package com.cky.proxy.client;

import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProxyClient {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.exceptionHandler(t -> {
            log.error(t.getMessage(), t);
        });
        vertx.deployVerticle("com.cky.proxy.client.verticle.MainVerticle", res -> {
            if (!res.succeeded()) {
                log.info("deploy fail", res.cause());
            }
        });

    }
}
