package com.cky.proxy.server.domain.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Data;

import java.util.Date;

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
    @DatabaseField(columnName="proxy_client_id")
    private Integer proxyClientId;
    /**
     * 规则名称
     */
    @DatabaseField
    private String name;
    /**
     * 服务端监听端口
     */
    @DatabaseField(columnName="server_port")
    private Integer serverPort;
    /**
     * 客户端转发地址，格式为 ip:port
     */
    @DatabaseField(columnName="client_address")
    private String clientAddress;
    /**
     * 启用标记
     */
    @DatabaseField(columnName="enable_flag")
    private Boolean enableFlag;
    /**
     * 创建人
     */
    @DatabaseField(columnName="create_by")
    private String createBy;
    /**
     * 创建时间
     */
    @DatabaseField(columnName="create_time")    
    private Date createTime;
    /**
     * 更新人
     */
    @DatabaseField(columnName="update_by")  
    private String updateBy;
    /**
     * 更新时间
     */
    @DatabaseField(columnName="update_time")
    private Date updateTime;
}
