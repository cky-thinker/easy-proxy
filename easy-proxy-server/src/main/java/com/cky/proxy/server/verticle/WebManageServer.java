package com.cky.proxy.server.verticle;

import com.cky.proxy.server.config.ConfigProperty;
import com.cky.proxy.server.controller.*;
import com.cky.proxy.server.http.HttpRouter;
import com.cky.proxy.server.util.CertGenerator;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;

public class WebManageServer {
    private static final Logger log = LoggerFactory.getLogger(WebManageServer.class);

    public void start() {
        try {
            ConfigProperty configProperty = ConfigProperty.getInstance();
            int webPort = configProperty.getServer().getWebPort();

            HttpServer server = HttpServer.create(new InetSocketAddress(webPort), 0);
            server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

            HttpRouter router = new HttpRouter();

            // 健康检查端点
            router.get("/api/open/health", ctx -> {
                try {
                    String json = "{\"status\":\"UP\"}";
                    byte[] bytes = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    ctx.getExchange().getResponseHeaders().add("Content-Type", "application/json");
                    ctx.getExchange().sendResponseHeaders(200, bytes.length);
                    try (var os = ctx.getExchange().getResponseBody()) {
                        os.write(bytes);
                    }
                } catch (Exception e) {
                    log.error("Serve health check failed", e);
                }
            });

            // 证书下载端点（公开，无需认证）
            router.get("/api/open/cert.pem", ctx -> {
                try {
                    Path pemPath = Paths.get(CertGenerator.getPemCertPath());
                    if (!Files.exists(pemPath)) {
                        String msg = "cert.pem not found";
                        ctx.getExchange().sendResponseHeaders(404, msg.length());
                        try (var os = ctx.getExchange().getResponseBody()) {
                            os.write(msg.getBytes());
                        }
                        return;
                    }
                    byte[] content = Files.readAllBytes(pemPath);
                    ctx.getExchange().getResponseHeaders().add("Content-Type", "application/x-pem-file");
                    ctx.getExchange().getResponseHeaders().add("Cache-Control", "no-store");
                    ctx.getExchange().sendResponseHeaders(200, content.length);
                    try (var os = ctx.getExchange().getResponseBody()) {
                        os.write(content);
                    }
                } catch (Exception e) {
                    log.error("Serve cert.pem failed", e);
                }
            });

            // 初始化所有 Controller
            new ProxyClientController(router);
            new ProxyClientRuleController(router);
            new SysLogController(router);
            new TrafficStatisticController(router);
            new DashboardController(router);
            new UserController(router);

            server.createContext("/", router);

            server.start();
            log.info("HTTP server started on port {}", webPort);

        } catch (Exception e) {
            log.error("Failed to start WebManageServer", e);
            throw new RuntimeException(e);
        }
    }
}
