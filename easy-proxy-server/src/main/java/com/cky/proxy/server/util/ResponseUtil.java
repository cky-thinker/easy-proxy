package com.cky.proxy.server.util;

import com.cky.proxy.server.domain.dto.Result;

import io.vertx.ext.web.RoutingContext;

public class ResponseUtil {

    /**
     * 响应成功结果
     */
    public static <T> void response(RoutingContext ctx, Result<T> result) {
        ctx.response()
                .setStatusCode(200)
                .putHeader("content-type", "application/json")
                .end(JsonUtil.toJson(result));
    }

    /**
     * 响应成功结果（带数据）
     */
    public static <T> void success(RoutingContext ctx, T data) {
        response(ctx, Result.success(data));
    }

    /**
     * 响应成功结果（带数据和消息）
     */
    public static <T> void success(RoutingContext ctx, T data, String message) {
        response(ctx, Result.success(data, message));
    }

    /**
     * 响应错误结果
     */
    public static void error(RoutingContext ctx, String message) {
        response(ctx, Result.error(message));
    }

    /**
     * 响应错误结果（带状态码）
     */
    public static void error(RoutingContext ctx, int statusCode, String message) {
        Result<Object> result = Result.error(message);
        result.code = statusCode;
        ctx.response()
                .setStatusCode(statusCode)
                .putHeader("content-type", "application/json")
                .end(JsonUtil.toJson(result));
    }
}
