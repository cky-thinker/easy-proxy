package com.cky.proxy.server.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cky.proxy.common.consts.OnlineStatus;
import com.cky.proxy.server.domain.dto.PageResult;
import com.cky.proxy.server.domain.dto.ClientImportDTO;
import com.cky.proxy.server.domain.dto.ProxyClientImportReq;
import com.cky.proxy.server.domain.dto.RuleImportDTO;
import com.cky.proxy.server.domain.entity.ProxyClient;
import com.cky.proxy.server.domain.entity.ProxyClientRule;
import com.cky.proxy.server.domain.entity.SysLog;
import com.cky.proxy.server.mapper.ProxyClientMapper;
import com.cky.proxy.server.mapper.ProxyClientRuleMapper;
import com.cky.proxy.server.mapper.SysLogMapper;
import com.cky.proxy.server.util.BeanContext;
import com.cky.proxy.server.util.EventBusUtil;
import com.cky.proxy.server.util.PageUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.List;

@Slf4j
public class ProxyClientService {
    private final ProxyClientMapper proxyClientMapper = BeanContext.getProxyClientMapper();
    private final ProxyClientRuleMapper proxyClientRuleMapper = BeanContext.getProxyClientRuleMapper();
    private final SysLogMapper sysLogMapper = BeanContext.getSysLogMapper();

    /**
     * 查询所有代理客户端
     */
    public List<ProxyClient> getProxyClients() {
        return proxyClientMapper.selectList(new QueryWrapper<>());
    }

    public ProxyClient selectByToken(String token) {
        return proxyClientMapper.selectOne(new QueryWrapper<ProxyClient>().eq("token", token));
    }

    /**
     * 分页查询代理客户端，支持 name、status、enableFlag 条件
     */
    public PageResult<ProxyClient> getProxyClientsPageable(cn.hutool.db.Page hutoolPage, String name, String status,
                                                           Boolean enableFlag) {

        QueryWrapper<ProxyClient> wrapper = new QueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            wrapper.like("name", name);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq("status", status);
        }
        if (enableFlag != null) {
            wrapper.eq("enable_flag", enableFlag);
        }

        IPage<ProxyClient> page = PageUtil.toMybatisPage(hutoolPage);
        IPage<ProxyClient> result = proxyClientMapper.selectPage(page, wrapper);
        return PageUtil.toPageResult(hutoolPage, result);
    }

    /**
     * 根据ID查询代理客户端详情
     */
    public ProxyClient getProxyClientById(Integer id) {
        return proxyClientMapper.selectById(id);
    }

    /**
     * 添加代理客户端
     */
    public ProxyClient addProxyClient(ProxyClient proxyClient) {
        validateForCreate(proxyClient);

        proxyClient.setCreateTime(new Date());
        if (proxyClient.getEnableFlag() == null)
            proxyClient.setEnableFlag(Boolean.TRUE);
        if (proxyClient.getStatus() == null)
            proxyClient.setStatus("offline");
        proxyClientMapper.insert(proxyClient);

        // 记录日志
        SysLog sysLog = new SysLog();
        sysLog.setLogType("CLIENT_ADD");
        sysLog.setLogContent("添加客户端: " + proxyClient.getName());
        sysLog.setCreateTime(new Date());
        sysLogMapper.insert(sysLog);

        EventBusUtil.publish(EventBusUtil.DB_CLIENT_ADD, proxyClient.getId());

        return proxyClient;
    }

    /**
     * 更新代理客户端
     */
    public ProxyClient updateProxyClient(ProxyClient proxyClient) {
        if (proxyClient == null || proxyClient.getId() == null) {
            throw new RuntimeException("请求体缺少 id");
        }
        ProxyClient existingClient = proxyClientMapper.selectById(proxyClient.getId());
        if (existingClient == null) {
            return null;
        }
        validateForUpdate(existingClient, proxyClient);

        if (proxyClient.getName() != null && !proxyClient.getName().equals(existingClient.getName())) {
            existingClient.setName(proxyClient.getName());
        }
        if (proxyClient.getToken() != null && !proxyClient.getToken().equals(existingClient.getToken())) {
            existingClient.setToken(proxyClient.getToken());
        }
        if (proxyClient.getEnableFlag() != null) {
            existingClient.setEnableFlag(proxyClient.getEnableFlag());
        }
        existingClient.setUpdateBy("admin");
        existingClient.setUpdateTime(new Date());

        proxyClientMapper.updateById(existingClient);

        EventBusUtil.publish(EventBusUtil.DB_CLIENT_UPDATE, existingClient.getId());

        // 记录日志
        SysLog sysLog = new SysLog();
        sysLog.setLogType("CLIENT_UPDATE");
        sysLog.setLogContent("更新客户端: " + existingClient.getName());
        sysLog.setCreateTime(new Date());
        sysLogMapper.insert(sysLog);

        return existingClient;
    }

    /**
     * 删除代理客户端
     */
    public boolean deleteProxyClient(Integer id) {
        ProxyClient existingClient = proxyClientMapper.selectById(id);
        if (existingClient == null) {
            throw new RuntimeException("客户端不存在");
        }

        List<ProxyClientRule> rules = proxyClientRuleMapper.selectList(new QueryWrapper<ProxyClientRule>().eq("proxy_client_id", id));
        if (!rules.isEmpty()) {
            throw new RuntimeException("客户端仍有关联规则，无法删除");
        }

        proxyClientMapper.deleteById(id);

        // 发布删除事件
        EventBusUtil.publish(EventBusUtil.DB_CLIENT_DELETE, id);

        // 记录日志
        SysLog sysLog = new SysLog();
        sysLog.setLogType("CLIENT_DELETE");
        sysLog.setLogContent("删除客户端: " + existingClient.getName());
        sysLog.setCreateTime(new Date());
        sysLogMapper.insert(sysLog);

        return true;
    }

    /**
     * 更新客户端在线状态
     */
    public ProxyClient updateClientStatus(String token, String status) {
        ProxyClient client = proxyClientMapper.selectOne(new QueryWrapper<ProxyClient>().eq("token", token));
        if (client == null) {
            throw new RuntimeException("客户端不存在");
        }
        client.setStatus(status);
        client.setUpdateTime(new Date());
        proxyClientMapper.updateById(client);
        log.info("EP>>ProxyClientService>> Update proxy client status, id: {}, status: {}", client.getId(), status);

        // 记录日志
        SysLog sysLog = new SysLog();
        sysLog.setLogType("CLIENT_STATUS");
        sysLog.setLogContent("客户端状态更新: [" + client.getName() + "] [" + OnlineStatus.valueOf(status).getDesc() + "]");
        sysLog.setCreateTime(new Date());
        sysLogMapper.insert(sysLog);

        return client;
    }

    /**
     * 批量导入客户端及规则
     */
    public void importClients(ProxyClientImportReq req) {
        if (req == null || req.getClients() == null || req.getClients().isEmpty()) {
            return;
        }

        // 1. 预校验
        for (ClientImportDTO clientDto : req.getClients()) {
            // 校验客户端名称
            validateNameUnique(clientDto.getName(), null);
            // 校验Token
            validateTokenFormat(clientDto.getClientKey());
            validateTokenUnique(clientDto.getClientKey(), null);

            // 校验规则
            if (clientDto.getProxyMappings() != null) {
                for (RuleImportDTO ruleDto : clientDto.getProxyMappings()) {
                    // 校验端口
                    validateServerPortRangeAndUnique(ruleDto.getInetPort());
                    // 校验目标地址格式
                    validateClientAddress(ruleDto.getLan());
                }
            }
        }

        // 2. 执行导入
        for (ClientImportDTO clientDto : req.getClients()) {
            ProxyClient client = new ProxyClient();
            client.setName(clientDto.getName());
            client.setToken(clientDto.getClientKey());
            client.setEnableFlag(req.getEnableFlag());
            client.setStatus("offline");
            client.setCreateTime(new Date());
            
            proxyClientMapper.insert(client);
            
            // 记录日志
            SysLog sysLog = new SysLog();
            sysLog.setLogType("CLIENT_IMPORT");
            sysLog.setLogContent("导入客户端: " + client.getName());
            sysLog.setCreateTime(new Date());
            sysLogMapper.insert(sysLog);
            
            EventBusUtil.publish(EventBusUtil.DB_CLIENT_ADD, client.getId());

            if (clientDto.getProxyMappings() != null) {
                for (RuleImportDTO ruleDto : clientDto.getProxyMappings()) {
                    ProxyClientRule rule = new ProxyClientRule();
                    rule.setProxyClientId(client.getId());
                    rule.setName(ruleDto.getName());
                    rule.setServerPort(ruleDto.getInetPort());
                    rule.setClientAddress(ruleDto.getLan());
                    rule.setEnableFlag(req.getEnableFlag());
                    rule.setCreateTime(new Date());
                    
                    proxyClientRuleMapper.insert(rule);
                    
                    // 记录日志
                    SysLog ruleLog = new SysLog();
                    ruleLog.setLogType("RULE_IMPORT");
                    ruleLog.setLogContent("导入规则: " + rule.getName());
                    ruleLog.setCreateTime(new Date());
                    sysLogMapper.insert(ruleLog);
                    
                    EventBusUtil.publish(EventBusUtil.DB_RULE_ADD, rule.getId());
                }
            }
        }
    }

    // ===== 私有校验方法 =====
    private void validateServerPortRangeAndUnique(Integer port) {
        if (port == null) return;
        if (port < 1 || port > 65535) throw new RuntimeException("服务端口范围为 1-65535");
        
        QueryWrapper<ProxyClientRule> wrapper = new QueryWrapper<>();
        wrapper.eq("server_port", port);
        boolean exists = !proxyClientRuleMapper.selectList(wrapper).isEmpty();
        if (exists) throw new RuntimeException("服务端口 " + port + " 已被占用");
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

    private void validateNameUnique(String name, Integer excludeId) {
        if (name == null)
            return;

        QueryWrapper<ProxyClient> wrapper = new QueryWrapper<>();
        wrapper.eq("name", name);
        if (excludeId != null) {
            wrapper.ne("id", excludeId);
        }
        boolean exists = !proxyClientMapper.selectList(wrapper).isEmpty();
        if (exists)
            throw new RuntimeException("客户端名称已存在");
    }

    private void validateTokenFormat(String token) {
        if (token == null)
            return;
        if (!token.matches("^[0-9a-fA-F]{32}$")) {
            throw new RuntimeException("Token 必须为32位十六进制字符串");
        }
    }

    private void validateTokenUnique(String token, Integer excludeId) {
        if (token == null)
            return;
        QueryWrapper<ProxyClient> wrapper = new QueryWrapper<>();
        wrapper.eq("token", token);
        if (excludeId != null) {
            wrapper.ne("id", excludeId);
        }
        boolean exists = !proxyClientMapper.selectList(wrapper).isEmpty();
        if (exists)
            throw new RuntimeException("Token 已存在");
    }

    private void validateForCreate(ProxyClient client) {
        validateNameUnique(client.getName(), null);
        validateTokenFormat(client.getToken());
        validateTokenUnique(client.getToken(), null);
    }

    private void validateForUpdate(ProxyClient existing, ProxyClient patch) {
        if (patch.getName() != null && !patch.getName().equals(existing.getName())) {
            validateNameUnique(patch.getName(), patch.getId());
        }
        if (patch.getToken() != null && !patch.getToken().equals(existing.getToken())) {
            validateTokenFormat(patch.getToken());
            validateTokenUnique(patch.getToken(), patch.getId());
        }
    }
}
