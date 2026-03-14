package com.cky.proxy.server.domain.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 系统日志
 */
@Data
@TableName("sys_log")
public class SysLog {
    @TableId(type = IdType.AUTO)
    private Integer id;
    /**
     * 日志类型
     */
    @TableField("log_type")
    private String logType;
    /**
     * 日志内容
     */
    @TableField("log_content")
    private String logContent;
    /**
     * 创建时间
     */
    @TableField("create_time")
    private Date createTime;
}
