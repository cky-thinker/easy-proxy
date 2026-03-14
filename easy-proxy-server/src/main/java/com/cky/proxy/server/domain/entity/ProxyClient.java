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
 * 代理客户端
 */
@Data
@TableName("proxy_client")
public class ProxyClient {
    @TableId(type = IdType.AUTO)
    @NotNull(groups = { UpdateGroup.class }, message = "ID不能为空")
    private Integer id;
    /**
     * 客户端名称
     */
    @NotEmpty(groups = { AddGroup.class }, message = "客户端名称不能为空")
    private String name;
    /**
     * token
     */
    @NotEmpty(groups = { AddGroup.class }, message = "Token不能为空")
    private String token;
    /**
     * 在线状态 online offline
     */
    private String status = "offline";
    /**
     * 启用标记
     */
    @TableField("enable_flag")
    private Boolean enableFlag = true;
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
