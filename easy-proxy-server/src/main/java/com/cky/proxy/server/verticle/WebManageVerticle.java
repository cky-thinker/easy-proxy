package com.cky.proxy.server.verticle;

import com.cky.proxy.server.config.ConfigProperty;
import com.cky.proxy.server.controller.ProxyClientController;
import com.cky.proxy.server.controller.ProxyClientRuleController;
import com.cky.proxy.server.controller.SysLogController;
import com.cky.proxy.server.controller.UserController;
import com.cky.proxy.server.controller.TrafficStatisticController;
import com.cky.proxy.server.controller.DashboardController;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import com.cky.proxy.server.util.CertGenerator;

public class WebManageVerticle extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(WebManageVerticle.class);

    private JWTAuth jwtAuth;

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

        // 证书下载端点（公开，无需认证）
        baseRouter.get("/cert.pem").handler(ctx -> {
            try {
                Path pemPath = Paths.get(CertGenerator.getPemCertPath());
                if (!Files.exists(pemPath)) {
                    ctx.response().setStatusCode(404).end("cert.pem not found");
                    return;
                }
                byte[] content = Files.readAllBytes(pemPath);
                ctx.response()
                        .putHeader("Content-Type", "application/x-pem-file")
                        .putHeader("Cache-Control", "no-store")
                        .end(io.vertx.core.buffer.Buffer.buffer(content));
            } catch (Exception e) {
                log.error("Serve cert.pem failed", e);
                ctx.response().setStatusCode(500).end("serve cert error");
            }
        });

        // 添加全局错误处理
        baseRouter.route().failureHandler(ctx -> {
            int statusCode = ctx.statusCode();
            Throwable failure = ctx.failure();
            if (failure != null) {
                log.error("Request failed", failure);
            }

            String errorMessage;
            if (failure instanceof jakarta.validation.ConstraintViolationException cve) {
                statusCode = statusCode == -1 ? 400 : statusCode;
                StringBuilder sb = new StringBuilder();
                cve.getConstraintViolations().forEach(v -> {
                    if (!sb.isEmpty()) sb.append("; ");
                    sb.append(v.getPropertyPath()).append(": ").append(v.getMessage());
                });
                errorMessage = sb.toString();
            } else {
                statusCode = statusCode == -1 ? 500 : statusCode;
                errorMessage = failure != null ? failure.getMessage() : "Server error";
            }

            JsonObject res = new JsonObject()
                .put("code", statusCode)
                .put("msg", errorMessage);

            ctx.response()
                .setStatusCode(statusCode)
                .putHeader("Content-Type", "application/json;charset=UTF-8")
                .end(res.encode());
        });

        // 添加JWT认证中间件到API路由
        JWTAuthHandler jwtAuthHandler = JWTAuthHandler.create(jwtAuth);

        // 将API路由挂载到基础路由，并添加JWT认证
        baseRouter.route("/api/*").handler(ctx -> {
            // 排除不需要认证的路径
            String path = ctx.request().path();
            if (path.equals("/api/sys/captchaImage") || path.equals("/api/sys/loginUser")|| path.equals("/api/sys/config") ||
                    path.equals("/api") || path.equals("/health")) {
                ctx.next();
                return;
            }

            // 其他API路径需要JWT认证
            jwtAuthHandler.handle(ctx);
        });

        // 手动设置API路由
        new ProxyClientController(baseRouter, vertx);
        new ProxyClientRuleController(baseRouter, vertx);
        new SysLogController(baseRouter);
        new TrafficStatisticController(baseRouter);
        new DashboardController(baseRouter);
        new UserController(baseRouter, vertx);

        // 启动HTTP服务器
        ConfigProperty configProperty = ConfigProperty.getInstance();
        int webPort = configProperty.getServer().getWebPort();
        vertx.createHttpServer()
                .requestHandler(baseRouter)
                .listen(webPort, http -> {
                    if (http.succeeded()) {
                        startPromise.complete();
                        log.info("HTTP server started on port {}", webPort);
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
