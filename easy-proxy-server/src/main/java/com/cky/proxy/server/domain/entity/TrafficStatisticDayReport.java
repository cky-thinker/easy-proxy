package com.cky.proxy.server.domain.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Data;

import java.util.Date;

/**
 * 代理流量统计天报告
 */
@Data
@DatabaseTable(tableName = "traffic_statistic_day_report")
public class TrafficStatisticDayReport {
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
     * 上行流量
     */
    @DatabaseField(columnName = "upward_traffic_bytes")
    private Long upwardTrafficBytes;
    /**
     * 下行流量
     */
    @DatabaseField(columnName = "downward_traffic_bytes")
    private Long downwardTrafficBytes;
    /**
     * 更新时间
     */
    @DatabaseField(columnName = "update_time")
    private Date updateTime;
}
