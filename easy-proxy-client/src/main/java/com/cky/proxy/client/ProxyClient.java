package com.cky.proxy.client;

import com.cky.proxy.client.verticle.MainVerticle;

import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProxyClient {
    public static void main(String[] args) {
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
