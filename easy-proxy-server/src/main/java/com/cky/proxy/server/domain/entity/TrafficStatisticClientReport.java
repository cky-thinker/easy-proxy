package com.cky.proxy.server.domain.entity;

import java.util.Date;

import com.cky.proxy.server.consts.AddGroup;
import com.cky.proxy.server.consts.UpdateGroup;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 流量统计总报告
 */
@Data
@DatabaseTable(tableName = "traffic_statistic_client_report")
public class TrafficStatisticClientReport {
    /**
     * 主键
     */
    @DatabaseField(generatedId = true)
    @NotNull(groups = { UpdateGroup.class }, message = "ID不能为空")
    private Integer id;
     /**
     * 代理客户端ID
     */
    @DatabaseField(columnName = "proxy_client_id")
    @NotNull(groups = { AddGroup.class }, message = "代理客户端ID不能为空")
    private Integer proxyClientId;
    /**
     * 日期
     */
    @DatabaseField(columnName = "date")
    @NotNull(groups = { AddGroup.class }, message = "日期不能为空")
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
}
