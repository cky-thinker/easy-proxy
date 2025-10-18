package com.cky.proxy.server.util;

import cn.hutool.db.Page;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import java.util.Set;

@Slf4j
public class RequestUtil {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

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
        T obj = JsonUtil.parseJson(bodyStr, clazz);
        if (obj == null) {
            return null;
        }
        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(obj);
        if (!violations.isEmpty()) {
            ConstraintViolationException ex = new ConstraintViolationException((Set) violations);
            ctx.fail(400, ex);
            throw ex;
        }
        return obj;
    }

    public static <T> T getParamsObj(RoutingContext ctx, Class<T> clazz) {
        String paramStr = ctx.request().params().toString();
        if (paramStr == null || paramStr.isEmpty()) {
            return null;
        }
        T obj = JsonUtil.parseJson(paramStr, clazz);
        if (obj == null) {
            return null;
        }
        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(obj);
        if (!violations.isEmpty()) {
            ConstraintViolationException ex = new ConstraintViolationException((Set) violations);
            ctx.fail(400, ex);
            throw ex;
        }
        return obj;
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
}