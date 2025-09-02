package com.cky.proxy.server;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.net.NetSocket;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BandWidthLimitVerticle extends AbstractVerticle {
    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        vertx.createNetServer()
            .connectHandler((socket) -> {
                socket.handler((buf) -> {
                    log.info("recv: {}", buf.toString());
                    // 发送文件
                    String filePath = "/Users/chenkeyu/Downloads/视频1.mp4";
                    FileSystem fs = vertx.fileSystem();
                    fs.readFile(filePath, result -> {
                        if (result.succeeded()) {
                            // 带宽限流：1Mbps = 1024 * 1024 / 8 = 131072 bytes/second
                            int bandwidthLimit = 1024 * 1024; // bytes per second
                            int chunkSize = 8192; // 8KB chunks
                            long delayMs = (chunkSize * 1000L) / bandwidthLimit; // delay between chunks

                            byte[] fileData = result.result().getBytes();
                            sendWithBandwidthLimit(socket, fileData, chunkSize, delayMs, 0);
                            log.info("File sent successfully with bandwidth limit");
                        } else {
                            String errorMsg = "Error reading file: " + result.cause().getMessage();
                            socket.write(errorMsg);
                            log.error("Failed to read file", result.cause());
                            socket.close();
                        }
                    });
                });
            })
            .listen(10001)
            .onFailure(t -> log.error("Server start failed", t));
    }

    private void sendWithBandwidthLimit(NetSocket socket, byte[] data, int chunkSize, long delayMs, int offset) {
        if (offset >= data.length) {
            socket.close();
            return;
        }

        int remainingBytes = data.length - offset;
        int currentChunkSize = Math.min(chunkSize, remainingBytes);

        byte[] chunk = new byte[currentChunkSize];
        System.arraycopy(data, offset, chunk, 0, currentChunkSize);

        socket.write(Buffer.buffer(chunk));

        // Schedule next chunk with delay
        vertx.setTimer(delayMs, id -> {
            sendWithBandwidthLimit(socket, data, chunkSize, delayMs, offset + currentChunkSize);
        });
    }
}
