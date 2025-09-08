package com.cky.proxy.server.util;

import cn.hutool.setting.dialect.Props;
import cn.hutool.setting.dialect.PropsUtil;

public class ConfigUtil {

    public static Props getConfig() {
        return PropsUtil.get("config.properties");
    }

    public static String getProperty(String prop) {
        return ConfigUtil.getConfig().getProperty(prop);
    }

    public static String getStr(String prop) {
        return ConfigUtil.getConfig().getProperty(prop);
    }

    public static Integer getInt(String prop) {
        return ConfigUtil.getConfig().getInt(prop);
    }
}
