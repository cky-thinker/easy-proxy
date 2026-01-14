package com.cky.proxy.server.socket.manager;

import cn.hutool.core.date.DateUtil;
import com.cky.proxy.server.dao.TsDayReportDao;
import com.cky.proxy.server.dao.TsHourReportDao;
import com.cky.proxy.server.dao.TsReportDao;
import com.cky.proxy.server.domain.entity.TsDayReport;
import com.cky.proxy.server.domain.entity.TsHourReport;
import com.cky.proxy.server.domain.entity.TsReport;
import com.cky.proxy.server.util.BeanContext;
import com.j256.ormlite.stmt.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 流量统计管理器
 * 负责收集、缓存和持久化代理流量数据
 */
public class TrafficStatisticManager {
    private static final Logger log = LoggerFactory.getLogger(TrafficStatisticManager.class);

    // userId -> Context (包含 ruleId, clientId)
    private static final Map<String, TrafficContext> connectionMap = new ConcurrentHashMap<>();

    // ruleId -> Stats (流量统计)
    private static final Map<Integer, TrafficStats> statsMap = new ConcurrentHashMap<>();

    private static class TrafficContext {
        final Integer clientId;
        final Integer ruleId;

        TrafficContext(Integer clientId, Integer ruleId) {
            this.clientId = clientId;
            this.ruleId = ruleId;
        }
    }

    private static class TrafficStats {
        final Integer clientId;
        final Integer ruleId;
        final AtomicLong upload = new AtomicLong(0);
        final AtomicLong download = new AtomicLong(0);
        
        // Active connections
        final AtomicLong activeConnections = new AtomicLong(0);
        
        // Rate limiting
        final AtomicLong currentSecondBytes = new AtomicLong(0);
        volatile long lastResetTime = System.currentTimeMillis();

        TrafficStats(Integer clientId, Integer ruleId) {
            this.clientId = clientId;
            this.ruleId = ruleId;
        }
    }

    /**
     * 注册新的连接上下文
     */
    public static void addConnection(String userId, Integer clientId, Integer ruleId) {
        connectionMap.put(userId, new TrafficContext(clientId, ruleId));
        getStats(ruleId, clientId).activeConnections.incrementAndGet();
    }

    /**
     * 移除连接上下文
     */
    public static void removeConnection(String userId) {
        TrafficContext ctx = connectionMap.remove(userId);
        if (ctx != null) {
            getStats(ctx).activeConnections.decrementAndGet();
        }
    }

    /**
     * 获取连接对应的规则ID
     */
    public static Integer getRuleId(String userId) {
        TrafficContext ctx = connectionMap.get(userId);
        return ctx != null ? ctx.ruleId : null;
    }

    /**
     * 获取规则的当前活动连接数
     */
    public static long getActiveConnections(Integer ruleId) {
        TrafficStats stats = statsMap.get(ruleId);
        return stats == null ? 0 : stats.activeConnections.get();
    }

    /**
     * 检查是否超过带宽限制
     * @param ruleId 规则ID
     * @param limitRateKB 限制速率(KB/s)
     * @param bytes 当前传输字节数
     * @return true if exceeded
     */
    public static boolean isRateExceeded(Integer ruleId, int limitRateKB, int bytes) {
        if (limitRateKB <= 0) return false;
        TrafficStats stats = statsMap.get(ruleId);
        if (stats == null) return false;
        
        long now = System.currentTimeMillis();
        long limitBytes = limitRateKB * 1024L;
        
        synchronized (stats) {
            if (now - stats.lastResetTime >= 1000) {
                stats.currentSecondBytes.set(0);
                stats.lastResetTime = now;
            }
            long current = stats.currentSecondBytes.addAndGet(bytes);
            return current > limitBytes;
        }
    }

    /**
     * 记录上传流量 (User -> Server -> Client)
     */
    public static void addUpload(String userId, long bytes) {
        TrafficContext ctx = connectionMap.get(userId);
        if (ctx != null) {
            getStats(ctx).upload.addAndGet(bytes);
        }
    }

    /**
     * 记录下载流量 (Client -> Server -> User)
     */
    public static void addDownload(String userId, long bytes) {
        TrafficContext ctx = connectionMap.get(userId);
        if (ctx != null) {
            getStats(ctx).download.addAndGet(bytes);
        }
    }

    private static TrafficStats getStats(TrafficContext ctx) {
        return statsMap.computeIfAbsent(ctx.ruleId, k -> new TrafficStats(ctx.clientId, ctx.ruleId));
    }

    private static TrafficStats getStats(Integer ruleId, Integer clientId) {
        return statsMap.computeIfAbsent(ruleId, k -> new TrafficStats(clientId, ruleId));
    }

    /**
     * 将缓存的统计数据刷新到数据库
     */
    public static void flush() {
        log.info("Start flushing traffic statistics...");
        Date now = new Date();
        Date today = DateUtil.beginOfDay(now);

        TsHourReportDao hourDao = BeanContext.getTsHourReportDao();
        TsDayReportDao dayDao = BeanContext.getTsDayReportDao();
        TsReportDao totalDao = BeanContext.getTsReportDao();

        for (TrafficStats stats : statsMap.values()) {
            long up = stats.upload.getAndSet(0);
            long down = stats.download.getAndSet(0);

            if (up == 0 && down == 0) continue;

            try {
                // 1. 保存小时报告 (TsHourReport)
                TsHourReport hourReport = new TsHourReport();
                hourReport.setProxyClientId(stats.clientId);
                hourReport.setProxyClientRuleId(stats.ruleId);
                hourReport.setDate(now);
                hourReport.setUploadBytes(up);
                hourReport.setDownloadBytes(down);
                hourReport.setCreateTime(now);
                hourDao.insert(hourReport);

                // 2. 更新天报告 (TsDayReport)
                updateDayReport(dayDao, stats, up, down, today, now);

                // 3. 更新总报告 (TsReport)
                updateTotalReport(totalDao, stats, up, down, now);

            } catch (Exception e) {
                log.error("Failed to flush stats for rule {}", stats.ruleId, e);
            }
        }
        log.info("Flush traffic statistics finished.");
    }

    private static void updateDayReport(TsDayReportDao dao, TrafficStats stats, long up, long down, Date today, Date now) throws Exception {
        QueryBuilder<TsDayReport, Integer> qb = dao.getDao().queryBuilder();
        qb.where().eq("proxy_client_rule_id", stats.ruleId).and().eq("date", today);
        TsDayReport report = qb.queryForFirst();

        if (report == null) {
            report = new TsDayReport();
            report.setProxyClientId(stats.clientId);
            report.setProxyClientRuleId(stats.ruleId);
            report.setDate(today);
            report.setUploadBytes(up);
            report.setDownloadBytes(down);
            report.setCreateTime(now);
            dao.insert(report);
        } else {
            // 注意：这里需要处理并发更新，但在单机简单场景下，且flush是单线程执行（定时任务），一般没问题。
            // 如果有多个实例，需要数据库层面的原子更新。目前假设单实例。
            report.setUploadBytes((report.getUploadBytes() == null ? 0 : report.getUploadBytes()) + up);
            report.setDownloadBytes((report.getDownloadBytes() == null ? 0 : report.getDownloadBytes()) + down);
            // 这里不需要更新 createTime，也不需要 updateTime 字段（实体中未定义 updateTime 用于天报表? 
            // 检查实体定义: TsDayReport 只有 createTime，没有 updateTime。
            // 可以在此处不更新时间，只更新数据。
            dao.updateById(report);
        }
    }

    private static void updateTotalReport(TsReportDao dao, TrafficStats stats, long up, long down, Date now) throws Exception {
        QueryBuilder<TsReport, Integer> qb = dao.getDao().queryBuilder();
        qb.where().eq("proxy_client_rule_id", stats.ruleId);
        TsReport report = qb.queryForFirst();

        if (report == null) {
            report = new TsReport();
            report.setProxyClientId(stats.clientId);
            report.setProxyClientRuleId(stats.ruleId);
            report.setUploadBytes(up);
            report.setDownloadBytes(down);
            report.setCreateTime(now);
            report.setUpdateTime(now);
            dao.insert(report);
        } else {
            report.setUploadBytes((report.getUploadBytes() == null ? 0 : report.getUploadBytes()) + up);
            report.setDownloadBytes((report.getDownloadBytes() == null ? 0 : report.getDownloadBytes()) + down);
            report.setUpdateTime(now);
            dao.updateById(report);
        }
    }
}
