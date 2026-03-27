package com.cky.proxy.server.domain.dto;

import java.util.List;

import com.cky.proxy.server.domain.entity.ProxyClient;
import com.cky.proxy.server.domain.entity.ProxyClientRule;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ExtendedProxyClient extends ProxyClient {
    private List<ProxyClientRule> proxyRules;
}