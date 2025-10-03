package com.cky.proxy.server.config;

import cn.hutool.core.io.FileUtil;
import cn.hutool.setting.yaml.YamlUtil;
import com.cky.proxy.common.util.PathUtil;

import java.io.File;

public class ConfigProperty {
    private static volatile ConfigProperty instance;

    private ServerProperty server;
    private DatabaseProperty db;

    // 私有构造函数，防止外部实例化
    private ConfigProperty() {
    }

    // 双重检查锁定的单例模式
    public static ConfigProperty getInstance() {
        if (instance == null) {
            synchronized (ConfigProperty.class) {
                if (instance == null) {
                    String jarFilePath = PathUtil.getJarFilePath(ConfigProperty.class);
                    String configFilePath = jarFilePath + File.separator + "config.yaml";
                    if (FileUtil.exist(configFilePath)) {
                        instance = YamlUtil.loadByPath(configFilePath, ConfigProperty.class);
                    } else {
                        instance = YamlUtil.loadByPath("config.yaml", ConfigProperty.class);
                    }
                }
            }
        }
        return instance;
    }

    public ServerProperty getServer() {
        return server;
    }

    public void setServer(ServerProperty server) {
        this.server = server;
    }

    public DatabaseProperty getDb() {
        return db;
    }

    public void setDb(DatabaseProperty db) {
        this.db = db;
    }
}
