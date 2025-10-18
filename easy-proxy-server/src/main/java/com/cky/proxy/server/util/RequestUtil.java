package com.cky.proxy.server.util;

import cn.hutool.db.Page;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
import io.vertx.ext.web.RoutingContext;

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
}