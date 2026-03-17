package com.cky.proxy.server;

import com.cky.proxy.server.util.TokenBucket;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;

public class GlobalRateLimitTcpServer {
    // 监听端口
    private static final int PORT = 8888;

    // ====================== 全局限速配置 ======================
    private static final int GLOBAL_UP_RATE = 128 * 1024; // 全局上行总带宽：128 KB/s
    private static final int GLOBAL_DOWN_RATE = 256 * 1024; // 全局下行总带宽：256 KB/s

    // 全局唯一令牌桶（所有连接共享）
    private static TokenBucket globalUpBucket;
    private static TokenBucket globalDownBucket;

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        // 创建全局桶
        globalUpBucket = new TokenBucket(vertx, GLOBAL_UP_RATE);
        globalDownBucket = new TokenBucket(vertx, GLOBAL_DOWN_RATE);

        NetServer server = vertx.createNetServer();

        server.connectHandler(socket -> {
            String remote = socket.remoteAddress().toString();
            System.out.println("新连接：" + remote);

            // 异常&关闭处理
            socket.exceptionHandler(e -> {
                System.err.println("连接异常 " + remote + ": " + e.getMessage());
                socket.close();
            });
            socket.closeHandler(v -> System.out.println("连接关闭：" + remote));

            // 【核心】上行读取 + 全局限速 + 背压
            socket.handler(buffer -> handleReadWithBackpressure(vertx, socket, buffer));
        });

        // 启动服务
        server.listen(PORT, "0.0.0.0", res -> {
            if (res.succeeded()) {
                System.out.println("===== 全局带宽限速服务启动成功 =====");
                System.out.println("监听端口：" + PORT);
                System.out.println("全局上行总带宽：" + GLOBAL_UP_RATE / 1024 + " KB/s");
                System.out.println("全局下行总带宽：" + GLOBAL_DOWN_RATE / 1024 + " KB/s");
                System.out.println("==================================");
            } else {
                System.err.println("启动失败：" + res.cause());
            }
        });
    }

    /**
     * 带背压的读取处理（不丢数据、不OOM）
     */
    private static void handleReadWithBackpressure(Vertx vertx, NetSocket socket, Buffer buffer) {
        // 1. 等待队列过长 → 背压：暂停读取
        if (globalUpBucket.isBackpressure()) {
            socket.pause();
            // 50ms 后恢复读取，给令牌桶消费时间
            vertx.setTimer(50, t -> socket.resume());
            return;
        }

        byte[] data = buffer.getBytes();
        int offset = 0;
        int remaining = data.length;

        // 分片限速发送，防止单块过大
        while (remaining > 0) {
            int chunkSize = Math.min(remaining, TokenBucket.CHUNK_SIZE);
            byte[] chunk = new byte[chunkSize];
            System.arraycopy(data, offset, chunk, 0, chunkSize);

            // 2. 全局上行限速
            boolean ok = globalUpBucket.acquire(chunkSize, okBytes -> {
                // 3. 拿到令牌后，下行也走全局限速
                writeWithRateLimit(socket, chunk);
            });

            if (!ok) {
                // 队列满，触发背压
                socket.pause();
                vertx.setTimer(100, t -> socket.resume());
                break;
            }

            offset += chunkSize;
            remaining -= chunkSize;
        }
    }

    /**
     * 下行全局限速 + 写队列保护
     */
    private static void writeWithRateLimit(NetSocket socket, byte[] data) {
        globalDownBucket.acquire(data.length, okBytes -> {
            // 处理Vert.x写队列满，防止内存暴涨
            if (socket.writeQueueFull()) {
                socket.drainHandler(v -> socket.write(Buffer.buffer(data)));
            } else {
                socket.write(Buffer.buffer(data));
            }
        });
    }
}