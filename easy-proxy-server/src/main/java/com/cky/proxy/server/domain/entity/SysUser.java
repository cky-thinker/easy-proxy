package com.cky.proxy.server.domain.entity;

import java.util.Date;

import com.cky.proxy.server.consts.AddGroup;
import com.cky.proxy.server.consts.UpdateGroup;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 系统账号
 */
@Data
@DatabaseTable(tableName = "sys_user")
public class SysUser {
    @DatabaseField(generatedId = true)
    @NotNull(groups = { UpdateGroup.class }, message = "ID不能为空")
    private Integer id;
    /**
     * 账号
     */
    @DatabaseField(unique = true)
    @NotEmpty(groups = { AddGroup.class }, message = "账号不能为空")
    private String username;
    /**
     * 手机号
     */
    @DatabaseField(unique = true)
    private String mobile;
    /**
     * 邮箱
     */
    @DatabaseField(unique = true)
    private String email;
    /**
     * 密码
     */
    @DatabaseField
    @NotEmpty(groups = { AddGroup.class }, message = "密码不能为空")
    private String password;
    /**
     * 角色 admin 管理员 user 普通用户
     */
    @DatabaseField
    @NotEmpty(groups = { AddGroup.class }, message = "角色不能为空")
    private String role;
    /**
     * 头像地址
     */
    @DatabaseField
    private String avatar;
    /**
     * 启用标记
     */
    @DatabaseField(columnName = "enable_flag")
    private Boolean enableFlag = true;
    /**
     * 上次登录时间
     */
    @DatabaseField(columnName = "login_time")
    private Date loginTime;
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
}
