package com.cky.proxy.server.domain.entity;

import java.util.Date;

import org.h2.command.dml.Update;

import com.cky.proxy.server.consts.AddGroup;
import com.cky.proxy.server.consts.UpdateGroup;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * 代理规则
 */
@DatabaseTable(tableName = "proxy_client_rule")
public class ProxyClientRule {
    @DatabaseField(generatedId = true)
    @NotNull(groups = { UpdateGroup.class })
    private Integer id;
    /**
     * 代理客户端ID
     */
    @DatabaseField(columnName = "proxy_client_id")
    @NotNull(groups = { AddGroup.class })
    private Integer proxyClientId;
    /**
     * 规则名称
     */
    @DatabaseField
    @NotEmpty(groups = { AddGroup.class })
    private String name;
    /**
     * 服务端监听端口
     */
    @DatabaseField(columnName = "server_port")
    @NotNull(groups = { AddGroup.class })
    private Integer serverPort;
    /**
     * 客户端转发地址，格式为 ip:port
     */
    @DatabaseField(columnName = "client_address")
    @NotEmpty(groups = { AddGroup.class })
    private String clientAddress;
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

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getProxyClientId() {
        return proxyClientId;
    }

    public void setProxyClientId(Integer proxyClientId) {
        this.proxyClientId = proxyClientId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getServerPort() {
        return serverPort;
    }

    public void setServerPort(Integer serverPort) {
        this.serverPort = serverPort;
    }

    public String getClientAddress() {
        return clientAddress;
    }

    public void setClientAddress(String clientAddress) {
        this.clientAddress = clientAddress;
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
