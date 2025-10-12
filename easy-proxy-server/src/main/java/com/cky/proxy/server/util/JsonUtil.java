
package com.cky.proxy.server.util;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONUtil;

public class JsonUtil {
    public static String toJson(Object obj) {
        // 不忽略空值，确保如 loginTime 等字段在为 null 时也返回
        JSONConfig config = JSONConfig.create().setIgnoreNullValue(false);
        return JSONUtil.parse(obj, config).toString();
    }

    public static <T> T parseJson(String json, Class<T> clazz) {
        return JSONUtil.toBean(json, clazz);
    }

    public static <T> T parseJson(String json, TypeReference<T> typeReference) {
        return JSONUtil.toBean(json, typeReference, false);
    }
}
