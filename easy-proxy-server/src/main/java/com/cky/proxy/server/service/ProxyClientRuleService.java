package com.cky.proxy.server.service;

import java.util.Date;
import java.util.List;

import com.cky.proxy.server.dao.ProxyClientRuleDao;
import com.cky.proxy.server.dao.SysLogDao;
import com.cky.proxy.server.domain.dto.PageResult;
import com.cky.proxy.server.domain.entity.ProxyClientRule;
import com.cky.proxy.server.domain.entity.SysLog;
import com.cky.proxy.server.util.BeanContext;
import com.cky.proxy.server.util.EventBusUtil;

import cn.hutool.db.Page;

public class ProxyClientRuleService {
    private final ProxyClientRuleDao proxyClientRuleDao = BeanContext.getProxyClientRuleDao();
    private final SysLogDao sysLogDao = BeanContext.getSysLogDao();

    public List<ProxyClientRule> getProxyClientRules(Integer proxyClientId) {
        return proxyClientRuleDao.selectByProxyClientId(proxyClientId);
    }

    /**
     * 查询所有代理客户端规则，支持按名称、服务端口、客户端ID筛选
     */
    public List<ProxyClientRule> getAllProxyClientRules(String name, Integer serverPort, Integer proxyClientId) {
        return proxyClientRuleDao.selectList(wrapper -> {
            if (name != null && !name.isEmpty()) {
                wrapper.like("name", name);
            }
            if (serverPort != null) {
                wrapper.eq("server_port", serverPort);
            }
            if (proxyClientId != null) {
                wrapper.eq("proxy_client_id", proxyClientId);
            }
        });
    }

    /**
     * 分页查询代理客户端规则，支持按名称、服务端口、客户端ID筛选
     */
    public PageResult<ProxyClientRule> getProxyClientRulesPageable(Page page, String name, Integer serverPort,
            Integer proxyClientId) {
        return proxyClientRuleDao.selectPage(page, wrapper -> {
            if (name != null && !name.isEmpty()) {
                wrapper.like("name", name);
            }
            if (serverPort != null) {
                wrapper.eq("server_port", serverPort);
            }
            if (proxyClientId != null) {
                wrapper.eq("proxy_client_id", proxyClientId);
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
        validateForCreate(rule);

        rule.setCreateTime(new Date());
        if (rule.getEnableFlag() == null) rule.setEnableFlag(Boolean.TRUE);
        proxyClientRuleDao.insert(rule);
        
        // 记录日志
        SysLog sysLog = new SysLog();
        sysLog.setLogType("RULE_ADD");
        sysLog.setLogContent("添加规则: " + rule.getName());
        sysLog.setCreateTime(new Date());
        sysLogDao.insert(sysLog);
        
        EventBusUtil.publish(EventBusUtil.DB_RULE_ADD, rule.getId());
        
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
        validateForUpdate(existingRule, rule);
        if (rule.getName() != null && !rule.getName().equals(existingRule.getName())) {
            existingRule.setName(rule.getName());
        }
        if (rule.getServerPort() != null && !rule.getServerPort().equals(existingRule.getServerPort())) {
            existingRule.setServerPort(rule.getServerPort());
        }
        if (rule.getClientAddress() != null && !rule.getClientAddress().equals(existingRule.getClientAddress())) {
            existingRule.setClientAddress(rule.getClientAddress());
        }
        if (rule.getLimitConn() != null) {
            existingRule.setLimitConn(rule.getLimitConn());
        }
        if (rule.getLimitRate() != null) {
            existingRule.setLimitRate(rule.getLimitRate());
        }
        if (rule.getEnableFlag() != null) {
            existingRule.setEnableFlag(rule.getEnableFlag());
        }
        existingRule.setUpdateBy("system");
        existingRule.setUpdateTime(new Date());

        proxyClientRuleDao.updateById(existingRule);
        
        // 记录日志
        SysLog sysLog = new SysLog();
        sysLog.setLogType("RULE_UPDATE");
        sysLog.setLogContent("更新规则: " + existingRule.getName());
        sysLog.setCreateTime(new Date());
        sysLogDao.insert(sysLog);
        
        EventBusUtil.publish(EventBusUtil.DB_RULE_UPDATE, existingRule.getId());
        
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
        
        // 记录日志
        SysLog sysLog = new SysLog();
        sysLog.setLogType("RULE_DELETE");
        sysLog.setLogContent("删除规则: " + existingRule.getName());
        sysLog.setCreateTime(new Date());
        sysLogDao.insert(sysLog);
        
        EventBusUtil.publish(EventBusUtil.DB_RULE_DELETE, id);
        
        return true;
    }

    // ===== 私有校验方法 =====
    private void validateRuleNameUnique(Integer proxyClientId, String name, Integer excludeId) {
        if (name == null) return;
        boolean exists = !proxyClientRuleDao.selectList(wrapper -> {
            wrapper.eq("proxy_client_id", proxyClientId).eq("name", name);
            if (excludeId != null) {
                wrapper.ne("id", excludeId);
            }
        }).isEmpty();
        if (exists) throw new RuntimeException("同客户端下规则名称已存在");
    }

    private void validateServerPortRangeAndUnique(Integer port, Integer excludeId) {
        if (port == null) return;
        if (port < 1 || port > 65535) throw new RuntimeException("服务端口范围为 1-65535");
        boolean exists = !proxyClientRuleDao.selectList(wrapper -> {
            wrapper.eq("server_port", port);
            if (excludeId != null) {
                wrapper.ne("id", excludeId);
            }
        }).isEmpty();
        if (exists) throw new RuntimeException("服务端口已被占用");
    }

    private void validateClientAddress(String clientAddress) {
        if (clientAddress == null) return;
        int idx = clientAddress.lastIndexOf(":");
        if (idx <= 0 || idx >= clientAddress.length() - 1) throw new RuntimeException("客户端地址格式应为 host:port");
        String portStr = clientAddress.substring(idx + 1);
        try {
            int p = Integer.parseInt(portStr);
            if (p < 1 || p > 65535) throw new RuntimeException("客户端地址端口范围为 1-65535");
        } catch (NumberFormatException ex) {
            throw new RuntimeException("客户端地址端口格式不正确");
        }
    }

    private void validateLimits(Integer limitConn, Integer limitRate) {
        if (limitConn != null && limitConn < 0) throw new RuntimeException("连接数限制不能为负数");
        if (limitRate != null && limitRate < 0) throw new RuntimeException("带宽限制不能为负数");
    }

    private void validateForCreate(ProxyClientRule rule) {
        validateRuleNameUnique(rule.getProxyClientId(), rule.getName(), null);
        validateServerPortRangeAndUnique(rule.getServerPort(), null);
        validateClientAddress(rule.getClientAddress());
        validateLimits(rule.getLimitConn(), rule.getLimitRate());
    }

    private void validateForUpdate(ProxyClientRule existing, ProxyClientRule patch) {
        if (patch.getProxyClientId() != null && !patch.getProxyClientId().equals(existing.getProxyClientId())) {
            throw new RuntimeException("所属客户端不允许变更");
        }
        if (patch.getName() != null && !patch.getName().equals(existing.getName())) {
            Integer pid = existing.getProxyClientId();
            validateRuleNameUnique(pid, patch.getName(), patch.getId());
        }
        if (patch.getServerPort() != null && !patch.getServerPort().equals(existing.getServerPort())) {
            validateServerPortRangeAndUnique(patch.getServerPort(), patch.getId());
        }
        if (patch.getClientAddress() != null && !patch.getClientAddress().equals(existing.getClientAddress())) {
            validateClientAddress(patch.getClientAddress());
        }
        validateLimits(patch.getLimitConn(), patch.getLimitRate());
    }
}
