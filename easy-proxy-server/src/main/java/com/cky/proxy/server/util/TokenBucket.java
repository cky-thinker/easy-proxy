package com.cky.proxy.server.util;

import io.vertx.core.Vertx;
import io.vertx.core.net.NetSocket;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.function.Consumer;

public class TokenBucket {
    private static final int REFILL_INTERVAL = 100; // 补充速度 单位 ms

    private final Vertx vertx;
    private final int capacity; // 桶容量
    private final int refillTokens; // 每次补充令牌数
    private int tokens; // 当前令牌数

    private static class WriteTask {
        final NetSocket socket;
        final byte[] data;
        int offset;
        final Consumer<byte[]> action;

        WriteTask(NetSocket socket, byte[] data, Consumer<byte[]> action) {
            this.socket = socket;
            this.data = data;
            this.offset = 0;
            this.action = action;
        }

        int remaining() {
            return data.length - offset;
        }
    }

    private final LinkedList<WriteTask> waitQueue = new LinkedList<>();
    private int queuedBytes = 0;

    private final int highWaterMark; // 背压高水位
    private final int lowWaterMark;  // 恢复低水位

    private final Set<NetSocket> pausedSockets = new HashSet<>();
    private final long timerId;

    /**
     * @param vertx            Vertx实例
     * @param refillRatePerSec 每秒字节数
     */
    public TokenBucket(Vertx vertx, int refillRatePerSec) {
        this.vertx = vertx;
        this.capacity = refillRatePerSec / 4; // 令牌桶容量，应对250ms突发窗口
        this.tokens = capacity;
        this.refillTokens = refillRatePerSec * REFILL_INTERVAL / 1000;
        
        // 高水位设为2秒的限速量，低水位设为1秒的限速量
        this.highWaterMark = refillRatePerSec * 2;
        this.lowWaterMark = refillRatePerSec;

        // 每100ms补充一次令牌，更平滑
        this.timerId = vertx.setPeriodic(REFILL_INTERVAL, id -> refill());
    }

    public void writeWithLimit(NetSocket socket, byte[] data, Consumer<byte[]> writeAction) {
        if (data == null || data.length == 0) {
            return;
        }

        synchronized (this) {
            // 如果队列为空且令牌充足，直接发送，降低延迟
            if (waitQueue.isEmpty() && tokens >= data.length) {
                tokens -= data.length;
                writeAction.accept(data);
                return;
            }

            // 否则加入等待队列
            waitQueue.add(new WriteTask(socket, data, writeAction));
            queuedBytes += data.length;

            // 检查背压
            if (queuedBytes >= highWaterMark) {
                if (pausedSockets.add(socket)) {
                    try {
                        socket.pause();
                    } catch (Exception ignored) {}
                }
            }
        }
        
        // 尝试消费队列
        drainQueue();
    }

    // 定时补令牌
    private void refill() {
        synchronized (this) {
            tokens = Math.min(capacity, tokens + refillTokens);
        }
        drainQueue();
    }

    // 消费等待队列
    private void drainQueue() {
        synchronized (this) {
            while (!waitQueue.isEmpty() && tokens > 0) {
                WriteTask task = waitQueue.peek();
                int remaining = task.remaining();
                if (tokens >= remaining) {
                    // 令牌足够发送剩余的所有分片
                    waitQueue.poll();
                    tokens -= remaining;
                    queuedBytes -= remaining;
                    
                    if (task.offset == 0) {
                        task.action.accept(task.data);
                    } else {
                        byte[] toSend = new byte[remaining];
                        System.arraycopy(task.data, task.offset, toSend, 0, remaining);
                        task.action.accept(toSend);
                    }
                } else {
                    // 令牌不足，只能发送部分（按字节截断）
                    int sendBytes = tokens;
                    byte[] toSend = new byte[sendBytes];
                    System.arraycopy(task.data, task.offset, toSend, 0, sendBytes);
                    
                    task.offset += sendBytes;
                    tokens = 0;
                    queuedBytes -= sendBytes;
                    
                    task.action.accept(toSend);
                    break; // 令牌耗尽
                }
            }

            // 检查是否可以恢复被暂停的 Socket
            if (queuedBytes <= lowWaterMark && !pausedSockets.isEmpty()) {
                for (NetSocket s : pausedSockets) {
                    try {
                        s.resume();
                    } catch (Exception ignored) {
                        // 忽略可能因 socket 已关闭引发的异常
                    }
                }
                pausedSockets.clear();
            }
        }
    }

    public void stop() {
        vertx.cancelTimer(timerId);
        synchronized (this) {
            waitQueue.clear();
            queuedBytes = 0;
            for (NetSocket s : pausedSockets) {
                try { 
                    s.resume(); 
                } catch (Exception ignored) {}
            }
            pausedSockets.clear();
        }
    }
}