package com.cky.proxy.server.service;

import cn.hutool.db.Page;

import com.cky.proxy.server.dao.ProxyClientDao;
import com.cky.proxy.server.domain.dto.PageResult;
import com.cky.proxy.server.domain.entity.ProxyClient;
import com.cky.proxy.server.util.BeanContext;

import java.util.Date;
import java.util.List;

public class ProxyClientService {
    private final ProxyClientDao proxyClientDao = BeanContext.getProxyClientDao();

    // ===== 私有校验方法 =====
    private void validateNameUnique(String name, Integer excludeId) {
        if (name == null) return;
        boolean exists = !proxyClientDao.selectList(qb -> {
            if (excludeId == null) {
                qb.where().eq("name", name);
            } else {
                qb.where().eq("name", name).and().ne("id", excludeId);
            }
        }).isEmpty();
        if (exists) throw new RuntimeException("客户端名称已存在");
    }

    private void validateTokenFormat(String token) {
        if (token == null) return;
        if (!token.matches("^[0-9a-fA-F]{64}$")) {
            throw new RuntimeException("Token 必须为64位十六进制字符串");
        }
    }

    private void validateTokenUnique(String token, Integer excludeId) {
        if (token == null) return;
        boolean exists = !proxyClientDao.selectList(qb -> {
            if (excludeId == null) {
                qb.where().eq("token", token);
            } else {
                qb.where().eq("token", token).and().ne("id", excludeId);
            }
        }).isEmpty();
        if (exists) throw new RuntimeException("Token 已存在");
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

    public List<ProxyClient> getProxyClients() {
        return proxyClientDao.selectList(qb -> {
        });
    }

    /**
     * 分页查询代理客户端，支持 name、status、enableFlag 条件
     */
    public PageResult<ProxyClient> getProxyClientsPageable(Page hutoolPage, String name, String status, Boolean enableFlag) {
        return proxyClientDao.selectPage(
                hutoolPage,
                where -> {
                    boolean hasWhere = false;
                    if (name != null && !name.isEmpty()) {
                        where.like("name", "%" + name + "%");
                        hasWhere = true;
                    }
                    if (status != null && !status.isEmpty()) {
                        if (hasWhere) where.and();
                        where.eq("status", status);
                        hasWhere = true;
                    }
                    if (enableFlag != null) {
                        if (hasWhere) where.and();
                        where.eq("enable_flag", enableFlag);
                    }
                });
    }

    /**
     * 根据ID查询代理客户端详情
     */
    public ProxyClient getProxyClientById(Integer id) {
        return proxyClientDao.selectById(id);
    }

    /**
     * 添加代理客户端
     */
    public ProxyClient addProxyClient(ProxyClient proxyClient) {
        validateForCreate(proxyClient);

        proxyClient.setCreateTime(new Date());
        if (proxyClient.getEnableFlag() == null) proxyClient.setEnableFlag(Boolean.TRUE);
        if (proxyClient.getStatus() == null) proxyClient.setStatus("offline");
        proxyClientDao.insert(proxyClient);
        return proxyClient;
    }

    /**
     * 更新代理客户端
     */
    public ProxyClient updateProxyClient(ProxyClient proxyClient) {
        if (proxyClient == null || proxyClient.getId() == null) {
            throw new RuntimeException("请求体缺少 id");
        }
        ProxyClient existingClient = proxyClientDao.selectById(proxyClient.getId());
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

        proxyClientDao.updateById(existingClient);
        return existingClient;
    }

    /**
     * 删除代理客户端
     */
    public boolean deleteProxyClient(Integer id) {
        ProxyClient existingClient = proxyClientDao.selectById(id);
        if (existingClient == null) {
            return false;
        }

        proxyClientDao.deleteById(id);
        return true;
    }
}
