package com.cky.proxy.server.dao;

import com.cky.proxy.server.domain.entity.SysLog;
import com.cky.proxy.server.mapper.SysLogMapper;

/**
 * 系统日志 DAO
 */
public class SysLogDao extends BaseDao<SysLog, SysLogMapper> {
    @Override
    protected Class<SysLogMapper> getMapperClass() {
        return SysLogMapper.class;
    }
}
