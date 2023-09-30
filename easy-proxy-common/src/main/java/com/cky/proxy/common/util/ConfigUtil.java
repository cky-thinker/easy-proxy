package com.cky.proxy.common.util;

import cn.hutool.setting.dialect.Props;
import cn.hutool.setting.dialect.PropsUtil;

public class ConfigUtil {
    public static Props getConfig() {
        return PropsUtil.get("config.properties");
    }
}
