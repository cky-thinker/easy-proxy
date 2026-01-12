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
    private final com.cky.proxy.server.dao.ProxyClientDao proxyClientDao = BeanContext.getProxyClientDao();

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
            boolean hasWhere = false;
            if (name != null && !name.isEmpty()) {
                qb.where().like("name", "%" + name + "%");
                hasWhere = true;
            }
            if (serverPort != null) {
                if (hasWhere) qb.where().and();
                qb.where().eq("server_port", serverPort);
                hasWhere = true;
            }
            if (proxyClientId != null) {
                if (hasWhere) qb.where().and();
                qb.where().eq("proxy_client_id", proxyClientId);
            }
            // 当所有条件为空时，不调用 qb.where()，以返回全部
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
        if (rule == null || rule.getId() == null) {
            throw new RuntimeException("请求体缺少 id");
        }
        ProxyClientRule existingRule = proxyClientRuleDao.selectById(rule.getId());
        if (existingRule == null) {
            return null;
        }

        // 若传入且变更 proxyClientId
        if (rule.getProxyClientId() != null && !rule.getProxyClientId().equals(existingRule.getProxyClientId())) {
            throw new RuntimeException("所属客户端不允许变更");
        }

        // name 唯一（在同一 proxyClientId 下）
        if (rule.getName() != null && !rule.getName().equals(existingRule.getName())) {
            Integer pid = rule.getProxyClientId() != null ? rule.getProxyClientId() : existingRule.getProxyClientId();
            boolean exists = !proxyClientRuleDao.selectList(qb -> {
                qb.where().eq("proxy_client_id", pid).and().eq("name", rule.getName()).and().ne("id", rule.getId());
            }).isEmpty();
            if (exists) throw new RuntimeException("同客户端下规则名称已存在");
            existingRule.setName(rule.getName());
        }

        // serverPort 校验：范围 + 全局唯一
        if (rule.getServerPort() != null && !rule.getServerPort().equals(existingRule.getServerPort())) {
            int port = rule.getServerPort();
            if (port < 1 || port > 65535) throw new RuntimeException("服务端口范围为 1-65535");
            boolean exists = !proxyClientRuleDao.selectList(qb -> {
                qb.where().eq("server_port", port).and().ne("id", rule.getId());
            }).isEmpty();
            if (exists) throw new RuntimeException("服务端口已被占用");
            existingRule.setServerPort(port);
        }

        // clientAddress 基础格式校验：host:port（端口范围 1-65535）
        if (rule.getClientAddress() != null && !rule.getClientAddress().equals(existingRule.getClientAddress())) {
            String ca = rule.getClientAddress();
            int idx = ca.lastIndexOf(":");
            if (idx <= 0 || idx >= ca.length() - 1) throw new RuntimeException("客户端地址格式应为 host:port");
            String portStr = ca.substring(idx + 1);
            try {
                int p = Integer.parseInt(portStr);
                if (p < 1 || p > 65535) throw new RuntimeException("客户端地址端口范围为 1-65535");
            } catch (NumberFormatException ex) {
                throw new RuntimeException("客户端地址端口格式不正确");
            }
            existingRule.setClientAddress(ca);
        }

        // limitConn / limitRate 非负
        if (rule.getLimitConn() != null) {
            if (rule.getLimitConn() < 0) throw new RuntimeException("连接数限制不能为负数");
            existingRule.setLimitConn(rule.getLimitConn());
        }
        if (rule.getLimitRate() != null) {
            if (rule.getLimitRate() < 0) throw new RuntimeException("带宽限制不能为负数");
            existingRule.setLimitRate(rule.getLimitRate());
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
