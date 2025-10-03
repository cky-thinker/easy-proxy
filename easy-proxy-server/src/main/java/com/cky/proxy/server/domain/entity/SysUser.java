package com.cky.proxy.server.domain.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

/**
 * 系统账号
 */
@DatabaseTable(tableName = "sys_user")
public class SysUser {
    @DatabaseField(generatedId = true)
    private Integer id;
    /**
     * 账号
     */
    @DatabaseField(unique = true)
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
    private String password;
    /**
     * 角色 admin 管理员 user 普通用户
     */
    @DatabaseField
    private String role;
    /**
     * 头像地址
     */
    @DatabaseField
    private String avatar;
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

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public Boolean getEnableFlag() { return enableFlag; }
    public void setEnableFlag(Boolean enableFlag) { this.enableFlag = enableFlag; }

    public Date getLoginTime() { return loginTime; }
    public void setLoginTime(Date loginTime) { this.loginTime = loginTime; }

    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }

    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
