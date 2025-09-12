package com.cky.proxy.server.config;

import com.cky.proxy.common.util.PathUtil;

import cn.hutool.core.io.FileUtil;
import cn.hutool.setting.yaml.YamlUtil;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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
                    String configFilePath = jarFilePath.substring(0, jarFilePath.lastIndexOf("/")) + "/config.yaml";
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

    // 保持向后兼容性
    public static ConfigProperty getSysConfig() {
        return getInstance();
    }
}
