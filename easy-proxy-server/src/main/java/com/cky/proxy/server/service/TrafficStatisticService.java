package com.cky.proxy.server.service;

import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cky.proxy.server.domain.dto.ClientTrafficDayReport;
import com.cky.proxy.server.domain.dto.PageResult;
import com.cky.proxy.server.domain.entity.TsDayReport;
import com.cky.proxy.server.domain.entity.TsHourReport;
import com.cky.proxy.server.domain.entity.TsReport;
import com.cky.proxy.server.mapper.TsDayReportMapper;
import com.cky.proxy.server.mapper.TsHourReportMapper;
import com.cky.proxy.server.mapper.TsReportMapper;
import com.cky.proxy.server.util.BeanContext;
import com.cky.proxy.server.util.PageUtil;

public class TrafficStatisticService {
    private final TsReportMapper clientRuleReportMapper = BeanContext.getTsReportMapper();
    private final TsDayReportMapper dayReportMapper = BeanContext.getTsDayReportMapper();
    private final TsHourReportMapper hourReportMapper = BeanContext.getTsHourReportMapper();

    // 客户端总报告分页查询
    public PageResult<ClientTrafficDayReport> getClientReportsPageable(cn.hutool.db.Page page, Integer proxyClientId, Date startDate, Date endDate) {
        IPage<ClientTrafficDayReport> mybatisPage = new Page<>(page.getPageNumber(), page.getPageSize());
        IPage<ClientTrafficDayReport> resultPage = dayReportMapper.selectClientTrafficDayReports(mybatisPage, proxyClientId, startDate, endDate);
        return PageUtil.toPageResult(page, resultPage);
    }

    // 客户端规则总报告分页查询
    public PageResult<TsReport> getClientRuleReportsPageable(cn.hutool.db.Page page, Integer proxyClientRuleId, Date startDate, Date endDate) {
        LambdaQueryWrapper<TsReport> wrapper = new LambdaQueryWrapper<>();
        if (proxyClientRuleId != null) {
            wrapper.eq(TsReport::getProxyClientRuleId, proxyClientRuleId);
        }
        if (startDate != null) {
            wrapper.ge(TsReport::getCreateTime, startDate);
        }
        if (endDate != null) {
            wrapper.le(TsReport::getCreateTime, endDate);
        }
        
        IPage<TsReport> mybatisPage = PageUtil.toMybatisPage(page);
        IPage<TsReport> result = clientRuleReportMapper.selectPage(mybatisPage, wrapper);
        return PageUtil.toPageResult(page, result);
    }

    // 天报告分页查询
    public PageResult<TsDayReport> getDayReportsPageable(cn.hutool.db.Page page, Integer proxyClientRuleId, Date startDate, Date endDate) {
        LambdaQueryWrapper<TsDayReport> wrapper = new LambdaQueryWrapper<>();
        if (proxyClientRuleId != null) {
            wrapper.eq(TsDayReport::getProxyClientRuleId, proxyClientRuleId);
        }
        if (startDate != null) {
            wrapper.ge(TsDayReport::getDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(TsDayReport::getDate, endDate);
        }
        
        IPage<TsDayReport> mybatisPage = PageUtil.toMybatisPage(page);
        IPage<TsDayReport> result = dayReportMapper.selectPage(mybatisPage, wrapper);
        return PageUtil.toPageResult(page, result);
    }

    // 小时报告分页查询
    public PageResult<TsHourReport> getHourReportsPageable(cn.hutool.db.Page page, Integer proxyClientRuleId, Date startDate, Date endDate) {
        LambdaQueryWrapper<TsHourReport> wrapper = new LambdaQueryWrapper<>();
        if (proxyClientRuleId != null) {
            wrapper.eq(TsHourReport::getProxyClientRuleId, proxyClientRuleId);
        }
        if (startDate != null) {
            wrapper.ge(TsHourReport::getDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(TsHourReport::getDate, endDate);
        }
        
        IPage<TsHourReport> mybatisPage = PageUtil.toMybatisPage(page);
        IPage<TsHourReport> result = hourReportMapper.selectPage(mybatisPage, wrapper);
        return PageUtil.toPageResult(page, result);
    }

    // 明细列表（不分页）示例
    public List<TsDayReport> listDayReports(Integer proxyClientRuleId, Date startDate, Date endDate) {
        LambdaQueryWrapper<TsDayReport> wrapper = new LambdaQueryWrapper<>();
        if (proxyClientRuleId != null) {
            wrapper.eq(TsDayReport::getProxyClientRuleId, proxyClientRuleId);
        }
        if (startDate != null) {
            wrapper.ge(TsDayReport::getDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(TsDayReport::getDate, endDate);
        }
        return dayReportMapper.selectList(wrapper);
    }
}
