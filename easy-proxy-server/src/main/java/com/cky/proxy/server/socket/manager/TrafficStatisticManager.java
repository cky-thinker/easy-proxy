package com.cky.proxy.server.socket.manager;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cky.proxy.server.domain.entity.TsDayReport;
import com.cky.proxy.server.domain.entity.TsHourReport;
import com.cky.proxy.server.domain.entity.TsReport;
import com.cky.proxy.server.mapper.TsDayReportMapper;
import com.cky.proxy.server.mapper.TsHourReportMapper;
import com.cky.proxy.server.mapper.TsReportMapper;
import com.cky.proxy.server.util.BeanContext;
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
        // Rate limiting config (KB/s)
        volatile int limitRate = 0;

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
     * 更新规则限流配置
     */
    public static void updateRuleLimit(Integer ruleId, Integer limitRate) {
        TrafficStats stats = statsMap.get(ruleId);
        if (stats != null && limitRate != null) {
            stats.limitRate = limitRate;
        }
    }

    /**
     * 检查是否超过带宽限制 (使用缓存的配置)
     */
    public static boolean isRateExceeded(Integer ruleId, int bytes) {
        TrafficStats stats = statsMap.get(ruleId);
        if (stats == null) return false;
        return isRateExceeded(ruleId, stats.limitRate, bytes);
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
     * Get recommended wait time in ms based on current usage and limit.
     * Should be called immediately after isRateExceeded returns true.
     */
    public static long getWaitTime(Integer ruleId) {
        TrafficStats stats = statsMap.get(ruleId);
        if (stats == null || stats.limitRate <= 0) return 0;

        synchronized (stats) {
            long limitBytes = stats.limitRate * 1024L;
            long current = stats.currentSecondBytes.get();
            if (current <= limitBytes) return 0;

            return (current - limitBytes) * 1000 / limitBytes;
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

        TsHourReportMapper hourMapper = BeanContext.getTsHourReportMapper();
        TsDayReportMapper dayMapper = BeanContext.getTsDayReportMapper();
        TsReportMapper totalMapper = BeanContext.getTsReportMapper();

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
                hourMapper.insert(hourReport);

                // 2. 更新天报告 (TsDayReport)
                updateDayReport(dayMapper, stats, up, down, today, now);

                // 3. 更新总报告 (TsReport)
                updateTotalReport(totalMapper, stats, up, down, now);

            } catch (Exception e) {
                log.error("Failed to flush stats for rule {}", stats.ruleId, e);
            }
        }
        log.info("Flush traffic statistics finished.");
    }

    private static void updateDayReport(TsDayReportMapper mapper, TrafficStats stats, long up, long down, Date today, Date now) throws Exception {
        TsDayReport report = mapper.selectOne(new QueryWrapper<TsDayReport>()
                .eq("proxy_client_rule_id", stats.ruleId)
                .eq("date", today));

        if (report == null) {
            report = new TsDayReport();
            report.setProxyClientId(stats.clientId);
            report.setProxyClientRuleId(stats.ruleId);
            report.setDate(today);
            report.setUploadBytes(up);
            report.setDownloadBytes(down);
            report.setCreateTime(now);
            mapper.insert(report);
        } else {
            report.setUploadBytes((report.getUploadBytes() == null ? 0 : report.getUploadBytes()) + up);
            report.setDownloadBytes((report.getDownloadBytes() == null ? 0 : report.getDownloadBytes()) + down);
            mapper.updateById(report);
        }
    }

    private static void updateTotalReport(TsReportMapper mapper, TrafficStats stats, long up, long down, Date now) throws Exception {
        TsReport report = mapper.selectOne(new QueryWrapper<TsReport>()
                .eq("proxy_client_rule_id", stats.ruleId));

        if (report == null) {
            report = new TsReport();
            report.setProxyClientId(stats.clientId);
            report.setProxyClientRuleId(stats.ruleId);
            report.setUploadBytes(up);
            report.setDownloadBytes(down);
            report.setCreateTime(now);
            report.setUpdateTime(now);
            mapper.insert(report);
        } else {
            report.setUploadBytes((report.getUploadBytes() == null ? 0 : report.getUploadBytes()) + up);
            report.setDownloadBytes((report.getDownloadBytes() == null ? 0 : report.getDownloadBytes()) + down);
            report.setUpdateTime(now);
            mapper.updateById(report);
        }
    }
}
