package com.cky.proxy.common.domain;

import lombok.Data;

import java.util.List;

@Data
public class ProxyClientConfig {
    /**
     * 客户端名称
     */
    private String name;
    /**
     * token
     */
    private String token;
    /**
     * 在线状态 online offline
     */
    private String status;
    /**
     * 使用流量
     */
    private long usedTraffic;
    /**
     * 启用标记
     */
    private Boolean enableFlag;
    /**
     * 转发规则
     */
    private List<ProxyRule> proxyRules;
}
