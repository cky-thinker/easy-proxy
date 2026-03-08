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
        
        // Real-time speed (Passive)
        // 记录上一秒完成时的速度
        volatile long lastUploadSpeed = 0;
        volatile long lastDownloadSpeed = 0;
        // 当前秒正在累积的流量
        final AtomicLong currentSecondUpload = new AtomicLong(0);
        final AtomicLong currentSecondDownload = new AtomicLong(0);
        // 上次更新时间戳
        volatile long lastUploadTime = System.currentTimeMillis();
        volatile long lastDownloadTime = System.currentTimeMillis();

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
            TrafficStats stats = getStats(ctx);
            stats.upload.addAndGet(bytes);
            
            // 惰性更新速度计算
            long now = System.currentTimeMillis();
            synchronized (stats) {
                if (now - stats.lastUploadTime >= 1000) {
                    // 如果跨秒，将当前累计值作为上一秒的速度，并重置计数
                    stats.lastUploadSpeed = stats.currentSecondUpload.getAndSet(0);
                    stats.lastUploadTime = now;
                }
            }
            stats.currentSecondUpload.addAndGet(bytes);
        }
    }

    /**
     * 记录下载流量 (Client -> Server -> User)
     */
    public static void addDownload(String userId, long bytes) {
        TrafficContext ctx = connectionMap.get(userId);
        if (ctx != null) {
            TrafficStats stats = getStats(ctx);
            stats.download.addAndGet(bytes);
            
            // 惰性更新速度计算
            long now = System.currentTimeMillis();
            synchronized (stats) {
                if (now - stats.lastDownloadTime >= 1000) {
                    stats.lastDownloadSpeed = stats.currentSecondDownload.getAndSet(0);
                    stats.lastDownloadTime = now;
                }
            }
            stats.currentSecondDownload.addAndGet(bytes);
        }
    }

    /**
     * 获取上传速度 (bytes/s)
     */
    public static long getUploadSpeed(Integer ruleId) {
        TrafficStats stats = statsMap.get(ruleId);
        if (stats == null) return 0;
        
        long now = System.currentTimeMillis();
        // 如果超过1.5秒没有流量更新，说明当前速度早已归零
        // 这里的1500ms是一个容错值，避免因为轻微的调度延迟导致闪烁
        if (now - stats.lastUploadTime > 1500) {
            return 0;
        }
        return stats.lastUploadSpeed;
    }

    /**
     * 获取下载速度 (bytes/s)
     */
    public static long getDownloadSpeed(Integer ruleId) {
        TrafficStats stats = statsMap.get(ruleId);
        if (stats == null) return 0;
        
        long now = System.currentTimeMillis();
        if (now - stats.lastDownloadTime > 1500) {
            return 0;
        }
        return stats.lastDownloadSpeed;
    }

    /**
     * 获取总上行速度 (bytes/s)
     */
    public static long getTotalUploadSpeed() {
        long total = 0;
        for (Integer ruleId : statsMap.keySet()) {
            total += getUploadSpeed(ruleId);
        }
        return total;
    }

    /**
     * 获取总下行速度 (bytes/s)
     */
    public static long getTotalDownloadSpeed() {
        long total = 0;
        for (Integer ruleId : statsMap.keySet()) {
            total += getDownloadSpeed(ruleId);
        }
        return total;
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
