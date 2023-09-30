package com.cky.proxy.common.domain;

import lombok.Data;

import java.util.List;

@Data
public class ProxyClientConfig {
    private String name;
    private String token;
    private List<ProxyRule> proxyRules;
}
