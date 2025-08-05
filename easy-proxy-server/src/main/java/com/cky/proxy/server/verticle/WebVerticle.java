package com.cky.proxy.server.verticle;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.cky.proxy.server.controller.ProxyClientController;
import com.cky.proxy.server.controller.SysUserController;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;
import io.vertx.ext.auth.PubSecKeyOptions;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WebVerticle extends AbstractVerticle {

    private JWTAuth jwtAuth;
    private static final Set<String> WHITE_LIST = new HashSet<>(Arrays.asList("/api/auth/login", "/api/auth/captcha"));
    
    @Override
    public void start(Promise<Void> startPromise) {
        // 初始化JWT认证
        initJWTAuth();
        
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
        
        // 添加JWT认证中间件到API路由
        JWTAuthHandler jwtAuthHandler = JWTAuthHandler.create(jwtAuth);
        
        // 将API路由挂载到基础路由，并添加JWT认证
        baseRouter.route("/api/*").handler(ctx -> {
            // 排除不需要认证的路径
            String path = ctx.request().path();
            if (path.equals("/api/sys/captchaImage") || path.equals("/api/sys/loginUser") || 
                path.equals("/api") || path.equals("/health")) {
                ctx.next();
                return;
            }
            
            // 其他API路径需要JWT认证
            jwtAuthHandler.handle(ctx);
        });
        
        // 手动设置API路由
        new ProxyClientController(baseRouter);
        new SysUserController(baseRouter, vertx);
        
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
    
    /**
     * 初始化JWT认证
     */
    private void initJWTAuth() {
        // 配置JWT认证选项
        JWTAuthOptions jwtAuthOptions = new JWTAuthOptions()
                .addPubSecKey(new PubSecKeyOptions()
                        .setAlgorithm("HS256")
                        .setBuffer("easy-proxy-secret-key-for-jwt-authentication"));
        
        // 创建JWT认证实例
        this.jwtAuth = JWTAuth.create(vertx, jwtAuthOptions);
    }
}
