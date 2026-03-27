package com.cky.proxy.client;

import com.cky.proxy.client.config.ConfigProperty;
import com.cky.proxy.client.util.CertDownloader;
import com.cky.proxy.common.util.ConfigBootstrap;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProxyClient {
    public static void main(String[] args) throws InterruptedException {
        // 全局异常处理
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            log.error("Uncaught exception in thread {}: {}", t.getName(), e.getMessage(), e);
        });

        // 检查并复制默认配置文件
        ConfigBootstrap.initConfigs(ConfigProperty.CONFIG_FILE);
        // 检查并下载证书：不存在则下载证书
        try {
            CertDownloader.downloadIfNotExists();
        } catch (Exception e) {
            log.error("download cert fail!", e);
        }

        // 启动客户端
        new ClientRunner().start();

        // 保持主线程存活
        Thread.currentThread().join();
    }
}
