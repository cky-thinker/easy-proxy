package com.cky.proxy.server.dao;

import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cky.proxy.server.domain.dto.ClientTrafficDayReport;
import com.cky.proxy.server.domain.dto.PageResult;
import com.cky.proxy.server.domain.dto.TrafficRankingDTO;
import com.cky.proxy.server.domain.dto.TrafficTrendDTO;
import com.cky.proxy.server.domain.entity.TsDayReport;
import com.cky.proxy.server.mapper.TsDayReportMapper;
import cn.hutool.db.Page;

public class TsDayReportDao extends BaseDao<TsDayReport, TsDayReportMapper> {
    @Override
    protected Class<TsDayReportMapper> getMapperClass() {
        return TsDayReportMapper.class;
    }

    public PageResult<ClientTrafficDayReport> getClientReportsPageable(Page page, Integer proxyClientId, Date startDate, Date endDate) {
        return execute(mapper -> {
             IPage<ClientTrafficDayReport> mybatisPage = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page.getPageNumber(), page.getPageSize());
             IPage<ClientTrafficDayReport> resultPage = mapper.selectClientTrafficDayReports(mybatisPage, proxyClientId, startDate, endDate);
             return new PageResult<>(page.getPageNumber(), page.getPageSize(), (int)resultPage.getPages(), (int)resultPage.getTotal(), resultPage.getRecords());
        });
    }

    public List<TrafficRankingDTO> getTrafficRanking(Date startDate, Date endDate, int limit) {
        return execute(mapper -> mapper.selectTrafficRanking(startDate, endDate, limit));
    }

    public List<TrafficTrendDTO> getTrafficTrend(Date startDate, Date endDate) {
        return execute(mapper -> mapper.selectTrafficTrend(startDate, endDate));
    }
}
