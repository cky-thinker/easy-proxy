package com.cky.proxy.server.domain.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Data;

import java.util.Date;

/**
 * 系统日志
 */
@Data
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
}
