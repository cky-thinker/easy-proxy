package com.cky.proxy.server.domain.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Data;

import java.util.Date;

/**
 * 系统账号
 */
@Data
@DatabaseTable(tableName = "sys_user")
public class SysUser {
    @DatabaseField(generatedId = true)
    public Integer id;
    /**
     * 账号
     */
    @DatabaseField(unique = true)
    public String username;
    /**
     * 手机号
     */
    @DatabaseField(unique = true)
    public String mobile;
    /**
     * 邮箱
     */
    @DatabaseField(unique = true)
    public String email;
    /**
     * 密码
     */
    @DatabaseField
    public String password;
    /**
     * 角色 admin 管理员 user 普通用户
     */
    @DatabaseField
    public String role;
    /**
     * 启用标记
     */
    @DatabaseField(columnName="enable_flag")
    private Boolean enableFlag;
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
