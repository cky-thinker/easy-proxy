package com.cky.proxy.server.domain.dto;

import lombok.Data;

@Data
public class ProxyClientRuleReq {
    private String q;
    private Integer serverPort;
    private Integer proxyClientId;
}
