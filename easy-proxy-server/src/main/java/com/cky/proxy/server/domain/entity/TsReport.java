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
 * 流量统计总报告
 */
@Data
@TableName("ts_report")
public class TsReport {
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
     * 上传字节
     */
    @TableField("upload_bytes")
    private Long uploadBytes;
    /**
     * 下载字节
     */
    @TableField("download_bytes")
    private Long downloadBytes;
    /**
     * 创建时间
     */
    @TableField("create_time")
    private Date createTime;
    /**
     * 更新时间
     */
    @TableField("update_time")
    private Date updateTime;
}
