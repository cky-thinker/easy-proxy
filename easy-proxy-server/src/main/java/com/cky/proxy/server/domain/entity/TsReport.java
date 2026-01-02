package com.cky.proxy.server.domain.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

import com.cky.proxy.server.consts.AddGroup;
import com.cky.proxy.server.consts.UpdateGroup;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 流量统计总报告
 */
@Data
@DatabaseTable(tableName = "ts_report")
public class TsReport {
    /**
     * 主键
     */
    @DatabaseField(generatedId = true)
    @NotNull(groups = { UpdateGroup.class }, message = "ID不能为空")
    private Integer id;
    /**
     * 代理客户端ID
     */
    @DatabaseField(columnName = "proxy_client_id", index = true)
    @NotNull(groups = { AddGroup.class }, message = "代理客户端ID不能为空")
    private Integer proxyClientId;
     /**
     * 代理客户端规则ID
     */
    @DatabaseField(columnName = "proxy_client_rule_id", index = true, uniqueCombo = true)
    @NotNull(groups = { AddGroup.class }, message = "代理客户端规则ID不能为空")
    private Integer proxyClientRuleId;
    /**
     * 上传字节
     */
    @DatabaseField(columnName = "upload_bytes", defaultValue = "0")
    private Long uploadBytes;
    /**
     * 下载字节
     */
    @DatabaseField(columnName = "download_bytes", defaultValue = "0")
    private Long downloadBytes;
    /**
     * 创建时间
     */
    @DatabaseField(columnName = "create_time")
    private Date createTime;
    /**
     * 更新时间
     */
    @DatabaseField(columnName = "update_time")
    private Date updateTime;
}
