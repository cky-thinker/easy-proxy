package com.cky.proxy.server.domain.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Data;

import java.util.Date;

/**
 * 代理流量统计报告
 */
@Data
@DatabaseTable(tableName = "traffic_statistic_total_report")
public class TrafficStatisticTotalReport {
    /**
     * 主键
     */
    @DatabaseField(generatedId = true)
    private Integer id;
     /**
     * 代理客户端规则ID
     */
    @DatabaseField(columnName = "proxy_client_rule_id")
    private Integer proxyClientRuleId;
    /**
     * 日期
     */
    @DatabaseField(columnName = "date")
    private Date date;
     /**
     * 已使用流量（单位byte）
     */
    @DatabaseField(columnName = "using_bytes")
    private Long bytes;
    /**
     * 更新时间
     */
    @DatabaseField(columnName="update_time")
    private Date updateTime;
}
