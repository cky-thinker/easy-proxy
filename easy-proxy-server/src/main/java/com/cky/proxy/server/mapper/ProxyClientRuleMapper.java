package com.cky.proxy.server.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cky.proxy.server.domain.entity.ProxyClientRule;

public interface ProxyClientRuleMapper extends BaseMapper<ProxyClientRule> {
    default ProxyClientRule getByServerPort(Integer serverPort) {
        LambdaQueryWrapper<ProxyClientRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProxyClientRule::getServerPort, serverPort);
        return selectOne(wrapper);
    }
}
