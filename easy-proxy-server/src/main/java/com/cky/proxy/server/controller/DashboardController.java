package com.cky.proxy.server.controller;

import com.cky.proxy.server.domain.dto.Result;
import com.cky.proxy.server.service.ProxyClientRuleService;
import com.cky.proxy.server.service.ProxyClientService;
import com.cky.proxy.server.util.BeanContext;
import com.cky.proxy.server.util.ResponseUtil;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cky.proxy.server.domain.entity.ProxyClient;
import com.cky.proxy.server.domain.entity.ProxyClientRule;

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

    private void getDashboardStats(RoutingContext ctx) {
        try {
            List<ProxyClient> clients = proxyClientService.getProxyClients();
            int online = 0, offline = 0;
            for (ProxyClient c : clients) {
                if ("online".equalsIgnoreCase(String.valueOf(c.getStatus()))) online++; else offline++;
            }

            List<ProxyClientRule> allRules = proxyClientRuleService.getAllProxyClientRules(null, null, null);
            int activeConnections = 0;
            for (ProxyClientRule r : allRules) {
                if (Boolean.TRUE.equals(r.getEnableFlag())) activeConnections++;
            }

            String sumSql = "SELECT COALESCE(SUM(upward_traffic_bytes) + SUM(downward_traffic_bytes), 0) FROM ts_day_report";
            String[] res = BeanContext.getTrafficStatisticDayReportDao().getDao()
                    .queryRaw(sumSql).getFirstResult();
            long totalTraffic = 0L;
            if (res != null && res.length > 0 && res[0] != null) {
                totalTraffic = Long.parseLong(res[0]);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("onlineClients", online);
            data.put("offlineClients", offline);
            data.put("totalTraffic", totalTraffic);
            data.put("activeConnections", activeConnections);

            ResponseUtil.success(ctx, data);
        } catch (Exception e) {
            ResponseUtil.error(ctx, 500, "Failed to load dashboard stats: " + e.getMessage());
        }
    }

    private void getTrafficRanking(RoutingContext ctx) {
        try {
            String period = ctx.request().getParam("period");
            if (period == null || period.isEmpty()) period = "day";
            java.util.Calendar cal = java.util.Calendar.getInstance();
            java.util.Date end = cal.getTime();
            if ("day".equals(period)) { cal.add(java.util.Calendar.DAY_OF_MONTH, -1); }
            else if ("week".equals(period)) { cal.add(java.util.Calendar.DAY_OF_MONTH, -7); }
            else if ("month".equals(period)) { cal.add(java.util.Calendar.DAY_OF_MONTH, -30); }
            java.util.Date start = cal.getTime();

            String sql = "SELECT proxy_client_id, COALESCE(SUM(upward_traffic_bytes) + SUM(downward_traffic_bytes), 0) AS total FROM ts_day_report WHERE date >= ? AND date <= ? GROUP BY proxy_client_id ORDER BY total DESC LIMIT 10";
            String[] params = new String[] { new java.sql.Timestamp(start.getTime()).toString(), new java.sql.Timestamp(end.getTime()).toString() };
            var res = BeanContext.getTrafficStatisticDayReportDao().getDao().queryRaw(sql, params);
            java.util.List<Map<String, Object>> list = new java.util.ArrayList<>();

            // 预取规则连接数
            java.util.Map<Integer, Integer> connMap = new java.util.HashMap<>();
            for (ProxyClientRule r : proxyClientRuleService.getAllProxyClientRules(null, null, null)) {
                if (Boolean.TRUE.equals(r.getEnableFlag())) {
                    connMap.merge(r.getProxyClientId(), 1, Integer::sum);
                }
            }

            // 将结果映射为排行项
            java.util.Map<Integer, ProxyClient> clientMap = new java.util.HashMap<>();
            for (ProxyClient c : proxyClientService.getProxyClients()) clientMap.put(c.getId(), c);

            for (String[] row : res.getResults()) {
                Integer clientId = Integer.valueOf(row[0]);
                long total = Long.parseLong(row[1]);
                ProxyClient client = clientMap.get(clientId);
                String name = client != null ? client.getName() : ("客户端" + clientId);
                String ip = "-";
                // 从任一规则的 client_address 提取 ip
                java.util.List<ProxyClientRule> rules = proxyClientRuleService.getProxyClientRules(clientId);
                if (!rules.isEmpty()) {
                    String addr = rules.get(0).getClientAddress();
                    if (addr != null && addr.contains(":")) ip = addr.split(":")[0];
                }
                int connections = connMap.getOrDefault(clientId, 0);
                java.util.Map<String, Object> item = new java.util.HashMap<>();
                item.put("name", name);
                item.put("ip", ip);
                item.put("traffic", total);
                item.put("connections", connections);
                list.add(item);
            }
            ResponseUtil.success(ctx, list);
        } catch (Exception e) {
            ResponseUtil.error(ctx, 500, "Failed to load ranking: " + e.getMessage());
        }
    }

    private void getTrafficTrend(RoutingContext ctx) {
        try {
            String period = ctx.request().getParam("period");
            if (period == null || period.isEmpty()) period = "day";
            java.util.Calendar cal = java.util.Calendar.getInstance();
            java.util.Date end = cal.getTime();
            java.util.Date start;
            String sql;
            if ("day".equals(period)) {
                cal.add(java.util.Calendar.DAY_OF_MONTH, -1);
                start = cal.getTime();
                sql = "SELECT date, COALESCE(SUM(upward_traffic_bytes),0) AS upload, COALESCE(SUM(downward_traffic_bytes),0) AS download FROM ts_hour_report WHERE date >= ? AND date <= ? GROUP BY date ORDER BY date ASC";
            } else {
                if ("week".equals(period)) cal.add(java.util.Calendar.DAY_OF_MONTH, -7); else cal.add(java.util.Calendar.DAY_OF_MONTH, -30);
                start = cal.getTime();
                sql = "SELECT date, COALESCE(SUM(upward_traffic_bytes),0) AS upload, COALESCE(SUM(downward_traffic_bytes),0) AS download FROM ts_day_report WHERE date >= ? AND date <= ? GROUP BY date ORDER BY date ASC";
            }
            String[] params = new String[] { new java.sql.Timestamp(start.getTime()).toString(), new java.sql.Timestamp(end.getTime()).toString() };
            var res = BeanContext.getTrafficStatisticDayReportDao().getDao().queryRaw(sql, params);
            java.util.List<java.util.Map<String, Object>> data = new java.util.ArrayList<>();
            for (String[] row : res.getResults()) {
                java.util.Map<String, Object> item = new java.util.HashMap<>();
                item.put("time", row[0]);
                item.put("upload", Long.parseLong(row[1]));
                item.put("download", Long.parseLong(row[2]));
                data.add(item);
            }
            ResponseUtil.success(ctx, data);
        } catch (Exception e) {
            ResponseUtil.error(ctx, 500, "Failed to load trend: " + e.getMessage());
        }
    }

    private void getRecentActivities(RoutingContext ctx) {
        try {
            // 取最近10条系统日志
            var page = new cn.hutool.db.Page(0, 10);
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
        } catch (Exception e) {
            ResponseUtil.error(ctx, 500, "Failed to load activities: " + e.getMessage());
        }
    }
}