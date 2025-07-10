package com.cky.proxy.server.verticle;

import com.cky.proxy.common.domain.ProxyClientConfig;
import com.cky.proxy.server.util.DbUtil;
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
        Router router = Router.router(vertx);
        
        // 添加CORS处理
        router.route().handler(ctx -> {
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
        
        router.route().handler(BodyHandler.create());
        
        // 健康检查端点
        router.get("/health").handler(ctx -> {
            ctx.response()
                .putHeader("content-type", "application/json")
                .end(new JsonObject().put("status", "UP").encode());
        });
        
        // API文档端点
        router.get("/api").handler(ctx -> {
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
        
        // API路由设置
        router.get("/api/proxy-clients").handler(this::getAllProxyClients);
        router.get("/api/proxy-clients/:token").handler(this::getProxyClientByToken);
        router.post("/api/proxy-clients").handler(this::addProxyClient);
        router.put("/api/proxy-clients/:token").handler(this::updateProxyClient);
        router.delete("/api/proxy-clients/:token").handler(this::deleteProxyClient);
        
        // 添加全局错误处理
        router.route().failureHandler(ctx -> {
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
        
        // 启动HTTP服务器
        vertx.createHttpServer()
            .requestHandler(router)
            .listen(8888, http -> {
                if (http.succeeded()) {
                    startPromise.complete();
                    log.info("HTTP server started on port 8888");
                } else {
                    startPromise.fail(http.cause());
                }
            });
    }
    
    // 获取所有代理客户端配置
    private void getAllProxyClients(RoutingContext context) {
        Collection<ProxyClientConfig> clientsCollection = DbUtil.getProxyClients();
        // 将Collection转换为List，避免序列化HashMap$Values的问题
        List<ProxyClientConfig> clients = new ArrayList<>(clientsCollection);
        context.response()
            .putHeader("content-type", "application/json")
            .end(Json.encodePrettily(clients));
    }
    
    // 根据token获取特定代理客户端配置
    private void getProxyClientByToken(RoutingContext context) {
        String token = context.pathParam("token");
        ProxyClientConfig client = DbUtil.getProxyClientByToken(token);
        HttpServerResponse response = context.response();
        
        if (client != null) {
            response.putHeader("content-type", "application/json")
                .end(Json.encodePrettily(client));
        } else {
            response.setStatusCode(404).end();
        }
    }
    
    // 添加新的代理客户端配置
    private void addProxyClient(RoutingContext context) {
        try {
            ProxyClientConfig client = Json.decodeValue(context.getBodyAsString(), ProxyClientConfig.class);
            DbUtil.addProxyClient(client);
            context.response()
                .setStatusCode(201)
                .putHeader("content-type", "application/json")
                .end(Json.encodePrettily(client));
        } catch (Exception e) {
            log.error("Failed to add proxy client", e);
            context.response().setStatusCode(400).end(new JsonObject()
                .put("error", e.getMessage()).encode());
        }
    }
    
    // 更新现有代理客户端配置
    private void updateProxyClient(RoutingContext context) {
        String token = context.pathParam("token");
        try {
            ProxyClientConfig client = Json.decodeValue(context.getBodyAsString(), ProxyClientConfig.class);
            
            // 确保路径参数中的token与请求体中的token一致
            if (!token.equals(client.getToken())) {
                context.response().setStatusCode(400).end(new JsonObject()
                    .put("error", "Token in path does not match token in request body").encode());
                return;
            }
            
            // 检查客户端是否存在
            if (DbUtil.getProxyClientByToken(token) == null) {
                context.response().setStatusCode(404).end();
                return;
            }
            
            DbUtil.updateProxyClient(client);
            context.response()
                .putHeader("content-type", "application/json")
                .end(Json.encodePrettily(client));
        } catch (Exception e) {
            log.error("Failed to update proxy client", e);
            context.response().setStatusCode(400).end(new JsonObject()
                .put("error", e.getMessage()).encode());
        }
    }
    
    // 删除代理客户端配置
    private void deleteProxyClient(RoutingContext context) {
        String token = context.pathParam("token");
        ProxyClientConfig client = DbUtil.getProxyClientByToken(token);
        
        if (client != null) {
            DbUtil.deleteProxyClient(client);
            context.response().setStatusCode(204).end();
        } else {
            context.response().setStatusCode(404).end();
        }
    }
}
