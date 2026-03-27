package com.cky.proxy.server.util;

import com.cky.proxy.server.domain.dto.Result;
import com.cky.proxy.server.http.HttpContext;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ResponseUtil {

    public static <T> void response(HttpContext ctx, Result<T> result) {
        sendResponse(ctx, 200, JsonUtil.toJson(result));
    }

    public static <T> void success(HttpContext ctx, T data) {
        response(ctx, Result.success(data));
    }

    public static <T> void success(HttpContext ctx, T data, String message) {
        response(ctx, Result.success(data, message));
    }

    public static void error(HttpContext ctx, String message) {
        response(ctx, Result.error(message));
    }

    public static void error(HttpContext ctx, int statusCode, String message) {
        Result<Object> result = Result.error(message);
        result.code = statusCode;
        sendResponse(ctx, statusCode, JsonUtil.toJson(result));
    }

    private static void sendResponse(HttpContext ctx, int statusCode, String json) {
        try {
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            ctx.getExchange().getResponseHeaders().add("Content-Type", "application/json;charset=UTF-8");
            ctx.getExchange().sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = ctx.getExchange().getResponseBody()) {
                os.write(bytes);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to write response", e);
        }
    }
}
