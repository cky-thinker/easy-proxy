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
