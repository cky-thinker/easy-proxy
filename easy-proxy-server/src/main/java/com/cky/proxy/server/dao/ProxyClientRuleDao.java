package com.cky.proxy.server.dao;

import java.util.List;
import com.cky.proxy.server.domain.entity.ProxyClientRule;
import com.cky.proxy.server.mapper.ProxyClientRuleMapper;

public class ProxyClientRuleDao extends BaseDao<ProxyClientRule, ProxyClientRuleMapper> {
    @Override
    protected Class<ProxyClientRuleMapper> getMapperClass() {
        return ProxyClientRuleMapper.class;
    }

    public List<ProxyClientRule> selectByProxyClientId(Integer proxyClientId) {
        return selectList(wrapper -> wrapper.eq("proxy_client_id", proxyClientId));
    }
}
