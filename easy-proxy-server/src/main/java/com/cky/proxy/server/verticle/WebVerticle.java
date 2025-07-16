package com.cky.proxy.server.verticle;

import com.cky.proxy.common.domain.ProxyClientConfig;
import com.cky.proxy.server.util.DbUtil;
import com.cky.proxy.server.web.ProxyClientController;
import com.cky.proxy.server.web.SysUserController;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
public class WebVerticle extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startPromise) {
        // 创建基础路由器
        Router baseRouter = Router.router(vertx);
        
        // 添加CORS处理
        baseRouter.route().handler(ctx -> {
            ctx.response()
                .putHeader("Access-Control-Allow-Origin", "*")
                .putHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
                .putHeader("Access-Control-Allow-Headers", "Content-Type, Authorization")
                .putHeader("Access-Control-Max-Age", "86400");
            
            // 处理预检请求
            if (ctx.request().method() == io.vertx.core.http.HttpMethod.OPTIONS) {
                ctx.response().setStatusCode(204).end();
            } else {
                ctx.next();
            }
        });
        
        baseRouter.route().handler(BodyHandler.create());
        
        // 健康检查端点
        baseRouter.get("/health").handler(ctx -> {
            ctx.response()
                .putHeader("content-type", "application/json")
                .end(new JsonObject().put("status", "UP").encode());
        });
        
        // API文档端点
        baseRouter.get("/api").handler(ctx -> {
            JsonObject apiDocs = new JsonObject()
                .put("name", "Easy Proxy API")
                .put("version", "1.0.0")
                .put("endpoints", new JsonObject()
                    .put("GET /api/proxy-clients", "获取所有代理客户端配置")
                    .put("GET /api/proxy-clients/:token", "根据token获取特定代理客户端配置")
                    .put("POST /api/proxy-clients", "添加新的代理客户端配置")
                    .put("PUT /api/proxy-clients/:token", "更新现有代理客户端配置")
                    .put("DELETE /api/proxy-clients/:token", "删除代理客户端配置")
                );
            
            ctx.response()
                .putHeader("content-type", "application/json")
                .end(Json.encodePrettily(apiDocs));
        });
        
        // 添加全局错误处理
        baseRouter.route().failureHandler(ctx -> {
            int statusCode = ctx.statusCode();
            if (statusCode == -1) {
                statusCode = 500;
            }
            
            Throwable failure = ctx.failure();
            String errorMessage = failure != null ? failure.getMessage() : "Unknown error";
            
            log.error("API错误: {}", errorMessage, failure);
            
            ctx.response()
                .setStatusCode(statusCode)
                .putHeader("content-type", "application/json")
                .end(new JsonObject()
                    .put("error", errorMessage)
                    .put("path", ctx.request().path())
                    .put("status", statusCode)
                    .encode());
        });
        
        // 手动设置API路由
        new ProxyClientController(baseRouter);
        new SysUserController(baseRouter);
        
        // 启动HTTP服务器
        vertx.createHttpServer()
            .requestHandler(baseRouter)
            .listen(8888, http -> {
                if (http.succeeded()) {
                    startPromise.complete();
                    log.info("HTTP server started on port 8888");
                } else {
                    startPromise.fail(http.cause());
                }
            });
    }
}
