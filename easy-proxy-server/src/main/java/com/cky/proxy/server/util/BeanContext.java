package com.cky.proxy.server.util;

import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cky.proxy.server.config.DatabaseConnectionManager;
import com.cky.proxy.server.domain.entity.ProxyClient;
import com.cky.proxy.server.mapper.ProxyClientMapper;
import com.cky.proxy.server.mapper.ProxyClientRuleMapper;
import com.cky.proxy.server.mapper.SysLogMapper;
import com.cky.proxy.server.mapper.SysUserMapper;
import com.cky.proxy.server.mapper.TsDayReportMapper;
import com.cky.proxy.server.mapper.TsHourReportMapper;
import com.cky.proxy.server.mapper.TsReportMapper;
import com.cky.proxy.server.service.ProxyClientRuleService;
import com.cky.proxy.server.service.ProxyClientService;
import com.cky.proxy.server.service.SysLogService;
import com.cky.proxy.server.service.TrafficStatisticService;
import com.cky.proxy.server.service.UserService;

import io.vertx.core.Vertx;
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

    // Mapper实例
    private SysUserMapper userMapper;
    private ProxyClientMapper proxyClientMapper;
    private ProxyClientRuleMapper proxyClientRuleMapper;
    private SysLogMapper sysLogMapper;
    private TsReportMapper trafficStatisticClientRuleReportMapper;
    private TsDayReportMapper trafficStatisticDayReportMapper;
    private TsHourReportMapper trafficStatisticHourReportMapper;

    // 服务实例
    private ProxyClientService proxyClientService;
    private ProxyClientRuleService proxyClientRuleService;
    private SysLogService sysLogService;
    private TrafficStatisticService trafficStatisticService;
    private UserService userService;

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

    public static SysUserMapper getUserMapper() {
        return instance.userMapper;
    }

    public static ProxyClientMapper getProxyClientMapper() {
        return instance.proxyClientMapper;
    }

    public static ProxyClientRuleMapper getProxyClientRuleMapper() {
        return instance.proxyClientRuleMapper;
    }

    public static SysLogMapper getSysLogMapper() {
        return instance.sysLogMapper;
    }

    public static TsReportMapper getTsReportMapper() {
        return instance.trafficStatisticClientRuleReportMapper;
    }

    public static TsDayReportMapper getTsDayReportMapper() {
        return instance.trafficStatisticDayReportMapper;
    }

    public static TsHourReportMapper getTsHourReportMapper() {
        return instance.trafficStatisticHourReportMapper;
    }

    public static ProxyClientService getProxyClientService() {
        return instance.proxyClientService;
    }

    public static ProxyClientRuleService getProxyClientRuleService() {
        return instance.proxyClientRuleService;
    }

    public static SysLogService getSysLogService() {
        return instance.sysLogService;
    }

    public static TrafficStatisticService getTrafficStatisticService() {
        return instance.trafficStatisticService;
    }

    public static UserService getUserService() {
        return instance.userService;
    }

    /**
     * 初始化所有数据库表
     */
    public void init() {
        try {
            // 初始化数据库
            initializeDb();
            // 初始化Mapper
            initializeMapper();
            // 初始化服务
            initializeService();
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
        sysLogService = new SysLogService();
        trafficStatisticService = new TrafficStatisticService();
    }

    public void initUserService(Vertx vertx) {
        if (userService == null) {
            userService = new UserService(vertx);
        }
    }

    private void initializeMapper() {
        // 初始化Mapper代理
        userMapper = MapperProxyFactory.getMapper(SysUserMapper.class);
        proxyClientMapper = MapperProxyFactory.getMapper(ProxyClientMapper.class);
        proxyClientRuleMapper = MapperProxyFactory.getMapper(ProxyClientRuleMapper.class);
        sysLogMapper = MapperProxyFactory.getMapper(SysLogMapper.class);
        trafficStatisticClientRuleReportMapper = MapperProxyFactory.getMapper(TsReportMapper.class);
        trafficStatisticDayReportMapper = MapperProxyFactory.getMapper(TsDayReportMapper.class);
        trafficStatisticHourReportMapper = MapperProxyFactory.getMapper(TsHourReportMapper.class);
    }

    /**
     * 初始化所有表
     */
    private void initializeDb() {
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
            getProxyClientMapper().updateAllOffline();
        } catch (Exception e) {
            log.error("初始化默认数据失败", e);
            throw e;
        }
    }
}
