package com.cky.proxy.server.service;

import com.cky.proxy.server.dao.ProxyClientDao;
import com.cky.proxy.server.domain.entity.ProxyClient;

import java.util.List;

public class ProxyClientService {
    private ProxyClientDao proxyClientDao = new ProxyClientDao();

    public List<ProxyClient> getProxyClients() {
        return proxyClientDao.selectList(qb -> {});
    }
}
