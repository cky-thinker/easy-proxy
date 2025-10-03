package com.cky.proxy.server.domain.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

/**
 * 代理流量统计小时报告
 */
@DatabaseTable(tableName = "traffic_statistic_hour_report")
public class TrafficStatisticHourReport {
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
    @DatabaseField(columnName="update_time")
    private Date updateTime;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getProxyClientRuleId() { return proxyClientRuleId; }
    public void setProxyClientRuleId(Integer proxyClientRuleId) { this.proxyClientRuleId = proxyClientRuleId; }

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    public Long getUpwardTrafficBytes() { return upwardTrafficBytes; }
    public void setUpwardTrafficBytes(Long upwardTrafficBytes) { this.upwardTrafficBytes = upwardTrafficBytes; }

    public Long getDownwardTrafficBytes() { return downwardTrafficBytes; }
    public void setDownwardTrafficBytes(Long downwardTrafficBytes) { this.downwardTrafficBytes = downwardTrafficBytes; }

    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
