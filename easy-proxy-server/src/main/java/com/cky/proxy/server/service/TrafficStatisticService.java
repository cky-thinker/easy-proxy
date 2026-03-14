package com.cky.proxy.server.service;

import java.util.Date;
import java.util.List;

import com.cky.proxy.server.domain.dto.ClientTrafficDayReport;
import com.cky.proxy.server.dao.TsReportDao;
import com.cky.proxy.server.dao.TsDayReportDao;
import com.cky.proxy.server.dao.TsHourReportDao;
import com.cky.proxy.server.domain.dto.PageResult;
import com.cky.proxy.server.domain.entity.TsReport;
import com.cky.proxy.server.domain.entity.TsDayReport;
import com.cky.proxy.server.domain.entity.TsHourReport;
import com.cky.proxy.server.util.BeanContext;

import cn.hutool.db.Page;

public class TrafficStatisticService {
    private final TsReportDao clientRuleReportDao = BeanContext.getTsReportDao();
    private final TsDayReportDao dayReportDao = BeanContext.getTsDayReportDao();
    private final TsHourReportDao hourReportDao = BeanContext.getTsHourReportDao();

    // 客户端总报告分页查询
    public PageResult<ClientTrafficDayReport> getClientReportsPageable(Page page, Integer proxyClientId, Date startDate, Date endDate) {
        return dayReportDao.getClientReportsPageable(page, proxyClientId, startDate, endDate);
    }

    // 客户端规则总报告分页查询
    public PageResult<TsReport> getClientRuleReportsPageable(Page page, Integer proxyClientRuleId, Date startDate, Date endDate) {
        return clientRuleReportDao.selectPage(page, wrapper -> {
            if (proxyClientRuleId != null) {
                wrapper.eq("proxy_client_rule_id", proxyClientRuleId);
            }
            if (startDate != null) {
                wrapper.ge("create_time", startDate);
            }
            if (endDate != null) {
                wrapper.le("create_time", endDate);
            }
        });
    }

    // 天报告分页查询
    public PageResult<TsDayReport> getDayReportsPageable(Page page, Integer proxyClientRuleId, Date startDate, Date endDate) {
        return dayReportDao.selectPage(page, wrapper -> {
            if (proxyClientRuleId != null) {
                wrapper.eq("proxy_client_rule_id", proxyClientRuleId);
            }
            if (startDate != null) {
                wrapper.ge("date", startDate);
            }
            if (endDate != null) {
                wrapper.le("date", endDate);
            }
        });
    }

    // 小时报告分页查询
    public PageResult<TsHourReport> getHourReportsPageable(Page page, Integer proxyClientRuleId, Date startDate, Date endDate) {
        return hourReportDao.selectPage(page, wrapper -> {
            if (proxyClientRuleId != null) {
                wrapper.eq("proxy_client_rule_id", proxyClientRuleId);
            }
            if (startDate != null) {
                wrapper.ge("date", startDate);
            }
            if (endDate != null) {
                wrapper.le("date", endDate);
            }
        });
    }

    // 明细列表（不分页）示例
    public List<TsDayReport> listDayReports(Integer proxyClientRuleId, Date startDate, Date endDate) {
        return dayReportDao.selectList(wrapper -> {
            if (proxyClientRuleId != null) {
                wrapper.eq("proxy_client_rule_id", proxyClientRuleId);
            }
            if (startDate != null) {
                wrapper.ge("date", startDate);
            }
            if (endDate != null) {
                wrapper.le("date", endDate);
            }
        });
    }
}
