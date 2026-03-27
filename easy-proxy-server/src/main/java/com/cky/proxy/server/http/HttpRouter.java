package com.cky.proxy.server.http;

import com.cky.proxy.server.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import cn.hutool.jwt.JWTUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpRouter implements HttpHandler {
    private static final Logger log = LoggerFactory.getLogger(HttpRouter.class);
    
    private final List<Route> routes = new ArrayList<>();
    private final byte[] jwtSecret = "easy-proxy-secret-key-for-jwt-authentication".getBytes();

    private static class Route {
        String method;
        String path;
        Consumer<HttpContext> handler;

        Route(String method, String path, Consumer<HttpContext> handler) {
            this.method = method;
            this.path = path;
            this.handler = handler;
        }
    }

    public void get(String path, Consumer<HttpContext> handler) {
        routes.add(new Route("GET", path, handler));
    }

    public void post(String path, Consumer<HttpContext> handler) {
        routes.add(new Route("POST", path, handler));
    }

    public void put(String path, Consumer<HttpContext> handler) {
        routes.add(new Route("PUT", path, handler));
    }

    public void delete(String path, Consumer<HttpContext> handler) {
        routes.add(new Route("DELETE", path, handler));
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        // CORS Headers
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.getResponseHeaders().add("Access-Control-Max-Age", "86400");

        if ("OPTIONS".equalsIgnoreCase(method)) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            // JWT Auth
            if (path.startsWith("/api/") && !path.startsWith("/api/open/")) {
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    sendError(exchange, 401, "Unauthorized");
                    return;
                }
                String token = authHeader.substring(7);
                if (!JWTUtil.verify(token, jwtSecret)) {
                    sendError(exchange, 401, "Invalid token");
                    return;
                }
            }

            // Routing
            for (Route route : routes) {
                if (route.method.equalsIgnoreCase(method) && route.path.equals(path)) {
                    HttpContext ctx = new HttpContext(exchange);
                    route.handler.accept(ctx);
                    return;
                }
            }
            sendError(exchange, 404, "Not Found");
        } catch (Exception e) {
            log.error("Request failed", e);
            sendError(exchange, 500, e.getMessage() != null ? e.getMessage() : "Server error");
        }
    }

    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        String json = "{\"code\":" + statusCode + ",\"msg\":\"" + message + "\"}";
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json;charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
