package com.cky.proxy.server;

import com.cky.proxy.server.config.ConfigProperty;
import com.cky.proxy.server.util.BeanContext;
import com.cky.proxy.common.util.ConfigBootstrap;
import com.cky.proxy.server.util.EventBusUtil;
import com.cky.proxy.server.util.H2ConsoleBootstrap;
import com.cky.proxy.server.util.CertGenerator;
import com.cky.proxy.server.verticle.ProxyServerRunner;
import com.cky.proxy.server.verticle.WebManageServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyServer {
    private static final Logger log = LoggerFactory.getLogger(ProxyServer.class);

    public static void main(String[] args) {
        // 检查并复制默认配置文件
        ConfigBootstrap.initConfigs(ConfigProperty.CONFIG_FILE);
        // 初始化对象管理器
        BeanContext.getInstance().init();
        // 检查并生成证书：不存在则生成JKS证书+导出PEM公钥
        try {
            CertGenerator.generateIfNotExists();
        } catch (Exception e) {
            log.error("generate cert fail!", e);
        }

        EventBusUtil.init();
        BeanContext.getInstance().initUserService();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            log.error(e.getMessage(), e);
        });

        log.info("ProxyServer start...");
        Thread.ofVirtual().start(() -> new ProxyServerRunner().start());
        
        log.info("WebManage start...");
        Thread.ofVirtual().start(() -> new WebManageServer().start());

        // 启动H2 Console
        if (ConfigProperty.getInstance().getDb().isH2ConsoleEnable()) {
            H2ConsoleBootstrap.startup();
        }
        
        // Block main thread
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
