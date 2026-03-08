package com.cky.proxy.server.config;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.setting.yaml.YamlUtil;
import lombok.Data;

import com.cky.proxy.common.util.PathUtil;

import java.io.File;

@Data
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
                    if (instance != null) {
                        instance.overrideWithEnv();
                    }
                }
            }
        }
        return instance;
    }

    private void overrideWithEnv() {
        if (this.server == null) {
            this.server = new ServerProperty();
        }

        String proxyPort = System.getenv("SERVER_PROXY_PORT");
        if (StrUtil.isNotBlank(proxyPort)) {
            this.server.setProxyPort(Integer.parseInt(proxyPort));
        }

        String webPort = System.getenv("SERVER_WEB_PORT");
        if (StrUtil.isNotBlank(webPort)) {
            this.server.setWebPort(Integer.parseInt(webPort));
        }

        String captchaImageEnable = System.getenv("SERVER_CAPTCHA_IMAGE_ENABLE");
        if (StrUtil.isNotBlank(captchaImageEnable)) {
            this.server.setCaptchaImageEnable(Boolean.parseBoolean(captchaImageEnable));
        }

        String publicHost = System.getenv("SERVER_PUBLIC_HOST");
        if (StrUtil.isNotBlank(publicHost)) {
            this.server.setPublicHost(publicHost);
        }

        String certValidityDays = System.getenv("SERVER_CERT_VALIDITY_DAYS");
        if (StrUtil.isNotBlank(certValidityDays)) {
            this.server.setCertValidityDays(Integer.parseInt(certValidityDays));
        }

        String certPassword = System.getenv("SERVER_CERT_PASSWORD");
        if (StrUtil.isNotBlank(certPassword)) {
            this.server.setCertPassword(certPassword);
        }
    }
}
