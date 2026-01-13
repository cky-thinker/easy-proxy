package com.cky.proxy.server.dao;

import com.cky.proxy.server.domain.entity.ProxyClient;

import java.sql.SQLException;
import java.util.List;

public class ProxyClientDao extends BaseDao<ProxyClient> {

    public ProxyClient selectByToken(String token) {
        List<ProxyClient> list = selectList(qb -> {
            try {
                qb.where().eq("token", token);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        return list.isEmpty() ? null : list.get(0);
    }

}
