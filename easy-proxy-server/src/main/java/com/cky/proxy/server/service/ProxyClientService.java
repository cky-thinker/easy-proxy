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
     * 分页查询代理客户端
     */
    public PageResult<ProxyClient> getProxyClientsPageable(Page hutoolPage, String name) {
        return proxyClientDao.selectPage(
                hutoolPage,
                where -> {
                    if (name != null && !name.isEmpty()) {
                        where.like("name", "%" + name + "%");
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
        ProxyClient existingClient = proxyClientDao.selectById(proxyClient.getId());
        if (existingClient == null) {
            return null;
        }

        if (proxyClient.getName() != null) {
            existingClient.setName(proxyClient.getName());
        }
        if (proxyClient.getToken() != null) {
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
