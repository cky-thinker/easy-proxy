package com.cky.proxy.server.domain.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Data;

import java.sql.Date;

@Data
@DatabaseTable(tableName = "user")
public class User {
    @DatabaseField(generatedId = true)
    public Integer id;
    /**
     * 头像
     */
    @DatabaseField
    public String avatar;
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
