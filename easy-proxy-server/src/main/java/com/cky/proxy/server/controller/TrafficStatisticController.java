package com.cky.proxy.server.controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.cky.proxy.server.domain.dto.PageResult;
import com.cky.proxy.server.domain.entity.TrafficStatisticClientReport;
import com.cky.proxy.server.domain.entity.TrafficStatisticClientRuleReport;
import com.cky.proxy.server.domain.entity.TrafficStatisticDayReport;
import com.cky.proxy.server.domain.entity.TrafficStatisticHourReport;
import com.cky.proxy.server.service.TrafficStatisticService;
import com.cky.proxy.server.util.PageUtil;
import com.cky.proxy.server.util.VertxUtil;

import cn.hutool.db.Page;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class TrafficStatisticController {
    private final Router router;
    private final TrafficStatisticService statisticService;

    public TrafficStatisticController(Router router) {
        this.router = router;
        this.statisticService = new TrafficStatisticService();
        initRoutes();
    }

    private void initRoutes() {
        // 客户端总报告分页
        router.get("/api/traffic/clientReport").handler(this::getClientReportsPageable);
        // 客户端规则总报告分页
        router.get("/api/traffic/clientRuleReport").handler(this::getClientRuleReportsPageable);
        // 天报告分页
        router.get("/api/traffic/dayReport").handler(this::getDayReportsPageable);
        // 小时报告分页
        router.get("/api/traffic/hourReport").handler(this::getHourReportsPageable);
    }

    private Integer getIntegerParam(RoutingContext ctx, String name) {
        String val = ctx.request().getParam(name);
        if (val == null || val.isEmpty()) return null;
        try { return Integer.parseInt(val); } catch (NumberFormatException e) { return null; }
    }

    private Date getDateParam(RoutingContext ctx, String name) {
        String val = ctx.request().getParam(name);
        if (val == null || val.isEmpty()) return null;
        try {
            // 支持 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss
            SimpleDateFormat sdf = val.length() > 10 ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss") : new SimpleDateFormat("yyyy-MM-dd");
            return sdf.parse(val);
        } catch (ParseException e) {
            return null;
        }
    }

    private void getClientReportsPageable(RoutingContext ctx) {
        Page page = PageUtil.getPage(ctx);
        Integer proxyClientId = getIntegerParam(ctx, "proxyClientId");
        Date startDate = getDateParam(ctx, "startDate");
        Date endDate = getDateParam(ctx, "endDate");
        PageResult<TrafficStatisticClientReport> result = statisticService.getClientReportsPageable(page, proxyClientId, startDate, endDate);
        VertxUtil.success(ctx, result);
    }

    private void getClientRuleReportsPageable(RoutingContext ctx) {
        Page page = PageUtil.getPage(ctx);
        Integer proxyClientRuleId = getIntegerParam(ctx, "proxyClientRuleId");
        Date startDate = getDateParam(ctx, "startDate");
        Date endDate = getDateParam(ctx, "endDate");
        PageResult<TrafficStatisticClientRuleReport> result = statisticService.getClientRuleReportsPageable(page, proxyClientRuleId, startDate, endDate);
        VertxUtil.success(ctx, result);
    }

    private void getDayReportsPageable(RoutingContext ctx) {
        Page page = PageUtil.getPage(ctx);
        Integer proxyClientRuleId = getIntegerParam(ctx, "proxyClientRuleId");
        Date startDate = getDateParam(ctx, "startDate");
        Date endDate = getDateParam(ctx, "endDate");
        PageResult<TrafficStatisticDayReport> result = statisticService.getDayReportsPageable(page, proxyClientRuleId, startDate, endDate);
        VertxUtil.success(ctx, result);
    }

    private void getHourReportsPageable(RoutingContext ctx) {
        Page page = PageUtil.getPage(ctx);
        Integer proxyClientRuleId = getIntegerParam(ctx, "proxyClientRuleId");
        Date startDate = getDateParam(ctx, "startDate");
        Date endDate = getDateParam(ctx, "endDate");
        PageResult<TrafficStatisticHourReport> result = statisticService.getHourReportsPageable(page, proxyClientRuleId, startDate, endDate);
        VertxUtil.success(ctx, result);
    }
}