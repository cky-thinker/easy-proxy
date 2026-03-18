package com.cky.proxy.server.util;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetSocket;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Consumer;

@Slf4j
public class TokenBucket {
    public static final int CHUNK_SIZE = 8 * 1024; // 分片长度
    private static final int REFILL_INTERVAL = 100; // 补充速度 单位 ms

    private final Vertx vertx;
    private final int capacity; // 桶容量
    private final int refillTokens; // 每次补充令牌数
    private int tokens; // 当前令牌数

    private final Queue<TokenRequest> waitQueue;
    private final int backpressureThreshold; // 背压阈值
    private final long timerId;

    private static class TokenRequest {
        final long bytes;
        final Handler<Long> callback;

        TokenRequest(long bytes, Handler<Long> callback) {
            this.bytes = bytes;
            this.callback = callback;
        }
    }

    /**
     * @param vertx            Vertx实例
     * @param refillRatePerSec 每秒字节数
     */
    public TokenBucket(Vertx vertx, int refillRatePerSec) {
        this.vertx = vertx;
        this.capacity = refillRatePerSec / 4; // 令牌桶容量，应对250ms突发窗口
        this.tokens = capacity;
        this.waitQueue = new LinkedList<>();
        // 不限制队列长度，仅用于背压处理，防止数据丢失
        int maxQueueSize = (int) (capacity * 2.5 / CHUNK_SIZE); // 最大等待队列长度 桶容量 * 2.5 / CHUNCK_SIZE
        this.backpressureThreshold = (int) (maxQueueSize * 0.75); // 背压阈值 桶容量 * 0.75
        // 每100ms补充一次令牌，更平滑
        this.timerId = vertx.setPeriodic(REFILL_INTERVAL, id -> refill());
        this.refillTokens = refillRatePerSec * REFILL_INTERVAL / 1000;
    }

    public void writeWithLimit(NetSocket socket, byte[] data, Consumer<byte[]> writeAction) {
        // 1. 等待队列过长 → 背压：暂停读取
        if (isBackpressure()) {
            socket.pause();
            // 100ms 后恢复读取，给令牌桶产出时间
            vertx.setTimer(100, t -> socket.resume());
            log.debug("TokenBucket >> Backpressure, pause socket");
        }

        int offset = 0;
        int remaining = data.length;

        // 分片限速发送，防止单块过大
        while (remaining > 0) {
            int chunkSize = Math.min(remaining, TokenBucket.CHUNK_SIZE);
            byte[] chunk = new byte[chunkSize];
            System.arraycopy(data, offset, chunk, 0, chunkSize);

            // 2. 全局上行限速
            acquire(chunkSize, okBytes -> {
                // 3. 拿到分片后转写
                writeAction.accept(chunk);
            });

            offset += chunkSize;
            remaining -= chunkSize;
        }
    }

    // 定时补令牌
    private synchronized void refill() {
        tokens = Math.min(capacity, tokens + refillTokens);
        drainQueue();
    }

    /**
     * 异步获取令牌
     */
    public synchronized void acquire(long bytes, Handler<Long> callback) {
        // 严格 FIFO：如果队列不为空，或者令牌不足，都必须入队等待
        if (waitQueue.isEmpty()) {
            if (tokens >= bytes) {
                tokens -= bytes;
                callback.handle(bytes);
                return;
            } else if (bytes > capacity && tokens >= capacity) {
                // 特殊处理：请求大小超过桶容量，且桶已满 -> 允许透支
                tokens -= bytes;
                callback.handle(bytes);
                return;
            }
        }
        
        // 加入等待队列
        waitQueue.offer(new TokenRequest(bytes, callback));
        // 尝试消费
        drainQueue();
    }

    // 消费等待队列
    private synchronized void drainQueue() {
        while (!waitQueue.isEmpty()) {
            TokenRequest req = waitQueue.peek();
            if (tokens >= req.bytes) {
                waitQueue.poll();
                tokens -= req.bytes;
                req.callback.handle(req.bytes);
            } else if (req.bytes > capacity && tokens >= capacity) {
                // 特殊处理：请求大小超过桶容量，且桶已满 -> 允许透支
                waitQueue.poll();
                tokens -= req.bytes;
                req.callback.handle(req.bytes);
            } else {
                // 头部请求令牌不足，停止处理，保证顺序
                break;
            }
        }
    }

    // 用于背压判断
    public synchronized int waitingQueueSize() {
        return waitQueue.size();
    }

    public boolean isBackpressure() {
        return waitingQueueSize() >= backpressureThreshold;
    }

    /**
     * 停止并立即发送所有积压数据
     */
    public synchronized void flush() {
        vertx.cancelTimer(timerId);
        while (!waitQueue.isEmpty()) {
            TokenRequest req = waitQueue.poll();
            // 忽略令牌限制，直接发送
            req.callback.handle(req.bytes);
        }
        log.info("TokenBucket flushed {} requests.", tokens); 
    }
}
