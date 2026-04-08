package com.cky.proxy.server.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cky.proxy.server.domain.entity.ProxyClient;

public interface ProxyClientMapper extends BaseMapper<ProxyClient> {
    default ProxyClient getByToken(String token) {
        LambdaQueryWrapper<ProxyClient> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProxyClient::getToken, token);
        return selectOne(wrapper);
    }

    default int updateAllOffline() {
        LambdaUpdateWrapper<ProxyClient> wrapper = new LambdaUpdateWrapper<>();
        wrapper.set(ProxyClient::getStatus, "offline");
        return update(wrapper);
    }
}
