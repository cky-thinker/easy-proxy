package com.cky.proxy.server.controller;

import java.util.Date;

import com.cky.proxy.server.domain.dto.PageResult;
import com.cky.proxy.server.domain.dto.ClientTrafficDayReport;
import com.cky.proxy.server.domain.entity.TsReport;
import com.cky.proxy.server.domain.entity.TsDayReport;
import com.cky.proxy.server.domain.entity.TsHourReport;
import com.cky.proxy.server.service.TrafficStatisticService;
import com.cky.proxy.server.socket.manager.TrafficStatisticManager;
import com.cky.proxy.server.util.RequestUtil;
import com.cky.proxy.server.util.ResponseUtil;

import java.util.HashMap;
import java.util.Map;

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
        // 规则实时流量
        router.get("/api/traffic/realtime").handler(this::getRealtimeTraffic);
    }

    private void getRealtimeTraffic(RoutingContext ctx) {
        Integer proxyClientRuleId = RequestUtil.getParamInt(ctx, "proxyClientRuleId");
        if (proxyClientRuleId == null) {
            ResponseUtil.error(ctx, 400, "proxyClientRuleId is required");
            return;
        }
        long uploadSpeed = TrafficStatisticManager.getUploadSpeed(proxyClientRuleId);
        long downloadSpeed = TrafficStatisticManager.getDownloadSpeed(proxyClientRuleId);
        
        Map<String, Object> data = new HashMap<>();
        data.put("uploadSpeed", uploadSpeed);
        data.put("downloadSpeed", downloadSpeed);
        
        ResponseUtil.success(ctx, data);
    }

    private void getClientReportsPageable(RoutingContext ctx) {
        Page page = RequestUtil.getPage(ctx);
        Integer proxyClientId = RequestUtil.getParamInt(ctx, "proxyClientId");
        Date startDate = RequestUtil.getParamDate(ctx, "startDate");
        Date endDate = RequestUtil.getParamDate(ctx, "endDate");
        PageResult<ClientTrafficDayReport> result = statisticService.getClientReportsPageable(page, proxyClientId,
                startDate, endDate);
        ResponseUtil.success(ctx, result);
    }

    private void getClientRuleReportsPageable(RoutingContext ctx) {
        Page page = RequestUtil.getPage(ctx);
        Integer proxyClientRuleId = RequestUtil.getParamInt(ctx, "proxyClientRuleId");
        Date startDate = RequestUtil.getParamDate(ctx, "startDate");
        Date endDate = RequestUtil.getParamDate(ctx, "endDate");
        PageResult<TsReport> result = statisticService.getClientRuleReportsPageable(page,
                proxyClientRuleId, startDate, endDate);
        ResponseUtil.success(ctx, result);
    }

    private void getDayReportsPageable(RoutingContext ctx) {
        Page page = RequestUtil.getPage(ctx);
        Integer proxyClientRuleId = RequestUtil.getParamInt(ctx, "proxyClientRuleId");
        Date startDate = RequestUtil.getParamDate(ctx, "startDate");
        Date endDate = RequestUtil.getParamDate(ctx, "endDate");
        PageResult<TsDayReport> result = statisticService.getDayReportsPageable(page, proxyClientRuleId,startDate, endDate);
        ResponseUtil.success(ctx, result);
    }

    private void getHourReportsPageable(RoutingContext ctx) {
        Page page = RequestUtil.getPage(ctx);
        Integer proxyClientRuleId = RequestUtil.getParamInt(ctx, "proxyClientRuleId");
        Date startDate = RequestUtil.getParamDate(ctx, "startDate");
        Date endDate = RequestUtil.getParamDate(ctx, "endDate");
        PageResult<TsHourReport> result = statisticService.getHourReportsPageable(page, proxyClientRuleId,
                startDate, endDate);
        ResponseUtil.success(ctx, result);
    }
}