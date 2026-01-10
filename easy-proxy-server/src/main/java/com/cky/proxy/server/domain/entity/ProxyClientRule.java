package com.cky.proxy.server.domain.entity;

import java.util.Date;

import com.cky.proxy.server.consts.AddGroup;
import com.cky.proxy.server.consts.UpdateGroup;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 代理规则
 */
@Data
@DatabaseTable(tableName = "proxy_client_rule")
public class ProxyClientRule {
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
     * 规则名称
     */
    @DatabaseField
    @NotEmpty(groups = { AddGroup.class }, message = "规则名称不能为空")
    private String name;
    /**
     * 服务端监听端口
     */
    @DatabaseField(columnName = "server_port")
    @NotNull(groups = { AddGroup.class }, message = "服务端监听端口不能为空")
    private Integer serverPort;
    /**
     * 客户端转发地址，格式为 ip:port
     */
    @DatabaseField(columnName = "client_address")
    @NotEmpty(groups = { AddGroup.class }, message = "客户端转发地址不能为空，格式为 ip:port")
    private String clientAddress;
    /**
     * 连接数限制
     */
    @DatabaseField(columnName = "limit_conn")
    private Integer limitConn;
    /**
     * 带宽限制 KB/s
     */
    @DatabaseField(columnName = "limit_rate")
    private Integer limitRate;
    /**
     * 启用标记
     */
    @DatabaseField(columnName = "enable_flag")
    private Boolean enableFlag;
    /**
     * 创建人
     */
    @DatabaseField(columnName = "create_by")
    private String createBy;
    /**
     * 创建时间
     */
    @DatabaseField(columnName = "create_time")
    private Date createTime;
    /**
     * 更新人
     */
    @DatabaseField(columnName = "update_by")
    private String updateBy;
    /**
     * 更新时间
     */
    @DatabaseField(columnName = "update_time")
    private Date updateTime;
}
