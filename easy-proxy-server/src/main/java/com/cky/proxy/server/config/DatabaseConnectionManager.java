package com.cky.proxy.server.config;

import java.sql.SQLException;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.annotation.DbType;
import com.cky.proxy.server.mapper.*;
import org.apache.ibatis.logging.slf4j.Slf4jImpl;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.hutool.db.ds.simple.SimpleDataSource;

/**
 * 数据库连接管理器
 * 实现SqlSessionFactory的单例模式和连接池管理
 */
public class DatabaseConnectionManager {
    private static final Logger log = LoggerFactory.getLogger(DatabaseConnectionManager.class);
    private static volatile DatabaseConnectionManager instance;
    private SqlSessionFactory sqlSessionFactory;

    // 私有构造函数，防止外部实例化
    private DatabaseConnectionManager() {
    }

    /**
     * 获取数据库连接管理器实例
     * 使用双重检查锁定的单例模式
     */
    public static DatabaseConnectionManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnectionManager.class) {
                if (instance == null) {
                    instance = new DatabaseConnectionManager();
                }
            }
        }
        return instance;
    }

    /**
     * 获取SqlSessionFactory
     */
    public SqlSessionFactory getSqlSessionFactory() {
        if (sqlSessionFactory == null) {
            synchronized (this) {
                if (sqlSessionFactory == null) {
                    try {
                        initSqlSessionFactory();
                    } catch (SQLException e) {
                        throw new RuntimeException("初始化SqlSessionFactory失败", e);
                    }
                }
            }
        }
        return sqlSessionFactory;
    }

    /**
     * 初始化SqlSessionFactory
     */
    private void initSqlSessionFactory() throws SQLException {
        // 获取数据库配置
        DatabaseProperty db = ConfigProperty.getInstance().getDb();
        if (db == null) {
            throw new SQLException("数据库配置不能为空");
        }

        // 验证配置参数
        validateDatabaseConfig(db);

        // 创建数据源
        SimpleDataSource dataSource = new SimpleDataSource(
                db.getUrl(),
                db.getUsername(),
                db.getPassword());
        
        // 创建MyBatis环境
        Environment environment = new Environment("development", new JdbcTransactionFactory(), dataSource);
        
        // 创建MyBatis配置
        MybatisConfiguration configuration = new MybatisConfiguration(environment);
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setLogImpl(Slf4jImpl.class);
        
        // Add Pagination Interceptor
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.H2));
        configuration.addInterceptor(interceptor);
        
        // 注册Mappers
        configuration.addMapper(SysUserMapper.class);
        configuration.addMapper(ProxyClientMapper.class);
        configuration.addMapper(ProxyClientRuleMapper.class);
        configuration.addMapper(SysLogMapper.class);
        configuration.addMapper(TsReportMapper.class);
        configuration.addMapper(TsDayReportMapper.class);
        configuration.addMapper(TsHourReportMapper.class);

        // 创建SqlSessionFactory
        this.sqlSessionFactory = new MybatisSqlSessionFactoryBuilder().build(configuration);
        log.info("SqlSessionFactory initialized successfully.");
    }

    /**
     * 验证数据库配置
     */
    private void validateDatabaseConfig(DatabaseProperty db) throws SQLException {
        if (db.getUrl() == null || db.getUrl().trim().isEmpty()) {
            throw new RuntimeException("数据库URL不能为空");
        }
        if (db.getUsername() == null || db.getUsername().trim().isEmpty()) {
            throw new RuntimeException("数据库用户名不能为空");
        }
        if (db.getPassword() == null) {
            throw new RuntimeException("数据库密码不能为空");
        }
    }
}
