package com.cky.proxy.server.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cky.proxy.server.domain.entity.ProxyClient;

public interface ProxyClientMapper extends BaseMapper<ProxyClient> {
    default ProxyClient getByToken(String token) {
        QueryWrapper<ProxyClient> wrapper = new QueryWrapper<>();
        wrapper.eq("token", token);
        return selectOne(wrapper);
    }
}
