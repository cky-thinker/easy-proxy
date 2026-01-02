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
        try {
            StringBuilder whereSql = new StringBuilder(" WHERE 1=1 ");
            java.util.List<Object> args = new java.util.ArrayList<>();
            if (proxyClientId != null) { whereSql.append(" AND proxy_client_id = ?"); args.add(proxyClientId); }
            if (startDate != null) { whereSql.append(" AND date >= ?"); args.add(new java.sql.Timestamp(startDate.getTime())); }
            if (endDate != null) { whereSql.append(" AND date <= ?"); args.add(new java.sql.Timestamp(endDate.getTime())); }

            String countSql = "SELECT COUNT(*) FROM (SELECT proxy_client_id, date FROM ts_day_report" + whereSql + " GROUP BY proxy_client_id, date) t";
            com.j256.ormlite.dao.GenericRawResults<String[]> countRes = dayReportDao.getDao().queryRaw(countSql, args.toArray(new String[0]));
            int total = 0; if (!countRes.getResults().isEmpty()) { total = Integer.parseInt(countRes.getResults().get(0)[0]); }
            int pageSize = page.getPageSize();
            int totalPage = total % pageSize == 0 ? total / pageSize : total / pageSize + 1;

            String dataSql = "SELECT proxy_client_id, date, SUM(upward_traffic_bytes) AS upload_bytes, SUM(downward_traffic_bytes) AS download_bytes FROM ts_day_report"
                    + whereSql + " GROUP BY proxy_client_id, date ORDER BY date DESC LIMIT ? OFFSET ?";
            args.add(pageSize);
            args.add(page.getStartPosition());

            com.j256.ormlite.dao.GenericRawResults<ClientTrafficDayReport> dataRes = dayReportDao.getDao().queryRaw(
                    dataSql,
                    new com.j256.ormlite.dao.RawRowMapper<ClientTrafficDayReport>() {
                        @Override
                        public ClientTrafficDayReport mapRow(String[] columnNames, String[] resultColumns) {
                            ClientTrafficDayReport r = new ClientTrafficDayReport();
                            r.setProxyClientId(Integer.valueOf(resultColumns[0]));
                            r.setDate(new java.sql.Timestamp(java.sql.Timestamp.valueOf(resultColumns[1]).getTime()));
                            r.setUploadBytes(Long.valueOf(resultColumns[2]));
                            r.setDownloadBytes(Long.valueOf(resultColumns[3]));
                            return r;
                        }
                    },
                    args.stream().map(Object::toString).toArray(String[]::new)
            );

            java.util.List<ClientTrafficDayReport> list = dataRes.getResults();
            return new PageResult<>(page.getPageNumber(), pageSize, totalPage, total, list);
        } catch (Exception e) {
            throw new RuntimeException("聚合客户端日流量失败", e);
        }
    }

    // 客户端规则总报告分页查询
    public PageResult<TsReport> getClientRuleReportsPageable(Page page, Integer proxyClientRuleId, Date startDate, Date endDate) {
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
    public PageResult<TsDayReport> getDayReportsPageable(Page page, Integer proxyClientRuleId, Date startDate, Date endDate) {
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
    public PageResult<TsHourReport> getHourReportsPageable(Page page, Integer proxyClientRuleId, Date startDate, Date endDate) {
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
    public List<TsDayReport> listDayReports(Integer proxyClientRuleId, Date startDate, Date endDate) {
        return dayReportDao.selectList(qb -> {
            qb.where();
            int clauses = 0;
            if (proxyClientRuleId != null) { clauses++; qb.where().eq("proxy_client_rule_id", proxyClientRuleId); }
            if (startDate != null) { clauses++; qb.where().ge("date", startDate); }
            if (endDate != null) { clauses++; qb.where().le("date", endDate); }
        });
    }
}