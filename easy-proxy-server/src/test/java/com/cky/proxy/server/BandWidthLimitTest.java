package com.cky.proxy.server;

import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
public class BandWidthLimitTest {
    @Test
    public void testBandWidthLimit() throws InterruptedException {
        Vertx vertx = Vertx.vertx();
        vertx.exceptionHandler(t -> {
            log.error(t.getMessage(), t);
        });
        vertx.deployVerticle(BandWidthLimitVerticle.class.getCanonicalName(), res -> {
            if (res.succeeded()) {
                log.info("deploy MainVerticle success!");
            } else {
                log.error("deploy MainVerticle fail!", res.cause());
            }
        });
        Thread.sleep(60 * 60 * 1000);
    }
}


