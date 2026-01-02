package com.cky.proxy.server;

import org.junit.jupiter.api.Test;

import com.cky.proxy.server.util.BeanContext;
import com.cky.proxy.server.dao.ProxyClientDao;
import com.cky.proxy.server.dao.ProxyClientRuleDao;
import com.cky.proxy.server.dao.TsDayReportDao;
import com.cky.proxy.server.dao.TsHourReportDao;
import com.cky.proxy.server.dao.SysLogDao;
import com.cky.proxy.server.dao.TsReportDao;
import com.cky.proxy.server.domain.entity.ProxyClient;
import com.cky.proxy.server.domain.entity.ProxyClientRule;
import com.cky.proxy.server.domain.entity.TsDayReport;
import com.cky.proxy.server.domain.entity.TsHourReport;
import com.cky.proxy.server.domain.entity.TsReport;
import com.cky.proxy.server.domain.entity.SysLog;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class DbInitTest {
    @Test
    public void dataInit() {
        BeanContext beanContext = BeanContext.getInstance();
        com.cky.proxy.server.config.DatabaseProperty db = new com.cky.proxy.server.config.DatabaseProperty();
        db.setUrl("jdbc:h2:mem:dashboard-test;DB_CLOSE_DELAY=-1");
        db.setUsername("sa");
        db.setPassword("");
        com.cky.proxy.server.config.ConfigProperty.getInstance().setDb(db);
        beanContext.initializeDatabase();
        ProxyClientDao clientDao = BeanContext.getProxyClientDao();
        ProxyClientRuleDao ruleDao = BeanContext.getProxyClientRuleDao();
        TsReportDao reportDao = BeanContext.getTrafficStatisticClientRuleReportDao();
        TsDayReportDao dayDao = BeanContext.getTrafficStatisticDayReportDao();
        TsHourReportDao hourDao = BeanContext.getTrafficStatisticHourReportDao();
        SysLogDao sysLogDao = BeanContext.getSysLogDao();
        
        // 清空表数据
        try {
            com.j256.ormlite.table.TableUtils.clearTable(clientDao.getDao().getConnectionSource(), ProxyClient.class);
            com.j256.ormlite.table.TableUtils.clearTable(ruleDao.getDao().getConnectionSource(), ProxyClientRule.class);
            com.j256.ormlite.table.TableUtils.clearTable(reportDao.getDao().getConnectionSource(), TsReport.class);
            com.j256.ormlite.table.TableUtils.clearTable(dayDao.getDao().getConnectionSource(), TsDayReport.class);
            com.j256.ormlite.table.TableUtils.clearTable(hourDao.getDao().getConnectionSource(), TsHourReport.class);
            com.j256.ormlite.table.TableUtils.clearTable(sysLogDao.getDao().getConnectionSource(), SysLog.class);
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("清空表失败", e);
        }

        List<ProxyClient> clients = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            ProxyClient c = new ProxyClient();
            c.setName(String.format("客户端-%03d", i));
            c.setToken(UUID.randomUUID().toString().replace("-", ""));
            c.setStatus(i % 2 == 0 ? "offline" : "online");
            c.setEnableFlag(i != 5);
            c.setCreateBy("test");
            c.setCreateTime(new Date());
            clientDao.insert(c);
            clients.add(c);
        }

        List<ProxyClientRule> rules = new ArrayList<>();
        int portBase = 8000;
        int ipBase = 100;
        for (ProxyClient c : clients) {
            int ruleCount = 3;
            for (int r = 1; r <= ruleCount; r++) {
                ProxyClientRule rule = new ProxyClientRule();
                rule.setProxyClientId(c.getId());
                rule.setName("规则-" + c.getName() + "-" + r);
                rule.setServerPort(portBase++);
                rule.setClientAddress("192.168.1." + (ipBase++) + ":" + (10000 + r));
                rule.setEnableFlag(!(c.getId() % 2 == 0 && r == 3));
                rule.setCreateBy("test");
                rule.setCreateTime(new Date());
                ruleDao.insert(rule);
                rules.add(rule);
            }
        }

        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        for (ProxyClientRule rule : rules) {
            // 生成近30日的天级数据（涵盖近7日和近30日）
            for (int d = 1; d <= 30; d++) {
                cal.setTime(new Date());
                cal.add(Calendar.DAY_OF_MONTH, -d);
                // 设置为当天的23:59:59，模拟每天结束生成
                cal.set(Calendar.HOUR_OF_DAY, 23);
                cal.set(Calendar.MINUTE, 59);
                cal.set(Calendar.SECOND, 59);
                Date day = cal.getTime();
                
                long up = rule.getEnableFlag() ? ThreadLocalRandom.current().nextLong(5_000_000L, 50_000_000L) : 0L;
                long down = rule.getEnableFlag() ? ThreadLocalRandom.current().nextLong(8_000_000L, 80_000_000L) : 0L;
                TsDayReport dr = new TsDayReport();
                dr.setProxyClientId(rule.getProxyClientId());
                dr.setProxyClientRuleId(rule.getId());
                dr.setDate(day);
                dr.setUploadBytes(up);
                dr.setDownloadBytes(down);
                dr.setCreateTime(new Date());
                dayDao.insert(dr);
            }
        }

        for (ProxyClientRule rule : rules) {
            if (!rule.getEnableFlag()) continue;
            // 生成近24小时的小时级数据
            for (int h = 1; h <= 24; h++) {
                cal.setTime(new Date());
                cal.add(Calendar.HOUR_OF_DAY, -h);
                // 设置为该小时的59分59秒，模拟小时结束生成
                cal.set(Calendar.MINUTE, 59);
                cal.set(Calendar.SECOND, 59);
                Date hour = cal.getTime();
                
                long up = ThreadLocalRandom.current().nextLong(200_000L, 2_000_000L);
                long down = ThreadLocalRandom.current().nextLong(300_000L, 3_000_000L);
                TsHourReport hr = new TsHourReport();
                hr.setProxyClientId(rule.getProxyClientId());
                hr.setProxyClientRuleId(rule.getId());
                hr.setDate(hour);
                hr.setUploadBytes(up);
                hr.setDownloadBytes(down);
                hr.setCreateTime(new Date());
                hourDao.insert(hr);
            }
        }

        String[] msgs = new String[] {
                "系统启动完成",
                "新增代理规则：端口8080",
                "客户端-002 下线",
                "客户端-003 流量异常",
                "客户端-001 上线"
        };
        String[] types = new String[] { "info", "success", "warning", "error" };
        for (int i = 0; i < msgs.length; i++) {
            SysLog log = new SysLog();
            log.setLogType(types[i % types.length]);
            log.setLogContent(msgs[i]);
            log.setCreateTime(new Date());
            sysLogDao.insert(log);
        }
    }
}
