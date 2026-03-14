package com.cky.proxy.server.controller;

import com.cky.proxy.common.consts.OnlineStatus;
import com.cky.proxy.server.domain.dto.TrafficRankingDTO;
import com.cky.proxy.server.domain.dto.TrafficTrendDTO;
import com.cky.proxy.server.service.ProxyClientRuleService;
import com.cky.proxy.server.service.ProxyClientService;
import com.cky.proxy.server.socket.manager.TrafficStatisticManager;
import com.cky.proxy.server.util.BeanContext;
import com.cky.proxy.server.util.ResponseUtil;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cky.proxy.server.domain.entity.ProxyClient;
import com.cky.proxy.server.domain.entity.ProxyClientRule;

@Slf4j
public class DashboardController {
    private final Router router;
    private final ProxyClientService proxyClientService;
    private final ProxyClientRuleService proxyClientRuleService;

    public DashboardController(Router router) {
        this.router = router;
        this.proxyClientService = new ProxyClientService();
        this.proxyClientRuleService = new ProxyClientRuleService();
        initRoutes();
    }

    private void initRoutes() {
        router.get("/api/dashboard/stats").handler(this::getDashboardStats);
        router.get("/api/dashboard/trafficRanking").handler(this::getTrafficRanking);
        router.get("/api/dashboard/trafficTrend").handler(this::getTrafficTrend);
        router.get("/api/dashboard/recentActivities").handler(this::getRecentActivities);
    }

    @SneakyThrows
    private void getDashboardStats(RoutingContext ctx) {
            List<ProxyClient> clients = proxyClientService.getProxyClients();
            int online = 0, offline = 0;
            for (ProxyClient c : clients) {
                if (OnlineStatus.online.name().equalsIgnoreCase(String.valueOf(c.getStatus())))
                    online++;
                else
                    offline++;
            }


            Map<String, Object> data = new HashMap<>();
            data.put("onlineClients", online);
            data.put("offlineClients", offline);
            data.put("uploadSpeed", TrafficStatisticManager.getTotalUploadSpeed());
            data.put("downloadSpeed", TrafficStatisticManager.getTotalDownloadSpeed());

            ResponseUtil.success(ctx, data);
    }

    @SneakyThrows
    private void getTrafficRanking(RoutingContext ctx) {
        String period = ctx.request().getParam("period");
        if (period == null || period.isEmpty())
            period = "day";
        java.util.Calendar cal = java.util.Calendar.getInstance();
        java.util.Date end = cal.getTime();
        java.util.Date start;
        
        List<TrafficRankingDTO> results;
        
        if ("day".equals(period)) {
            cal.add(java.util.Calendar.HOUR_OF_DAY, -24);
            start = cal.getTime();
            results = BeanContext.getTsHourReportDao().getTrafficRanking(start, end, 5);
        } else {
            if ("week".equals(period)) {
                cal.add(java.util.Calendar.DAY_OF_MONTH, -7);
            } else {
                cal.add(java.util.Calendar.DAY_OF_MONTH, -30);
            }
            start = cal.getTime();
            results = BeanContext.getTsDayReportDao().getTrafficRanking(start, end, 5);
        }
        
        java.util.List<Map<String, Object>> list = new java.util.ArrayList<>();

        // 预取规则名称
        java.util.Map<Integer, String> ruleMap = new java.util.HashMap<>();
        if (!results.isEmpty()) {
            for (ProxyClientRule r : proxyClientRuleService.getAllProxyClientRules(null, null, null)) {
                ruleMap.put(r.getId(), r.getName());
            }
        }

        for (TrafficRankingDTO row : results) {
            Integer ruleId = row.getProxyClientRuleId();
            long total = row.getTotalTraffic();
            String name = ruleMap.getOrDefault(ruleId, "规则" + ruleId);
            
            java.util.Map<String, Object> item = new java.util.HashMap<>();
            item.put("name", name);
            item.put("traffic", total);
            list.add(item);
        }
        ResponseUtil.success(ctx, list);
    }

    @SneakyThrows
    private void getTrafficTrend(RoutingContext ctx) {
        String period = ctx.request().getParam("period");
        if (period == null || period.isEmpty())
            period = "day";
        java.util.Calendar cal = java.util.Calendar.getInstance();
        java.util.Date end = cal.getTime();
        java.util.Date start;
        
        List<TrafficTrendDTO> results;
        
        if ("day".equals(period)) {
            cal.add(java.util.Calendar.DAY_OF_MONTH, -1);
            start = cal.getTime();
            results = BeanContext.getTsHourReportDao().getTrafficTrend(start, end);
        } else {
            if ("week".equals(period)) {
                cal.add(java.util.Calendar.DAY_OF_MONTH, -7);
            } else {
                cal.add(java.util.Calendar.DAY_OF_MONTH, -30);
            }
            // 设置为起始当天的 00:00:00，确保覆盖完整的一天
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
            cal.set(java.util.Calendar.MINUTE, 0);
            cal.set(java.util.Calendar.SECOND, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);
            start = cal.getTime();
            results = BeanContext.getTsDayReportDao().getTrafficTrend(start, end);
        }
        
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        java.util.List<java.util.Map<String, Object>> data = new java.util.ArrayList<>();
        for (TrafficTrendDTO row : results) {
            java.util.Map<String, Object> item = new java.util.HashMap<>();
            item.put("time", sdf.format(row.getDate()));
            item.put("upload", row.getUploadBytes());
            item.put("download", row.getDownloadBytes());
            data.add(item);
        }
        ResponseUtil.success(ctx, data);
    }

    @SneakyThrows
    private void getRecentActivities(RoutingContext ctx) {
            // 取最近10条系统日志
            var page = new cn.hutool.db.Page(0, 5);
            var service = new com.cky.proxy.server.service.SysLogService();
            var result = service.getSysLogsPageable(page, null, null);
            java.util.List<java.util.Map<String, Object>> list = new java.util.ArrayList<>();
            for (var log : result.getList()) {
                java.util.Map<String, Object> item = new java.util.HashMap<>();
                item.put("message", log.getLogContent());
                item.put("time", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(log.getCreateTime()));
                item.put("type", log.getLogType() != null ? log.getLogType() : "info");
                list.add(item);
            }
            ResponseUtil.success(ctx, list);
    }
}
