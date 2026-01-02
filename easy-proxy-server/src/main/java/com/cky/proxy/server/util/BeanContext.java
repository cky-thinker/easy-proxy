package com.cky.proxy.server.util;

import java.sql.SQLException;
import java.util.Date;
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
import com.cky.proxy.server.domain.entity.ProxyClientRule;
import com.cky.proxy.server.domain.entity.SysUser;
import com.cky.proxy.server.domain.entity.SysLog;
import com.cky.proxy.server.domain.entity.TsReport;
import com.cky.proxy.server.domain.entity.TsDayReport;
import com.cky.proxy.server.domain.entity.TsHourReport;
import com.j256.ormlite.jdbc.db.H2DatabaseType;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTableConfig;
import com.j256.ormlite.table.TableUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    /**
     * 初始化所有数据库表
     */
    public void initializeDatabase() {
        try {
            // 初始化Dao
            initializeDao();
            // 初始化所有表
            initializeAllTables();
            // 初始化默认数据
            initializeDefaultData();
            log.info("数据库初始化完成");
        } catch (SQLException e) {
            log.error("数据库初始化失败", e);
            throw new RuntimeException("数据库初始化失败: " + e.getMessage(), e);
        }
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
        try (ConnectionSource connectionSource = DatabaseConnectionManager.getInstance().createConnectionSource()) {
            // 初始化用户表
            initializeTable(connectionSource, SysUser.class);
            // 初始化代理客户端表
            initializeTable(connectionSource, ProxyClient.class);
            // 初始化代理客户端规则表
            initializeTable(connectionSource, ProxyClientRule.class);
            // 初始化系统日志表
            initializeTable(connectionSource, SysLog.class);
            // 初始化流量统计相关表
            initializeTable(connectionSource, TsReport.class);
            initializeTable(connectionSource, TsDayReport.class);
            initializeTable(connectionSource, TsHourReport.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 初始化单个表
     */
    private void initializeTable(ConnectionSource connectionSource, Class<?> entityClass)
            throws SQLException {
        String tableName = DatabaseTableConfig.extractTableName(new H2DatabaseType(), entityClass);
        try {
            int tableExists = TableUtils.createTableIfNotExists(connectionSource, entityClass);
            if (tableExists == 1) {
                log.info(tableName + " 已存在，跳过创建");
            } else {
                log.info(tableName + " 创建成功");
            }
            log.info("表 {} 初始化完成", tableName);
        } catch (SQLException e) {
            String errorMsg = "创建表 " + tableName + " 失败: " + e.getMessage();
            log.info(errorMsg);
            log.error(errorMsg, e);
            throw new SQLException(errorMsg, e);
        }
    }

    /**
     * 初始化默认数据
     */
    private void initializeDefaultData() throws SQLException {
        try {
            // 检查是否需要创建默认管理员用户
            initializeDefaultAdminUser();

        } catch (SQLException e) {
            log.error("初始化默认数据失败", e);
            log.info("初始化默认数据失败: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 初始化默认管理员用户
     */
    private void initializeDefaultAdminUser() throws SQLException {
        try {
            // 检查是否已存在管理员用户
            List<SysUser> existingSysUsers = userDao.selectList(qb -> {
                qb.where().eq("username", "admin");
            });

            if (existingSysUsers.isEmpty()) {
                // 创建默认管理员用户
                SysUser adminSysUser = new SysUser();
                adminSysUser.setUsername("admin");
                adminSysUser.setPassword("admin123"); // 注意：实际项目中应该使用加密密码
                adminSysUser.setMobile("13800000000");
                adminSysUser.setRole("admin");
                adminSysUser.setEnableFlag(true);
                adminSysUser.setCreateTime(new Date());

                userDao.insert(adminSysUser);
                log.info("默认管理员用户创建成功 (用户名: admin, 密码: admin123)");
                log.info("默认管理员用户创建成功");
            } else {
                log.info("管理员用户已存在，跳过创建");
                log.info("管理员用户已存在，跳过创建");
            }
        } catch (Exception e) {
            log.error("初始化默认管理员用户失败", e);
            throw new SQLException("初始化默认管理员用户失败: " + e.getMessage(), e);
        }
    }
}
