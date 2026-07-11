package com.cky.proxy.server.config;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.setting.yaml.YamlUtil;
import lombok.Data;

import com.cky.proxy.common.util.PathUtil;

import java.io.File;

@Data
public class ConfigProperty {
    public static final String CONFIG_FILE = "server.yaml";
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
                    String configFilePath = jarFilePath + File.separator + CONFIG_FILE;
                    if (FileUtil.exist(configFilePath)) {
                        instance = YamlUtil.loadByPath(configFilePath, ConfigProperty.class);
                    } else {
                        instance = YamlUtil.loadByPath(CONFIG_FILE, ConfigProperty.class);
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

        String webhook = System.getenv("SERVER_WEBHOOK");
        if (StrUtil.isNotBlank(webhook)) {
            this.server.setWebhook(webhook);
        }

        String loginFailureThreshold = System.getenv("SERVER_LOGIN_FAILURE_THRESHOLD");
        if (StrUtil.isNotBlank(loginFailureThreshold)) {
            this.server.setLoginFailureThreshold(Integer.parseInt(loginFailureThreshold));
        }

        String loginFailureWindowMinutes = System.getenv("SERVER_LOGIN_FAILURE_WINDOW_MINUTES");
        if (StrUtil.isNotBlank(loginFailureWindowMinutes)) {
            this.server.setLoginFailureWindowMinutes(Integer.parseInt(loginFailureWindowMinutes));
        }

        String loginLockMinutes = System.getenv("SERVER_LOGIN_LOCK_MINUTES");
        if (StrUtil.isNotBlank(loginLockMinutes)) {
            this.server.setLoginLockMinutes(Integer.parseInt(loginLockMinutes));
        }

        String loginIpFailureThreshold = System.getenv("SERVER_LOGIN_IP_FAILURE_THRESHOLD");
        if (StrUtil.isNotBlank(loginIpFailureThreshold)) {
            this.server.setLoginIpFailureThreshold(Integer.parseInt(loginIpFailureThreshold));
        }

        String loginIpWindowMinutes = System.getenv("SERVER_LOGIN_IP_WINDOW_MINUTES");
        if (StrUtil.isNotBlank(loginIpWindowMinutes)) {
            this.server.setLoginIpWindowMinutes(Integer.parseInt(loginIpWindowMinutes));
        }

        String loginIpBlockMinutes = System.getenv("SERVER_LOGIN_IP_BLOCK_MINUTES");
        if (StrUtil.isNotBlank(loginIpBlockMinutes)) {
            this.server.setLoginIpBlockMinutes(Integer.parseInt(loginIpBlockMinutes));
        }
    }
}
