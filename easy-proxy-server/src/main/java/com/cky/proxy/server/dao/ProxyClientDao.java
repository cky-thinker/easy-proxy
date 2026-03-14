package com.cky.proxy.server.dao;

import com.cky.proxy.server.domain.entity.ProxyClient;
import com.cky.proxy.server.mapper.ProxyClientMapper;

import java.util.List;

public class ProxyClientDao extends BaseDao<ProxyClient, ProxyClientMapper> {
    @Override
    protected Class<ProxyClientMapper> getMapperClass() {
        return ProxyClientMapper.class;
    }

    public ProxyClient selectByToken(String token) {
        List<ProxyClient> list = selectList(wrapper -> wrapper.eq("token", token));
        return list.isEmpty() ? null : list.get(0);
    }

}
