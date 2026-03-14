package com.cky.proxy.server.dao;

import com.cky.proxy.server.domain.dto.TrafficRankingDTO;
import com.cky.proxy.server.domain.dto.TrafficTrendDTO;
import com.cky.proxy.server.domain.entity.TsHourReport;
import com.cky.proxy.server.mapper.TsHourReportMapper;

import java.util.Date;
import java.util.List;

public class TsHourReportDao extends BaseDao<TsHourReport, TsHourReportMapper> {
    @Override
    protected Class<TsHourReportMapper> getMapperClass() {
        return TsHourReportMapper.class;
    }

    public List<TrafficRankingDTO> getTrafficRanking(Date startDate, Date endDate, int limit) {
        return execute(mapper -> mapper.selectTrafficRanking(startDate, endDate, limit));
    }

    public List<TrafficTrendDTO> getTrafficTrend(Date startDate, Date endDate) {
        return execute(mapper -> mapper.selectTrafficTrend(startDate, endDate));
    }
}
