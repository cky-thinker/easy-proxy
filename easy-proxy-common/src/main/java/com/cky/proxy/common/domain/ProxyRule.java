package com.cky.proxy.common.domain;

import lombok.Data;

@Data
public class ProxyRule {
    /**
     * 规则名称
     */
    private String name;
    /**
     * 服务端监听端口
     */
    private int serverPort;
    /**
     * 客户端转发地址，格式为 ip:port
     */
    private String clientAddress;
    /**
     * 启用标记
     */
    private Boolean enableFlag;
}
