package com.cky.proxy.server.verticle;

import io.vertx.core.Vertx;
import org.junit.jupiter.api.Test;

class WebManageVerticleTest {
    // @Test
    public void test() throws InterruptedException {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new WebManageVerticle());
        Thread.sleep(60_60_1000);
    }
}
