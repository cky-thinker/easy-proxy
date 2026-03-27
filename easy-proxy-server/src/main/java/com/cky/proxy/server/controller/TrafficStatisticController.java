package com.cky.proxy.server.controller;

import java.util.Date;

import cn.hutool.db.Page;
import com.cky.proxy.server.http.HttpContext;
import com.cky.proxy.server.http.HttpRouter;
import com.cky.proxy.server.domain.dto.PageResult;
import com.cky.proxy.server.domain.dto.ClientTrafficDayReport;
import com.cky.proxy.server.domain.entity.TsReport;
import com.cky.proxy.server.domain.entity.TsDayReport;
import com.cky.proxy.server.domain.entity.TsHourReport;
import com.cky.proxy.server.service.TrafficStatisticService;
import com.cky.proxy.server.socket.manager.TrafficStatisticManager;
import com.cky.proxy.server.util.BeanContext;
import com.cky.proxy.server.util.RequestUtil;
import com.cky.proxy.server.util.ResponseUtil;

import java.util.HashMap;
import java.util.Map;

public class TrafficStatisticController {
    private final HttpRouter router;
    private final TrafficStatisticService statisticService;

    public TrafficStatisticController(HttpRouter router) {
        this.router = router;
        this.statisticService = BeanContext.getTrafficStatisticService();
        initRoutes();
    }

    private void initRoutes() {
        // 客户端总报告分页
        router.get("/api/traffic/clientReport", this::getClientReportsPageable);
        // 客户端规则总报告分页
        router.get("/api/traffic/clientRuleReport", this::getClientRuleReportsPageable);
        // 天报告分页
        router.get("/api/traffic/dayReport", this::getDayReportsPageable);
        // 小时报告分页
        router.get("/api/traffic/hourReport", this::getHourReportsPageable);
        // 规则实时流量
        router.get("/api/traffic/realtime", this::getRealtimeTraffic);
    }

    private void getRealtimeTraffic(HttpContext ctx) {
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

    private void getClientReportsPageable(HttpContext ctx) {
        Page page = RequestUtil.getPage(ctx);
        Integer proxyClientId = RequestUtil.getParamInt(ctx, "proxyClientId");
        Date startDate = RequestUtil.getParamDate(ctx, "startDate");
        Date endDate = RequestUtil.getParamDate(ctx, "endDate");
        PageResult<ClientTrafficDayReport> result = statisticService.getClientReportsPageable(page, proxyClientId,
                startDate, endDate);
        ResponseUtil.success(ctx, result);
    }

    private void getClientRuleReportsPageable(HttpContext ctx) {
        Page page = RequestUtil.getPage(ctx);
        Integer proxyClientRuleId = RequestUtil.getParamInt(ctx, "proxyClientRuleId");
        Date startDate = RequestUtil.getParamDate(ctx, "startDate");
        Date endDate = RequestUtil.getParamDate(ctx, "endDate");
        PageResult<TsReport> result = statisticService.getClientRuleReportsPageable(page,
                proxyClientRuleId, startDate, endDate);
        ResponseUtil.success(ctx, result);
    }

    private void getDayReportsPageable(HttpContext ctx) {
        Page page = RequestUtil.getPage(ctx);
        Integer proxyClientRuleId = RequestUtil.getParamInt(ctx, "proxyClientRuleId");
        Date startDate = RequestUtil.getParamDate(ctx, "startDate");
        Date endDate = RequestUtil.getParamDate(ctx, "endDate");
        PageResult<TsDayReport> result = statisticService.getDayReportsPageable(page, proxyClientRuleId,startDate, endDate);
        ResponseUtil.success(ctx, result);
    }

    private void getHourReportsPageable(HttpContext ctx) {
        Page page = RequestUtil.getPage(ctx);
        Integer proxyClientRuleId = RequestUtil.getParamInt(ctx, "proxyClientRuleId");
        Date startDate = RequestUtil.getParamDate(ctx, "startDate");
        Date endDate = RequestUtil.getParamDate(ctx, "endDate");
        PageResult<TsHourReport> result = statisticService.getHourReportsPageable(page, proxyClientRuleId,
                startDate, endDate);
        ResponseUtil.success(ctx, result);
    }
}