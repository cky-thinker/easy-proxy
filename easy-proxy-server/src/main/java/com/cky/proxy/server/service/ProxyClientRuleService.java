package com.cky.proxy.server.service;

import java.util.Date;
import java.util.List;

import com.cky.proxy.server.dao.ProxyClientRuleDao;
import com.cky.proxy.server.domain.dto.PageResult;
import com.cky.proxy.server.domain.entity.ProxyClientRule;
import com.cky.proxy.server.util.BeanContext;

import cn.hutool.db.Page;

public class ProxyClientRuleService {
    private final ProxyClientRuleDao proxyClientRuleDao = BeanContext.getProxyClientRuleDao();

    public List<ProxyClientRule> getProxyClientRules(Integer proxyClientId) {
        return proxyClientRuleDao.selectList(qb -> {
            qb.where().eq("proxy_client_id", proxyClientId);
        });
    }

    /**
     * 查询所有代理客户端规则，支持按名称、服务端口、客户端ID筛选
     */
    public List<ProxyClientRule> getAllProxyClientRules(String name, Integer serverPort, Integer proxyClientId) {
        return proxyClientRuleDao.selectList(qb -> {
            var where = qb.where();
            boolean hasWhere = false;
            if (name != null && !name.isEmpty()) {
                where.like("name", "%" + name + "%");
                hasWhere = true;
            }
            if (serverPort != null) {
                if (hasWhere) where.and();
                where.eq("server_port", serverPort);
                hasWhere = true;
            }
            if (proxyClientId != null) {
                if (hasWhere) where.and();
                where.eq("proxy_client_id", proxyClientId);
            }
        });
    }

    /**
     * 分页查询代理客户端规则，支持按名称、服务端口、客户端ID筛选
     */
    public PageResult<ProxyClientRule> getProxyClientRulesPageable(Page page, String name, Integer serverPort,
            Integer proxyClientId) {
        return proxyClientRuleDao.selectPage(page, where -> {
            boolean hasWhere = false;
            if (name != null && !name.isEmpty()) {
                where.like("name", "%" + name + "%");
                hasWhere = true;
            }
            if (serverPort != null) {
                if (hasWhere) where.and();
                where.eq("server_port", serverPort);
                hasWhere = true;
            }
            if (proxyClientId != null) {
                if (hasWhere) where.and();
                where.eq("proxy_client_id", proxyClientId);
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
    public ProxyClientRule addProxyClientRule(ProxyClientRule rule) {
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
