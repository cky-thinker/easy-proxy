package com.cky.proxy.server.domain.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cky.proxy.server.consts.AddGroup;
import com.cky.proxy.server.consts.UpdateGroup;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 代理规则
 */
@Data
@TableName("proxy_client_rule")
public class ProxyClientRule {
    @TableId(type = IdType.AUTO)
    @NotNull(groups = { UpdateGroup.class }, message = "ID不能为空")
    private Integer id;
    /**
     * 代理客户端ID
     */
    @TableField("proxy_client_id")
    @NotNull(groups = { AddGroup.class }, message = "代理客户端ID不能为空")
    private Integer proxyClientId;
    /**
     * 规则名称
     */
    @NotEmpty(groups = { AddGroup.class }, message = "规则名称不能为空")
    private String name;
    /**
     * 服务端监听端口
     */
    @TableField("server_port")
    @NotNull(groups = { AddGroup.class }, message = "服务端监听端口不能为空")
    private Integer serverPort;
    /**
     * 客户端转发地址，格式为 ip:port
     */
    @TableField("client_address")
    @NotEmpty(groups = { AddGroup.class }, message = "客户端转发地址不能为空，格式为 ip:port")
    private String clientAddress;
    /**
     * 连接数限制
     */
    @TableField(value = "limit_conn", updateStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.ALWAYS)
    private Integer limitConn;
    /**
     * 带宽限制 KB/s
     */
    @TableField(value = "limit_rate", updateStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.ALWAYS)
    private Integer limitRate;
    /**
     * 启用标记
     */
    @TableField("enable_flag")
    private Boolean enableFlag;
    /**
     * 创建人
     */
    @TableField("create_by")
    private String createBy;
    /**
     * 创建时间
     */
    @TableField("create_time")
    private Date createTime;
    /**
     * 更新人
     */
    @TableField("update_by")
    private String updateBy;
    /**
     * 更新时间
     */
    @TableField("update_time")
    private Date updateTime;
}
