package com.cky.proxy.server.domain.entity;

import java.util.Date;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import lombok.Data;

/**
 * 代理客户端配置
 */
@Data
@DatabaseTable(tableName = "proxy_client")
public class ProxyClient {
    @DatabaseField(generatedId = true)
    private Integer id;
    /**
     * 客户端名称
     */
    @DatabaseField
    private String name;
    /**
     * token
     */
    @DatabaseField
    private String token;
    /**
     * 在线状态 online offline
     */
    @DatabaseField
    private String status;
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
