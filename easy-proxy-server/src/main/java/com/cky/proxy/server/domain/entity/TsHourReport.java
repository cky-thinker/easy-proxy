package com.cky.proxy.server.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cky.proxy.server.consts.AddGroup;
import com.cky.proxy.server.consts.UpdateGroup;

import java.util.Date;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 代理流量统计小时报告
 */
@Data
@TableName("ts_hour_report")
public class TsHourReport {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    @NotNull(groups = { UpdateGroup.class }, message = "ID不能为空")
    private Integer id;
    /**
     * 代理客户端ID
     */
    @TableField("proxy_client_id")
    @NotNull(groups = { AddGroup.class }, message = "代理客户端ID不能为空")
    private Integer proxyClientId;
    /**
     * 代理客户端规则ID
     */
    @TableField("proxy_client_rule_id")
    @NotNull(groups = { AddGroup.class }, message = "代理客户端规则ID不能为空")
    private Integer proxyClientRuleId;
    /**
     * 日期
     */
    @TableField("date")
    @NotNull(groups = { AddGroup.class }, message = "日期不能为空")
    private Date date;
    /**
     * 出站/上传字节（egress）
     */
    @TableField("upward_traffic_bytes")
    private Long uploadBytes;
    /**
     * 入站/下载字节（ingress）
     */
    @TableField("downward_traffic_bytes")
    private Long downloadBytes;
    /**
     * 更新时间
     */
    @TableField("create_time")
    private Date createTime;
}
