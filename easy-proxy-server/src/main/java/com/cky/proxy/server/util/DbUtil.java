package com.cky.proxy.server.util;

import cn.hutool.core.collection.IterUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.FileResource;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.json.JSONUtil;
import com.cky.proxy.common.domain.ProxyClientConfig;
import com.cky.proxy.server.domain.DataModifyEvent;
import io.vertx.core.Handler;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Slf4j
public class DbUtil {
    private static Map<String, ProxyClientConfig> tokenConfigMap = null;

    private static Handler<DataModifyEvent> dataModifyHandler = (e) -> {
    };

    public static void onDataModify(Handler<DataModifyEvent> handler) {
        dataModifyHandler = handler;
    }

    public static Collection<ProxyClientConfig> getProxyClients() {
        if (tokenConfigMap != null) {
            return tokenConfigMap.values();
        }
        String json;
        FileResource fileResource = getConfigFile();
        json = fileResource.readStr(StandardCharsets.UTF_8);
        List<ProxyClientConfig> proxyClientConfigs = JSONUtil.toBean(json, new TypeReference<List<ProxyClientConfig>>() {
        }, true);
        tokenConfigMap = IterUtil.toMap(proxyClientConfigs, ProxyClientConfig::getToken);
        return proxyClientConfigs;
    }

    // 根据 token 获取单个客户端配置
    public static ProxyClientConfig getProxyClientByToken(String token) {
        if (tokenConfigMap == null) {
            getProxyClients();
        }
        return tokenConfigMap.get(token);
    }

    public static void updateProxyClient(ProxyClientConfig proxyClientConfig) {
        tokenConfigMap.put(proxyClientConfig.getToken(), proxyClientConfig);
        syncConfigFile();
        dataModifyHandler.handle(DataModifyEvent.update(proxyClientConfig));
    }

    public static void addProxyClient(ProxyClientConfig proxyClientConfig) {
        tokenConfigMap.put(proxyClientConfig.getToken(), proxyClientConfig);
        syncConfigFile();
        dataModifyHandler.handle(DataModifyEvent.add(proxyClientConfig));
    }

    public static void deleteProxyClient(ProxyClientConfig proxyClientConfig) {
        tokenConfigMap.remove(proxyClientConfig.getToken());
        syncConfigFile();
        dataModifyHandler.handle(DataModifyEvent.delete(proxyClientConfig));
    }

    private static void syncConfigFile() {
        try {
            FileResource fileResource = getConfigFile();
            File configFile = fileResource.getFile();
            FileUtil.writeString(JSONUtil.toJsonStr(tokenConfigMap.values()), configFile, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private static FileResource getConfigFile() {
        String userHome = System.getProperty("user.home");
        String dbDir = userHome + File.separator + ".easyproxy";
        String filePath = dbDir + File.separator + "db.json";
        if (!FileUtil.exist(dbDir)) {
            FileUtil.mkdir(dbDir);
        }
        if (!FileUtil.exist(filePath)) {
            FileUtil.newFile(filePath);
        }
        return new FileResource(filePath);
    }
}
