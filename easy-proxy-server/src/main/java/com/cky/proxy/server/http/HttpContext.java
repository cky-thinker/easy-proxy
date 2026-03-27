package com.cky.proxy.server.http;

import com.sun.net.httpserver.HttpExchange;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HttpContext {
    private final HttpExchange exchange;
    private final Map<String, String> queryParams;
    private String bodyCache;

    public HttpContext(HttpExchange exchange) {
        this.exchange = exchange;
        this.queryParams = parseQueryParams(exchange.getRequestURI().getQuery());
    }

    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return map;
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0) {
                map.put(pair.substring(0, idx), pair.substring(idx + 1));
            } else if (idx == -1) {
                map.put(pair, "");
            }
        }
        return map;
    }

    public HttpExchange getExchange() {
        return exchange;
    }

    public String getParam(String name) {
        return queryParams.get(name);
    }

    public String getParam(String name, String defaultValue) {
        return queryParams.getOrDefault(name, defaultValue);
    }

    public String getBodyAsString() {
        if (bodyCache != null) {
            return bodyCache;
        }
        try (InputStream is = exchange.getRequestBody()) {
            byte[] bytes = is.readAllBytes();
            bodyCache = new String(bytes, StandardCharsets.UTF_8);
            return bodyCache;
        } catch (Exception e) {
            throw new RuntimeException("Failed to read request body", e);
        }
    }
}
