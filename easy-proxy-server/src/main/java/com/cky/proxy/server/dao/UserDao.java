package com.cky.proxy.server.dao;

import com.cky.proxy.server.domain.entity.SysUser;
import com.cky.proxy.server.mapper.SysUserMapper;

public class UserDao extends BaseDao<SysUser, SysUserMapper> {
    @Override
    protected Class<SysUserMapper> getMapperClass() {
        return SysUserMapper.class;
    }
}
