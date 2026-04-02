package com.cky.proxy.server.util;

import lombok.extern.slf4j.Slf4j;
import java.io.OutputStream;
import java.io.IOException;

@Slf4j
public class TokenBucket {
    public static final int CHUNK_SIZE = 8 * 1024; // 分片长度
    private final double refillRatePerSec; // 补充速度 字节/秒

    private double storedPermits;
    private final double maxPermits;
    private long nextFreeTicketMicros;

    public TokenBucket(int refillRatePerSec) {
        this.refillRatePerSec = refillRatePerSec;
        this.maxPermits = refillRatePerSec; // 最多存1秒的突发
        this.storedPermits = maxPermits;
        this.nextFreeTicketMicros = System.nanoTime() / 1000;
    }

    private void resync(long nowMicros) {
        if (nowMicros > nextFreeTicketMicros) {
            double newPermits = (nowMicros - nextFreeTicketMicros) / 1000000.0 * refillRatePerSec;
            storedPermits = Math.min(maxPermits, storedPermits + newPermits);
            nextFreeTicketMicros = nowMicros;
        }
    }

    public void acquire(int permits) {
        long sleepMicros = reserveAndGetWaitLength(permits);
        if (sleepMicros > 0) {
            try {
                Thread.sleep(sleepMicros / 1000, (int) ((sleepMicros % 1000) * 1000));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private synchronized long reserveAndGetWaitLength(int permits) {
        long nowMicros = System.nanoTime() / 1000;
        resync(nowMicros);

        long momentAvailable = nextFreeTicketMicros;
        double storedPermitsToSpend = Math.min(permits, storedPermits);
        double freshPermits = permits - storedPermitsToSpend;
        long waitMicros = (long) (freshPermits * 1000000.0 / refillRatePerSec);

        this.nextFreeTicketMicros += waitMicros;
        this.storedPermits -= storedPermitsToSpend;

        return momentAvailable - nowMicros;
    }

    public void writeWithLimit(OutputStream out, byte[] data) throws IOException {
        int offset = 0;
        int remaining = data.length;

        while (remaining > 0) {
            int chunkSize = Math.min(remaining, CHUNK_SIZE);
            acquire(chunkSize);
            out.write(data, offset, chunkSize);
            offset += chunkSize;
            remaining -= chunkSize;
        }
        out.flush();
    }

    public void flush() {
        // 无需实现，仅为了兼容老代码接口
    }
}