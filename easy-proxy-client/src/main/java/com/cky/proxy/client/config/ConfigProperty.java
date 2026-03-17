package com.cky.proxy.client.config;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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

    // 私有构造函数，防止外部实例化
    private ConfigProperty() {
    }

    // 双重检查锁定的单例模式
    public static ConfigProperty getInstance() {
        if (instance == null) {
            synchronized (ConfigProperty.class) {
                if (instance == null) {
                    Path target = Paths.get("config").resolve("config.yaml");
                    if (Files.exists(target)) {
                        instance = YamlUtil.loadByPath(target.toAbsolutePath().toString(), ConfigProperty.class);
                    } else {
                        instance = YamlUtil.loadByPath("config.yaml", ConfigProperty.class);
                    }
                }
            }
        }
        return instance;
    }
}
