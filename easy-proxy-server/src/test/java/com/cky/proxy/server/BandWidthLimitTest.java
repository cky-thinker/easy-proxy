package com.cky.proxy.server;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

@Slf4j
public class BandWidthLimitTest {
    @Test
    public void testBandWidthLimit() {
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
    }

    public static class BandWidthLimitVerticle extends AbstractVerticle {
        @Override
        public void start(Promise<Void> startPromise) throws Exception {
            vertx.createNetServer()
                .connectHandler((socket) -> {
                    socket.handler((buf) -> {
                        log.info("recv: {}", buf.toString());
                        // 发送文件
                        String filePath = "/Users/chenkeyu/Downloads/卡口资料文档.rar";
                        FileSystem fs = vertx.fileSystem();
                        fs.readFile(filePath, result -> {
                            if (result.succeeded()) {
                                socket.write(result.result());
                                log.info("File sent successfully");
                            } else {
                                String errorMsg = "Error reading file: " + result.cause().getMessage();
                                socket.write(errorMsg);
                                log.error("Failed to read file", result.cause());
                            }
                            socket.close();
                        });
                    });
                })
                .listen(10001)
                .onFailure(t -> log.error("Server start failed", t));
        }
    }
}


