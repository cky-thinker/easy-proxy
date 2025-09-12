package com.cky.proxy.server;

import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
public class BandWidthLimitTest {
    // @Test
    public void testBandWidthLimit() throws InterruptedException {
        Vertx vertx = Vertx.vertx();
        vertx.exceptionHandler(t -> {
            log.error(t.getMessage(), t);
        });
        vertx.deployVerticle(BandWidthLimitVerticle.class.getCanonicalName(), res -> {
            if (res.succeeded()) {
                log.info("deploy MainVerticle success!");
            } else {
                log.error("deploy MainVerticle fail!", res.cause());
            }
        });
        Thread.sleep(60 * 60 * 1000);
    }

    // @Test
    public void bandwidthTest() throws Exception {
        String serverHost = "127.0.0.1";
        int serverPort = 10001;

        try (java.net.Socket socket = new java.net.Socket(serverHost, serverPort);
             java.io.InputStream inputStream = socket.getInputStream()) {

            byte[] buffer = new byte[8192]; // 8KB buffer
            long totalBytes = 0;
            long startTime = System.currentTimeMillis();
            long lastTime = startTime;
            long lastBytes = 0;

            log.info("开始下载文件...");

            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                totalBytes += bytesRead;

                long currentTime = System.currentTimeMillis();
                long elapsedTime = currentTime - startTime;

                // 每秒显示一次下载速度
                if (currentTime - lastTime >= 1000) {
                    long bytesInLastSecond = totalBytes - lastBytes;
                    double speedMbps = (bytesInLastSecond * 8.0) / (1024 * 1024); // 转换为 Mbps
                    double speedKBps = bytesInLastSecond / 1024.0; // 转换为 KB/s

                    log.info("下载进度: {} KB, 当前速度: {} KB/s ({} Mbps), 总耗时: {} ms",
                            totalBytes / 1024, speedKBps, speedMbps, elapsedTime);

                    lastTime = currentTime;
                    lastBytes = totalBytes;
                }
            }

            long totalTime = System.currentTimeMillis() - startTime;
            double avgSpeedMbps = (totalBytes * 8.0) / (1024 * 1024) / (totalTime / 1000.0);
            double avgSpeedKBps = (totalBytes / 1024.0) / (totalTime / 1000.0);

            log.info("下载完成! 总大小: {} KB, 总耗时: {} ms, 平均速度: {} KB/s ({} Mbps)",
                    totalBytes / 1024, totalTime, avgSpeedKBps, avgSpeedMbps);

        } catch (Exception e) {
            log.error("下载文件时发生错误: {}", e.getMessage(), e);
            throw e;
        }
    }
}


