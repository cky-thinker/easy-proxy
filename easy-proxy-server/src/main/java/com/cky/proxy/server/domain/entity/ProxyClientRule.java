package com.cky.proxy.server.domain.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Data;

import java.sql.Date;

/**
 * 代理客户端规则
 */
@Data
@DatabaseTable(tableName = "proxy_client_rule")
public class ProxyClientRule {
    @DatabaseField(generatedId = true)
    private Integer id;
    /**
     * 代理客户端ID
     */
    @DatabaseField
    private Integer proxyClientId;
    /**
     * 规则名称
     */
    @DatabaseField
    private String name;
    /**
     * 服务端监听端口
     */
    @DatabaseField
    private int serverPort;
    /**
     * 客户端转发地址，格式为 ip:port
     */
    @DatabaseField
    private String clientAddress;
    /**
     * 启用标记
     */
    @DatabaseField
    private Boolean enableFlag;
    /**
     * 创建人
     */
    @DatabaseField
    private String createBy;
    /**
     * 创建时间
     */
    @DatabaseField
    private Date createTime;
    /**
     * 更新人
     */
    @DatabaseField
    private String updateBy;
    /**
     * 更新时间
     */
    @DatabaseField
    private Date updateTime;
}
