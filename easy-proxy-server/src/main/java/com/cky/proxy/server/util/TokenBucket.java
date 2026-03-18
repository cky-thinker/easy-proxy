package com.cky.proxy.server.util;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetSocket;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

@Slf4j
public class TokenBucket {
    public static final int CHUNK_SIZE = 8 * 1024; // 分片长度
    private static final int REFILL_INTERVAL = 100; // 补充速度 单位 ms

    private final Vertx vertx;
    private final int capacity; // 桶容量
    private final int refillTokens; // 每次补充令牌数
    private int tokens; // 当前令牌数

    private final LinkedBlockingQueue<Handler<Long>> waitQueue;
    private final int backpressureThreshold; // 背压阈值
    private final long timerId;

    /**
     * @param vertx            Vertx实例
     * @param refillRatePerSec 每秒字节数
     */
    public TokenBucket(Vertx vertx, int refillRatePerSec) {
        this.vertx = vertx;
        this.capacity = refillRatePerSec / 4; // 令牌桶容量，应对250ms突发窗口
        this.tokens = capacity;
        this.waitQueue = new LinkedBlockingQueue<>();
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
            boolean ok = acquire(chunkSize, okBytes -> {
                // 3. 拿到分片后转写
                writeAction.accept(chunk);
            });

            if (!ok) {
                // 异常情况，chunk会丢失没有写入
                log.error("Token bucket acquire fail, Data may be lost.");
            }

            offset += chunkSize;
            remaining -= chunkSize;
        }
    }

    // 定时补令牌
    private void refill() {
        tokens = Math.min(capacity, tokens + refillTokens);
        drainQueue();
    }

    /**
     * 异步获取令牌
     *
     * @return true=加入队列成功, false=队列已满
     */
    public boolean acquire(long bytes, Handler<Long> callback) {
        if (tokens >= bytes) {
            tokens -= bytes;
            callback.handle(bytes);
            return true;
        } else {
            // 加入等待队列
            return waitQueue.offer(callback);
        }
    }

    // 消费等待队列
    private void drainQueue() {
        while (!waitQueue.isEmpty() && tokens > 0) {
            Handler<Long> handler = waitQueue.poll();
            if (handler == null)
                break;

            // 每次最多发一个分片，保证平滑
            long take = Math.min(tokens, CHUNK_SIZE);
            tokens -= take;
            handler.handle(take);
        }
    }

    // 用于背压判断
    public int waitingQueueSize() {
        return waitQueue.size();
    }

    public boolean isBackpressure() {
        return waitingQueueSize() >= backpressureThreshold;
    }

    public void stop() {
        vertx.cancelTimer(timerId);
    }
}
