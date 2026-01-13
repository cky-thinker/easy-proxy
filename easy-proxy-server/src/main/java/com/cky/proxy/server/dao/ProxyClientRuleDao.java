package com.cky.proxy.server.dao;

import java.util.List;

import com.cky.proxy.server.domain.entity.ProxyClientRule;

public class ProxyClientRuleDao extends BaseDao<ProxyClientRule> {
    public List<ProxyClientRule> selectByProxyClientId(Integer proxyClientId) {
        return selectList(rb -> rb.where().eq("proxy_client_id", proxyClientId));
    }
}
