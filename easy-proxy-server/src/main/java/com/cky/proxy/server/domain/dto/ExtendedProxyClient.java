package com.cky.proxy.server.domain.dto;

import java.util.List;

import com.cky.proxy.server.domain.entity.ProxyClient;
import com.cky.proxy.server.domain.entity.ProxyClientRule;

import lombok.Data;

@Data
public class ExtendedProxyClient extends ProxyClient {
    private List<ProxyClientRule> proxyRules;
}