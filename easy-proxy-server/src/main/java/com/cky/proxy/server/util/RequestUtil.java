package com.cky.proxy.server.util;

import cn.hutool.db.Page;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
public class RequestUtil {

    /**
     * 从请求上下文中创建分页对象
     *
     * @param ctx 路由上下文
     * @return 分页对象
     */
    public static Page getPage(RoutingContext ctx) {
        // 获取分页参数
        int page = Integer.parseInt(ctx.request().getParam("page", "1"));
        int pageSize = Integer.parseInt(ctx.request().getParam("pageSize", "10"));

        // 创建分页对象
        Page hutoolPage = new Page(page, pageSize);

        // 获取排序参数
        String sortField = ctx.request().getParam("sortField");
        String sortOrder = ctx.request().getParam("sortOrder");
        if (sortField != null && !sortField.isEmpty()) {
            Direction direction = "desc".equalsIgnoreCase(sortOrder) ? Direction.DESC : Direction.ASC;
            hutoolPage.addOrder(new Order(sortField, direction));
        }

        return hutoolPage;
    }

    public static <T> T getBodyObj(RoutingContext ctx, Class<T> clazz) {
        String bodyStr = ctx.body().asString();
        if (bodyStr == null || bodyStr.isEmpty()) {
            return null;
        }
        return JsonUtil.parseJson(bodyStr, clazz);
    }

    public static Integer getParamInt(RoutingContext ctx, String paramName) {
        String paramStr = ctx.request().getParam(paramName);
        if (paramStr == null || paramStr.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(paramStr);
        } catch (NumberFormatException e) {
            log.error("Failed to parse int parameter: {}", paramStr, e);
            return null;
        }
    }

    public static Date getParamDate(RoutingContext ctx, String name) {
        String val = ctx.request().getParam(name);
        if (val == null || val.isEmpty())
            return null;
        try {
            // 支持 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss
            SimpleDateFormat sdf = val.length() > 10 ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    : new SimpleDateFormat("yyyy-MM-dd");
            return sdf.parse(val);
        } catch (ParseException e) {
            return null;
        }
    }

    public static String getParam(RoutingContext ctx, String paramName) {
        return ctx.request().getParam(paramName);
    }

    public static Boolean getParamBool(RoutingContext ctx, String paramName) {
        String paramStr = ctx.request().getParam(paramName);
        if (paramStr == null || paramStr.isEmpty()) {
            return null;
        }
        try {
            return Boolean.parseBoolean(paramStr);
        } catch (NumberFormatException e) {
            log.error("Failed to parse boolean parameter: {}", paramStr, e);
            return null;
        }
    }
}
