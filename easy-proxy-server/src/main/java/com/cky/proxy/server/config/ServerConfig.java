package com.cky.proxy.server.config;

import cn.hutool.setting.yaml.YamlUtil;

public class ServerConfig {
    private static ServerConfig serverConfig;

    static {
        serverConfig = YamlUtil.loadByPath("config.yaml", ServerConfig.class);
    }


}
