package com.cky.proxy.server.domain.entity;

import java.sql.Date;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import lombok.Data;

/**
 * 代理客户端分组
 */
@Data
@DatabaseTable(tableName = "proxy_client_group")
public class ProxyClientGroup {
    @DatabaseField(generatedId = true)
    private Integer id;
    /**
     * 分组名称
     */
    @DatabaseField
    private String name;
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
