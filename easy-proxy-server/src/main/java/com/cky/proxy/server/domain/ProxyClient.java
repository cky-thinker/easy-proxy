package com.cky.proxy.server.domain;

import java.time.LocalDateTime;

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
     * 分组id，默认0
     */
    @DatabaseField
     private Integer groupId;
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
     private LocalDateTime createTime;
    /**
     * 更新人
     */
    @DatabaseField
     private String updateBy;
    /**
     * 更新时间
     */
    @DatabaseField
     private LocalDateTime updateTime;
}
