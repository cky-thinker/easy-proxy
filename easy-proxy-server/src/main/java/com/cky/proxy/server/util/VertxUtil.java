package com.cky.proxy.server.util;

import com.cky.proxy.server.bean.dto.Result;

import io.vertx.ext.web.RoutingContext;

public class VertxUtil {
    public static <T> void response(RoutingContext ctx, Result<T> result) {
        ctx.response()
                .setStatusCode(200)
                .putHeader("content-type", "application/json")
                .end(JsonUtil.toJson(result));
    }
}
