package com.cky.proxy.server.service;

import com.cky.proxy.server.dao.ProxyClientRuleDao;
import com.cky.proxy.server.domain.entity.ProxyClientRule;

import io.vertx.core.json.JsonObject;

import java.util.Date;
import java.util.List;

public class ProxyClientRuleService {
    private ProxyClientRuleDao proxyClientRuleDao = new ProxyClientRuleDao();

    public List<ProxyClientRule> getProxyClientRules(Integer proxyClientId) {
        return proxyClientRuleDao.selectList(qb -> {
            qb.where().eq("proxy_client_id", proxyClientId);
        });
    }

    /**
     * 查询所有代理客户端规则
     */
    public List<ProxyClientRule> getAllProxyClientRules(String name) {
        return proxyClientRuleDao.selectList(qb -> {
            if (name != null && !name.isEmpty()) {
                qb.where().like("name", "%" + name + "%");
            }
        });
    }

    /**
     * 根据ID查询代理客户端规则详情
     */
    public ProxyClientRule getProxyClientRuleById(Integer id) {
        return proxyClientRuleDao.selectById(id);
    }

    /**
     * 添加代理客户端规则
     */
    public ProxyClientRule addProxyClientRule(JsonObject body) {
        ProxyClientRule rule = new ProxyClientRule();
        rule.setName(body.getString("name"));
        rule.setProxyClientId(body.getInteger("proxyClientId"));
        rule.setServerPort(body.getInteger("serverPort"));
        rule.setClientAddress(body.getString("clientAddress"));
        rule.setEnableFlag(body.getBoolean("enableFlag", false));
        rule.setCreateBy(body.getString("createBy", "system"));
        rule.setCreateTime(new Date());
        
        proxyClientRuleDao.insert(rule);
        return rule;
    }

    /**
     * 更新代理客户端规则
     */
    public ProxyClientRule updateProxyClientRule(ProxyClientRule rule) {
        ProxyClientRule existingRule = proxyClientRuleDao.selectById(rule.getId());
        if (existingRule == null) {
            return null;
        }
        
        if (rule.getName() != null) {
            existingRule.setName(rule.getName());
        }
        if (rule.getProxyClientId() != null) {
            existingRule.setProxyClientId(rule.getProxyClientId());
        }
        if (rule.getServerPort() != null) {
            existingRule.setServerPort(rule.getServerPort());
        }
        if (rule.getClientAddress() != null) {      
            existingRule.setClientAddress(rule.getClientAddress());
        }
        if (rule.getEnableFlag() != null) {
            existingRule.setEnableFlag(rule.getEnableFlag());
        }
        existingRule.setUpdateBy("system");
        existingRule.setUpdateTime(new Date());
        
        proxyClientRuleDao.updateById(existingRule);
        return existingRule;
    }

    /**
     * 删除代理客户端规则
     */
    public boolean deleteProxyClientRule(Integer id) {
        ProxyClientRule existingRule = proxyClientRuleDao.selectById(id);
        if (existingRule == null) {
            return false;
        }
        
        proxyClientRuleDao.deleteById(id);
        return true;
    }
}
