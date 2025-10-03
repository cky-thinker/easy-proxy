package com.cky.proxy.server.domain.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

/**
 * 系统日志
 */
@DatabaseTable(tableName = "sys_log")
public class SysLog {
    @DatabaseField(generatedId = true)
    private Integer id;
    /**
     * 日志类型
     */
    @DatabaseField(columnName = "log_type")
    private String logType;
     /**
     * 日志内容
     */
    @DatabaseField(columnName = "log_content")
    private String logContent;
     /**
     * 创建时间
     */
    @DatabaseField(columnName = "create_time")
    private Date createTime;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getLogType() { return logType; }
    public void setLogType(String logType) { this.logType = logType; }

    public String getLogContent() { return logContent; }
    public void setLogContent(String logContent) { this.logContent = logContent; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
