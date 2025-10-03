package com.cky.proxy.server.service;

import java.util.Date;
import java.util.List;

import com.cky.proxy.server.dao.TrafficStatisticClientReportDao;
import com.cky.proxy.server.dao.TrafficStatisticClientRuleReportDao;
import com.cky.proxy.server.dao.TrafficStatisticDayReportDao;
import com.cky.proxy.server.dao.TrafficStatisticHourReportDao;
import com.cky.proxy.server.domain.dto.PageResult;
import com.cky.proxy.server.domain.entity.TrafficStatisticClientReport;
import com.cky.proxy.server.domain.entity.TrafficStatisticClientRuleReport;
import com.cky.proxy.server.domain.entity.TrafficStatisticDayReport;
import com.cky.proxy.server.domain.entity.TrafficStatisticHourReport;
import com.cky.proxy.server.util.BeanContext;

import cn.hutool.db.Page;

public class TrafficStatisticService {
    private final TrafficStatisticClientReportDao clientReportDao = BeanContext.getTrafficStatisticClientReportDao();
    private final TrafficStatisticClientRuleReportDao clientRuleReportDao = BeanContext.getTrafficStatisticClientRuleReportDao();
    private final TrafficStatisticDayReportDao dayReportDao = BeanContext.getTrafficStatisticDayReportDao();
    private final TrafficStatisticHourReportDao hourReportDao = BeanContext.getTrafficStatisticHourReportDao();

    // 客户端总报告分页查询
    public PageResult<TrafficStatisticClientReport> getClientReportsPageable(Page page, Integer proxyClientId, Date startDate, Date endDate) {
        return clientReportDao.selectPage(page, where -> {
            int clauses = 0;
            if (proxyClientId != null) {
                clauses++; where.eq("proxy_client_id", proxyClientId);
            }
            if (startDate != null) {
                clauses++; where.ge("date", startDate);
            }
            if (endDate != null) {
                clauses++; where.le("date", endDate);
            }
            if (clauses == 0) { where.raw("1=1"); }
        });
    }

    // 客户端规则总报告分页查询
    public PageResult<TrafficStatisticClientRuleReport> getClientRuleReportsPageable(Page page, Integer proxyClientRuleId, Date startDate, Date endDate) {
        return clientRuleReportDao.selectPage(page, where -> {
            int clauses = 0;
            if (proxyClientRuleId != null) {
                clauses++; where.eq("proxy_client_rule_id", proxyClientRuleId);
            }
            if (startDate != null) {
                clauses++; where.ge("date", startDate);
            }
            if (endDate != null) {
                clauses++; where.le("date", endDate);
            }
            if (clauses == 0) { where.raw("1=1"); }
        });
    }

    // 天报告分页查询
    public PageResult<TrafficStatisticDayReport> getDayReportsPageable(Page page, Integer proxyClientRuleId, Date startDate, Date endDate) {
        return dayReportDao.selectPage(page, where -> {
            int clauses = 0;
            if (proxyClientRuleId != null) {
                clauses++; where.eq("proxy_client_rule_id", proxyClientRuleId);
            }
            if (startDate != null) {
                clauses++; where.ge("date", startDate);
            }
            if (endDate != null) {
                clauses++; where.le("date", endDate);
            }
            if (clauses == 0) { where.raw("1=1"); }
        });
    }

    // 小时报告分页查询
    public PageResult<TrafficStatisticHourReport> getHourReportsPageable(Page page, Integer proxyClientRuleId, Date startDate, Date endDate) {
        return hourReportDao.selectPage(page, where -> {
            int clauses = 0;
            if (proxyClientRuleId != null) {
                clauses++; where.eq("proxy_client_rule_id", proxyClientRuleId);
            }
            if (startDate != null) {
                clauses++; where.ge("date", startDate);
            }
            if (endDate != null) {
                clauses++; where.le("date", endDate);
            }
            if (clauses == 0) { where.raw("1=1"); }
        });
    }

    // 明细列表（不分页）示例
    public List<TrafficStatisticDayReport> listDayReports(Integer proxyClientRuleId, Date startDate, Date endDate) {
        return dayReportDao.selectList(qb -> {
            qb.where();
            int clauses = 0;
            if (proxyClientRuleId != null) { clauses++; qb.where().eq("proxy_client_rule_id", proxyClientRuleId); }
            if (startDate != null) { clauses++; qb.where().ge("date", startDate); }
            if (endDate != null) { clauses++; qb.where().le("date", endDate); }
        });
    }
}