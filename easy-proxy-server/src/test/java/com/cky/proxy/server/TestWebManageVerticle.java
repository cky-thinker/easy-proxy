package com.cky.proxy.server;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.cky.proxy.server.verticle.WebManageVerticle;

import com.cky.proxy.server.util.BeanContext;
import com.cky.proxy.server.util.EventBusUtil;
import com.cky.proxy.server.config.ConfigProperty;
import com.cky.proxy.server.config.DatabaseProperty;
import org.junit.jupiter.api.BeforeAll;

@ExtendWith(VertxExtension.class)
public class TestWebManageVerticle {

    @BeforeAll
    static void setup() {
        // Initialize BeanContext with in-memory H2 database
        BeanContext beanContext = BeanContext.getInstance();
        DatabaseProperty db = new DatabaseProperty();
        db.setUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        db.setUsername("sa");
        db.setPassword("");
        ConfigProperty.getInstance().setDb(db);
        beanContext.init();
    }

    @BeforeEach
    void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
        EventBusUtil.init(vertx);
        vertx.deployVerticle(new WebManageVerticle(), testContext.succeeding(id -> testContext.completeNow()));
    }

    @Test
    void verticle_deployed(Vertx vertx, VertxTestContext testContext) throws Throwable {
        testContext.completeNow();
    }
}
