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
 * 系统账号
 */
@Data
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)
    @NotNull(groups = { UpdateGroup.class }, message = "ID不能为空")
    private Integer id;
    /**
     * 账号
     */
    @NotEmpty(groups = { AddGroup.class }, message = "账号不能为空")
    private String username;
    /**
     * 手机号
     */
    private String mobile;
    /**
     * 邮箱
     */
    private String email;
    /**
     * 密码
     */
    @NotEmpty(groups = { AddGroup.class }, message = "密码不能为空")
    private String password;
    /**
     * 角色 admin 管理员 user 普通用户
     */
    @NotEmpty(groups = { AddGroup.class }, message = "角色不能为空")
    private String role;
    /**
     * 头像地址
     */
    private String avatar;
    /**
     * 启用标记
     */
    @TableField("enable_flag")
    private Boolean enableFlag = true;
    /**
     * 上次登录时间
     */
    @TableField("login_time")
    private Date loginTime;
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
