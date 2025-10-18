package com.cky.proxy.server.domain.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

import com.cky.proxy.server.consts.AddGroup;
import com.cky.proxy.server.consts.UpdateGroup;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 代理客户端
 */
@Data
@DatabaseTable(tableName = "proxy_client")
public class ProxyClient {
    @DatabaseField(generatedId = true)
    @NotNull(groups = { UpdateGroup.class }, message = "ID不能为空")
    private Integer id;
    /**
     * 客户端名称
     */
    @DatabaseField
    @NotEmpty(groups = { AddGroup.class }, message = "客户端名称不能为空")
    private String name;
    /**
     * token
     */
    @DatabaseField
    @NotEmpty(groups = { AddGroup.class }, message = "Token不能为空")
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
