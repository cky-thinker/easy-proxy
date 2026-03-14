package com.cky.proxy.server;

import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.Test;

import com.cky.proxy.server.util.BeanContext;
import com.cky.proxy.server.config.DatabaseConnectionManager;

import lombok.extern.slf4j.Slf4j;

import com.cky.proxy.common.consts.OnlineStatus;
import com.cky.proxy.server.mapper.ProxyClientMapper;
import com.cky.proxy.server.mapper.ProxyClientRuleMapper;
import com.cky.proxy.server.mapper.TsDayReportMapper;
import com.cky.proxy.server.mapper.TsHourReportMapper;
import com.cky.proxy.server.mapper.SysLogMapper;
import com.cky.proxy.server.mapper.TsReportMapper;
import com.cky.proxy.server.domain.entity.ProxyClient;
import com.cky.proxy.server.domain.entity.ProxyClientRule;
import com.cky.proxy.server.domain.entity.TsDayReport;
import com.cky.proxy.server.domain.entity.TsHourReport;
import com.cky.proxy.server.domain.entity.TsReport;
import com.cky.proxy.server.domain.entity.SysLog;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public class DbInitTest {
    // mvn -f easy-proxy-server/pom.xml -Dtest=DbInitTest test
    @Test
    public void dataInit() {
        BeanContext beanContext = BeanContext.getInstance();
        com.cky.proxy.server.config.DatabaseProperty db = new com.cky.proxy.server.config.DatabaseProperty();
        db.setUrl("jdbc:h2:./data/database");
        db.setUsername("test");
        db.setPassword("test");
        com.cky.proxy.server.config.ConfigProperty.getInstance().setDb(db);
        // 初始化数据库连接
        beanContext.init();
        ProxyClientMapper clientMapper = BeanContext.getProxyClientMapper();
        ProxyClientRuleMapper ruleMapper = BeanContext.getProxyClientRuleMapper();
        TsReportMapper reportMapper = BeanContext.getTsReportMapper();
        TsDayReportMapper dayMapper = BeanContext.getTsDayReportMapper();
        TsHourReportMapper hourMapper = BeanContext.getTsHourReportMapper();
        SysLogMapper sysLogMapper = BeanContext.getSysLogMapper();
        
        // 首先清空表数据
        log.info("首先清空表数据");
        try {
            clientMapper.delete(null);
            ruleMapper.delete(null);
            reportMapper.delete(null);
            dayMapper.delete(null);
            hourMapper.delete(null);
            sysLogMapper.delete(null);
        } catch (Exception e) {
            log.error("清空表数据失败", e);
        }

        try (SqlSession sqlSession = DatabaseConnectionManager.getInstance().getSqlSessionFactory().openSession(ExecutorType.BATCH, false)) {
            ProxyClientMapper batchClientMapper = sqlSession.getMapper(ProxyClientMapper.class);
            ProxyClientRuleMapper batchRuleMapper = sqlSession.getMapper(ProxyClientRuleMapper.class);
            TsDayReportMapper batchDayMapper = sqlSession.getMapper(TsDayReportMapper.class);
            TsHourReportMapper batchHourMapper = sqlSession.getMapper(TsHourReportMapper.class);
            TsReportMapper batchReportMapper = sqlSession.getMapper(TsReportMapper.class);
            SysLogMapper batchSysLogMapper = sqlSession.getMapper(SysLogMapper.class);

            log.info("生成客户端");
            List<ProxyClient> clients = new ArrayList<>();
            for (int i = 1; i <= 15; i++) {
                ProxyClient c = new ProxyClient();
                c.setName(String.format("客户端-%03d", i));
                c.setToken(UUID.randomUUID().toString().replace("-", ""));
                c.setStatus(i % 2 == 0 ? OnlineStatus.offline.name() : OnlineStatus.online.name());
                c.setEnableFlag(i != 5);
                c.setCreateBy("test");
                c.setCreateTime(new Date());
                batchClientMapper.insert(c);
                clients.add(c);
            }
            sqlSession.flushStatements();

            log.info("生成客户端规则");
            List<ProxyClientRule> rules = new ArrayList<>();
            int portBase = 8000;
            int ipBase = 100;
            for (ProxyClient c : clients) {
                int ruleCount = 12;
                for (int r = 1; r <= ruleCount; r++) {
                    ProxyClientRule rule = new ProxyClientRule();
                    rule.setProxyClientId(c.getId());
                    rule.setName("规则-" + c.getName() + "-" + r);
                    rule.setServerPort(portBase++);
                    rule.setClientAddress("192.168.1." + (ipBase++) + ":" + (10000 + r));
                    rule.setEnableFlag(!(c.getId() % 2 == 0 && r == 3));
                    rule.setCreateBy("test");
                    rule.setCreateTime(new Date());
                    batchRuleMapper.insert(rule);
                    rules.add(rule);
                }
            }
            sqlSession.flushStatements();

            log.info("生成客户端规则的天级数据");
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
                    cal.set(Calendar.MILLISECOND, 0);
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
                    batchDayMapper.insert(dr);
                }
            }
            sqlSession.flushStatements();

            log.info("生成客户端规则的小时级数据");
            
            for (ProxyClientRule rule : rules) {
                if (!rule.getEnableFlag()) continue;
                // 生成近24小时的小时级数据
                for (int h = 1; h <= 24; h++) {
                    cal.setTime(new Date());
                    cal.add(Calendar.HOUR_OF_DAY, -h);
                    // 设置为该小时的59分59秒，模拟小时结束生成
                    cal.set(Calendar.MINUTE, 59);
                    cal.set(Calendar.SECOND, 59);
                    cal.set(Calendar.MILLISECOND, 0);
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
                    batchHourMapper.insert(hr);
                }
            }
            sqlSession.flushStatements();

            log.info("生成客户端规则的总统计数据");
            for (ProxyClientRule rule : rules) {
                // 生成总统计数据
                long up = rule.getEnableFlag() ? ThreadLocalRandom.current().nextLong(50_000_000L, 500_000_000L) : 0L;
                long down = rule.getEnableFlag() ? ThreadLocalRandom.current().nextLong(80_000_000L, 800_000_000L) : 0L;
                TsReport report = new TsReport();
                report.setProxyClientId(rule.getProxyClientId());
                report.setProxyClientRuleId(rule.getId());
                report.setUploadBytes(up);
                report.setDownloadBytes(down);
                report.setCreateTime(new Date());
                report.setUpdateTime(new Date());
                batchReportMapper.insert(report);
            }
            sqlSession.flushStatements();

            log.info("生成系统日志");
            
            String[] msgs = new String[] {
                    "系统启动完成",
                    "新增代理规则：端口8080",
                    "客户端-002 下线",
                    "客户端-003 流量异常",
                    "客户端-001 上线"
            };
            String[] types = new String[] { "info", "success", "warning", "error" };
            for (int i = 0; i < msgs.length; i++) {
                SysLog logEntity = new SysLog();
                logEntity.setLogType(types[i % types.length]);
                logEntity.setLogContent(msgs[i]);
                logEntity.setCreateTime(new Date());
                batchSysLogMapper.insert(logEntity);
            }
            
            sqlSession.commit();
            log.info("数据批量插入完成");
        } catch (Exception e) {
            log.error("批量插入数据失败", e);
        }
    }
}
