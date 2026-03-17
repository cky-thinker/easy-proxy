package com.cky.proxy.server.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cky.proxy.server.domain.entity.ProxyClientRule;

public interface ProxyClientRuleMapper extends BaseMapper<ProxyClientRule> {
    default ProxyClientRule getByServerPort(Integer serverPort) {
        QueryWrapper<ProxyClientRule> wrapper = new QueryWrapper<>();
        wrapper.eq("server_port", serverPort);
        return selectOne(wrapper);
    }
}
