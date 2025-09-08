package com.cky.proxy.server.service;

import com.cky.proxy.server.bean.entity.ProxyClient;
import com.cky.proxy.server.dao.ProxyClientDao;

import java.util.List;

public class ProxyClientService {
    private ProxyClientDao proxyClientDao = new ProxyClientDao();

    public List<ProxyClient> getProxyClients() {
        return proxyClientDao.selectList(qb -> {});
    }
}
