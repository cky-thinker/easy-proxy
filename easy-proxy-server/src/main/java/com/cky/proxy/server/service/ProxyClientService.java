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
        proxyClient.setCreateTime(new Date());
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

        // name 唯一性
        if (proxyClient.getName() != null && !proxyClient.getName().equals(existingClient.getName())) {
            boolean exists = !proxyClientDao.selectList(qb -> {
                qb.where().eq("name", proxyClient.getName()).and().ne("id", proxyClient.getId());
            }).isEmpty();
            if (exists) throw new RuntimeException("客户端名称已存在");
            existingClient.setName(proxyClient.getName());
        }
        // token 格式 + 唯一性
        if (proxyClient.getToken() != null && !proxyClient.getToken().equals(existingClient.getToken())) {
            String token = proxyClient.getToken();
            if (!token.matches("^[0-9a-fA-F]{64}$")) {
                throw new RuntimeException("Token 必须为64位十六进制字符串");
            }
            boolean exists = !proxyClientDao.selectList(qb -> {
                qb.where().eq("token", token).and().ne("id", proxyClient.getId());
            }).isEmpty();
            if (exists) throw new RuntimeException("Token 已存在");
            existingClient.setToken(token);
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
