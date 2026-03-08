package com.cky.proxy.server.controller;

import com.cky.proxy.common.consts.OnlineStatus;
import com.cky.proxy.server.service.ProxyClientRuleService;
import com.cky.proxy.server.service.ProxyClientService;
import com.cky.proxy.server.socket.manager.TrafficStatisticManager;
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
        } catch (Exception e) {
            ResponseUtil.error(ctx, 500, "Failed to load dashboard stats: " + e.getMessage());
        }
    }

    private void getTrafficRanking(RoutingContext ctx) {
        try {
            String period = ctx.request().getParam("period");
            if (period == null || period.isEmpty())
                period = "day";
            java.util.Calendar cal = java.util.Calendar.getInstance();
            java.util.Date end = cal.getTime();
            if ("day".equals(period)) {
                cal.add(java.util.Calendar.DAY_OF_MONTH, -1);
            } else if ("week".equals(period)) {
                cal.add(java.util.Calendar.DAY_OF_MONTH, -7);
            } else if ("month".equals(period)) {
                cal.add(java.util.Calendar.DAY_OF_MONTH, -30);
            }
            java.util.Date start = cal.getTime();

            String sql = "SELECT proxy_client_id, COALESCE(SUM(upward_traffic_bytes) + SUM(downward_traffic_bytes), 0) AS total FROM ts_day_report WHERE date >= ? AND date <= ? GROUP BY proxy_client_id ORDER BY total DESC LIMIT 5";
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String[] params = new String[] { sdf.format(start), sdf.format(end) };
            var res = BeanContext.getTsDayReportDao().getDao().queryRaw(sql, params);
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
            for (ProxyClient c : proxyClientService.getProxyClients())
                clientMap.put(c.getId(), c);

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
                    if (addr != null && addr.contains(":"))
                        ip = addr.split(":")[0];
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
            if (period == null || period.isEmpty())
                period = "day";
            java.util.Calendar cal = java.util.Calendar.getInstance();
            java.util.Date end = cal.getTime();
            java.util.Date start;
            String sql;
            if ("day".equals(period)) {
                cal.add(java.util.Calendar.DAY_OF_MONTH, -1);
                start = cal.getTime();
                sql = "SELECT date, COALESCE(SUM(upward_traffic_bytes),0) AS upload, COALESCE(SUM(downward_traffic_bytes),0) AS download FROM ts_hour_report WHERE date >= ? AND date <= ? GROUP BY date ORDER BY date ASC";
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
                sql = "SELECT date, COALESCE(SUM(upward_traffic_bytes),0) AS upload, COALESCE(SUM(downward_traffic_bytes),0) AS download FROM ts_day_report WHERE date >= ? AND date <= ? GROUP BY date ORDER BY date ASC";
            }
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String[] params = new String[] { sdf.format(start), sdf.format(end) };
            // 根据 period 决定使用哪个 DAO，因为 ts_hour_report 和 ts_day_report 是不同的表
            com.j256.ormlite.dao.Dao<?, ?> dao;
            if ("day".equals(period)) {
                dao = BeanContext.getTsHourReportDao().getDao();
            } else {
                dao = BeanContext.getTsDayReportDao().getDao();
            }
            var res = dao.queryRaw(sql, params);
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
        } catch (Exception e) {
            ResponseUtil.error(ctx, 500, "Failed to load activities: " + e.getMessage());
        }
    }
}