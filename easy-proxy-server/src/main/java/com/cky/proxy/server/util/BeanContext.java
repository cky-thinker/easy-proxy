package com.cky.proxy.server.util;

import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.List;

import com.cky.proxy.server.config.DatabaseConnectionManager;
import com.cky.proxy.server.dao.ProxyClientDao;
import com.cky.proxy.server.dao.ProxyClientRuleDao;
import com.cky.proxy.server.dao.SysLogDao;
import com.cky.proxy.server.dao.TsReportDao;
import com.cky.proxy.server.dao.TsDayReportDao;
import com.cky.proxy.server.dao.TsHourReportDao;
import com.cky.proxy.server.dao.UserDao;
import com.cky.proxy.server.domain.entity.ProxyClient;
import com.cky.proxy.server.service.ProxyClientRuleService;
import com.cky.proxy.server.service.ProxyClientService;

import lombok.Data;

import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Data
public class BeanContext {
    private static final Logger log = LoggerFactory.getLogger(BeanContext.class);

    private static volatile BeanContext instance;
    private static final Object lock = new Object();

    // DAO实例
    private UserDao userDao;
    private ProxyClientDao proxyClientDao;
    private ProxyClientRuleDao proxyClientRuleDao;
    private SysLogDao sysLogDao;
    private TsReportDao trafficStatisticClientRuleReportDao;
    private TsDayReportDao trafficStatisticDayReportDao;
    private TsHourReportDao trafficStatisticHourReportDao;

    // 服务实例
    private ProxyClientService proxyClientService;
    private ProxyClientRuleService proxyClientRuleService;

    private BeanContext() {
    }

    /**
     * 获取数据库初始化服务实例
     */
    public static BeanContext getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new BeanContext();
                }
            }
        }
        return instance;
    }

    public static UserDao getUserDao() {
        return instance.userDao;
    }

    public static ProxyClientDao getProxyClientDao() {
        return instance.proxyClientDao;
    }

    public static ProxyClientRuleDao getProxyClientRuleDao() {
        return instance.proxyClientRuleDao;
    }

    public static SysLogDao getSysLogDao() {
        return instance.sysLogDao;
    }

    public static TsReportDao getTsReportDao() {
        return instance.trafficStatisticClientRuleReportDao;
    }

    public static TsDayReportDao getTsDayReportDao() {
        return instance.trafficStatisticDayReportDao;
    }

    public static TsHourReportDao getTsHourReportDao() {
        return instance.trafficStatisticHourReportDao;
    }

    public static ProxyClientService getProxyClientService() {
        return instance.proxyClientService;
    }

    public static ProxyClientRuleService getProxyClientRuleService() {
        return instance.proxyClientRuleService;
    }

    /**
     * 初始化所有数据库表
     */
    public void init() {
        try {
            // 初始化Dao
            initializeDao();
            // 初始化服务
            initializeService();
            // 初始化所有表
            initializeAllTables();
            // 初始化默认数据
            initializeData();
            log.info("数据库初始化完成");
        } catch (Exception e) {
            log.error("数据库初始化失败", e);
            throw new RuntimeException("数据库初始化失败: " + e.getMessage(), e);
        }
    }

    private void initializeService() {
        proxyClientService = new ProxyClientService();
        proxyClientRuleService = new ProxyClientRuleService();
    }

    private void initializeDao() {
        // 初始化Dao
        userDao = new UserDao();
        proxyClientDao = new ProxyClientDao();
        proxyClientRuleDao = new ProxyClientRuleDao();
        sysLogDao = new SysLogDao();
        trafficStatisticClientRuleReportDao = new TsReportDao();
        trafficStatisticDayReportDao = new TsDayReportDao();
        trafficStatisticHourReportDao = new TsHourReportDao();
    }

    /**
     * 初始化所有表
     */
    private void initializeAllTables() {
        try (SqlSession session = DatabaseConnectionManager.getInstance().getSqlSessionFactory().openSession()) {
            ScriptRunner runner = new ScriptRunner(session.getConnection());
            runner.setAutoCommit(true);
            runner.setStopOnError(true);
            runner.setLogWriter(null); // 禁止输出详细日志，避免刷屏
            runner.runScript(new InputStreamReader(BeanContext.class.getClassLoader().getResourceAsStream("schema.sql")));
            log.info("Schema initialized.");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database tables", e);
        }
    }

    /**
     * 初始化数据
     */
    private void initializeData() throws SQLException {
        try {
            // 初始化客户端状态
            initializeClientOffline();
        } catch (Exception e) {
            log.error("初始化默认数据失败", e);
            log.info("初始化默认数据失败: " + e.getMessage());
            throw e;
        }
    }

    private void initializeClientOffline() {
        List<ProxyClient> proxyClients = getProxyClientDao().selectList(wrapper -> {
        });
        proxyClients.forEach(client -> {
            client.setStatus("offline");
            getProxyClientDao().updateById(client);
        });
    }
}
