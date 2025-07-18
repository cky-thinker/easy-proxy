
package com.cky.proxy.server.util;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.json.JSONUtil;

public class JsonUtil {
    public static String toJson(Object obj) {
        return JSONUtil.toJsonStr(obj);
    }

    public static <T> T parseJson(String json, Class<T> clazz) {
        return JSONUtil.toBean(json, clazz);
    }

    public static <T> T parseJson(String json, TypeReference<T> typeReference) {
        return JSONUtil.toBean(json, typeReference, false);
    }
}
