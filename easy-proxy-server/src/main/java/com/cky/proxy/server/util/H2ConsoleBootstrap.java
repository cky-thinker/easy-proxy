
package com.cky.proxy.server.util;

import org.h2.tools.Server;

import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;

@Slf4j
public class H2ConsoleBootstrap {
    private static Thread h2ConsoleThread = null;
    private static Server webServer = null;
    private static Server tcpServer = null;

    public static void startup() {
        h2ConsoleThread = new Thread(() -> {
            try {
                // 1. 启动H2 TCP服务器（可选，支持远程连接，推荐开启）
                tcpServer = Server.createTcpServer(
                    "-tcp",          // 启用TCP服务
                    "-tcpPort", "9092",  // 指定TCP端口（默认9092，可自定义）
                    "-tcpAllowOthers"    // 允许其他机器连接（仅测试用，生产禁用）
                ).start();
                System.out.println("✅ H2 TCP服务器启动成功，端口：" + tcpServer.getPort());
                // 启动H2 Web Console（核心，浏览器访问）
                webServer = Server.createWebServer(
                        "-web", // 启用Web控制台
                        "-webPort", "10093", // 指定Web端口（默认8082，可自定义）
                        "-webAllowOthers" // 允许其他机器访问（仅测试用）
                ).start();
                log.info("✅ H2 Web Console启动成功，访问地址：http://localhost:10093");
                log.info("🔑 推荐JDBC URL：jdbc:h2:tcp://localhost:10093/./data/database");

                // 保持程序运行（否则控制台启动后会立即退出）
                synchronized (H2ConsoleBootstrap.class) {
                    H2ConsoleBootstrap.class.wait();
                }

            } catch (SQLException | InterruptedException e) {
                log.error("❌ 启动H2 Console失败：{}", e.getMessage(), e);
            }
        });
        h2ConsoleThread.start();
    }

    public void shutdown() {
        if (webServer != null) {
            webServer.stop();
            log.info("✅ H2 Web Console已关闭");
        }
        webServer = null;
        if (tcpServer != null) {
            tcpServer.stop();
            log.info("✅ H2 TCP服务器已关闭");
        }
        tcpServer = null;
        if (h2ConsoleThread != null) {
            h2ConsoleThread.interrupt();
            log.info("✅ H2 Console线程已中断");
        }
        
    }
}