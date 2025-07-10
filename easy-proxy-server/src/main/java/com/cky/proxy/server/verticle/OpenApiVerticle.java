package com.cky.proxy.server.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.openapi.RouterBuilderOptions;
import lombok.extern.slf4j.Slf4j;

/**
 * OpenAPI集成Verticle，提供Swagger UI界面
 */
@Slf4j
public class OpenApiVerticle extends AbstractVerticle {

    private static final int PORT = 8889;
    private static final String OPENAPI_SPEC_PATH = "openapi.yaml";
    private HttpServer server;

    @Override
    public void start(Promise<Void> startPromise) {
        // 从OpenAPI规范创建RouterBuilder
        RouterBuilder.create(vertx, OPENAPI_SPEC_PATH)
                .onSuccess(routerBuilder -> {
                    // 配置RouterBuilder选项
                    RouterBuilderOptions options = new RouterBuilderOptions()
                            .setMountResponseContentTypeHandler(true)
                            .setRequireSecurityHandlers(false);
                    routerBuilder.setOptions(options);

                    // 创建主路由器
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

                    // // 添加Swagger UI静态资源处理器
                    // router.route("/swagger/*").handler(ctx -> {
                    //     // 重定向到Swagger UI
                    //     ctx.reroute("/swagger/index.html");
                    // });

                    // 提供OpenAPI规范文件
                    router.get("/openapi.yaml").handler(ctx -> {
                        // 尝试从classpath读取
                        vertx.fileSystem().readFile("openapi.yaml", ar2 -> {
                            if (ar2.succeeded()) {
                                ctx.response()
                                        .putHeader("Content-Type", "text/yaml")
                                        .end(ar2.result());
                            } else {
                                log.error("Failed to read OpenAPI specification file from classpath: {}",
                                        ar2.cause().getMessage());
                                ctx.fail(ar2.cause());
                            }
                        });
                    });

                    // 提供OpenAPI规范文件
                    router.get("/swagger").handler(ctx -> {
                        log.info("swagger");
                        // 尝试从classpath读取
                        vertx.fileSystem().readFile("webroot/swagger-ui/index.html", ar2 -> {
                            if (ar2.succeeded()) {
                                ctx.response()
                                        .putHeader("Content-Type", "text/html")
                                        .end(ar2.result());
                            } else {
                                log.error("Failed to read index.html file from classpath: {}",
                                        ar2.cause().getMessage());
                                ctx.fail(ar2.cause());
                            }
                        });
                    });

                    // // 提供Swagger UI界面
                    // router.route("/swagger/*").handler(StaticHandler.create().setWebRoot("webroot/swagger-ui"));

                    // 创建Swagger UI配置
                    router.get("/swagger-config").handler(ctx -> {
                        JsonObject config = new JsonObject()
                                .put("url", "/openapi.yaml")
                                .put("dom_id", "#swagger-ui")
                                .put("deepLinking", true)
                                .put("presets", "[SwaggerUIBundle.presets.apis, SwaggerUIStandalonePreset]")
                                .put("layout", "StandaloneLayout");

                        ctx.response()
                                .putHeader("Content-Type", "application/json")
                                .end(config.encode());
                    });

                    // 启动HTTP服务器
                    server = vertx.createHttpServer();
                    server.requestHandler(router)
                            .listen(PORT, http -> {
                                if (http.succeeded()) {
                                    log.info("OpenAPI documentation server started on port {}", PORT);
                                    log.info("Swagger UI available at http://localhost:{}/swagger", PORT);
                                    startPromise.complete();
                                } else {
                                    log.error("Failed to start OpenAPI documentation server", http.cause());
                                    startPromise.fail(http.cause());
                                }
                            });
                })
                .onFailure(err -> {
                    log.error("Failed to create router builder from OpenAPI specification", err);
                    startPromise.fail(err);
                });
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        if (server != null) {
            server.close(ar -> {
                if (ar.succeeded()) {
                    log.info("OpenAPI documentation server stopped");
                    stopPromise.complete();
                } else {
                    log.error("Failed to stop OpenAPI documentation server", ar.cause());
                    stopPromise.fail(ar.cause());
                }
            });
        } else {
            stopPromise.complete();
        }
    }
}