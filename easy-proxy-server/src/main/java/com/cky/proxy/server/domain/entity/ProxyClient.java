package com.cky.proxy.server.domain.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

/**
 * 代理客户端
 */
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

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getEnableFlag() {
        return enableFlag;
    }

    public void setEnableFlag(Boolean enableFlag) {
        this.enableFlag = enableFlag;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
