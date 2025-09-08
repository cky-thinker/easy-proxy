package com.cky.proxy.server.service;

import com.cky.proxy.server.bean.entity.ProxyClientRule;
import com.cky.proxy.server.dao.ProxyClientRuleDao;

import java.util.List;

public class ProxyClientRuleService {
    private ProxyClientRuleDao proxyClientRuleDao = new ProxyClientRuleDao();

    public List<ProxyClientRule> getProxyClientRules(Integer proxyClientId) {
        return proxyClientRuleDao.selectList(qb -> {
            qb.where().eq("proxy_client_id", proxyClientId);
        });
    }
}
