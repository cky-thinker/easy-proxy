package com.cky.proxy.server.domain.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

import com.cky.proxy.server.consts.AddGroup;
import com.cky.proxy.server.consts.UpdateGroup;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 代理流量统计天报告
 */
@Data
@DatabaseTable(tableName = "ts_day_report")
public class TsDayReport {
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
     * 代理客户端规则ID
     */
    @DatabaseField(columnName = "proxy_client_rule_id")
    @NotNull(groups = { AddGroup.class }, message = "代理客户端规则ID不能为空")
    private Integer proxyClientRuleId;
    /**
     * 日期
     */
    @DatabaseField(columnName = "date")
    @NotNull(groups = { AddGroup.class }, message = "日期不能为空")
    private Date date;
    /**
     * 出站/上传字节（egress）
     */
    @DatabaseField(columnName = "upward_traffic_bytes")
    private Long uploadBytes;
    /**
     * 入站/下载字节（ingress）
     */
    @DatabaseField(columnName = "downward_traffic_bytes")
    private Long downloadBytes;
    /**
     * 更新时间
     */
    @DatabaseField(columnName = "create_time")
    private Date createTime;

}
