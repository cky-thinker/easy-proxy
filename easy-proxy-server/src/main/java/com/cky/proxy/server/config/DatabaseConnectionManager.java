package com.cky.proxy.server.config;

import java.sql.SQLException;

import com.j256.ormlite.jdbc.JdbcConnectionSource;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * 数据库连接管理器
 * 实现ConnectionSource的单例模式和连接池管理
 */
@Slf4j
public class DatabaseConnectionManager {
    private static volatile DatabaseConnectionManager instance;

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
     * 初始化数据库连接源
     */
    @SneakyThrows
    public JdbcConnectionSource createConnectionSource() {
        // 获取数据库配置
        DatabaseProperty db = ConfigProperty.getInstance().getDb();
        if (db == null) {
            throw new SQLException("数据库配置不能为空");
        }

        // 验证配置参数
        validateDatabaseConfig(db);

        // 创建新的连接源
        return new JdbcConnectionSource(
                db.getUrl(),
                db.getUsername(),
                db.getPassword());
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
