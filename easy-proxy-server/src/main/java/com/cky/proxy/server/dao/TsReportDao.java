package com.cky.proxy.server.dao;

import com.cky.proxy.server.domain.entity.TsReport;
import com.cky.proxy.server.mapper.TsReportMapper;

public class TsReportDao extends BaseDao<TsReport, TsReportMapper> {
    @Override
    protected Class<TsReportMapper> getMapperClass() {
        return TsReportMapper.class;
    }
}
