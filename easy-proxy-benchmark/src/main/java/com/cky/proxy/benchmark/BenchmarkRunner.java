package com.cky.proxy.benchmark;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;

@Slf4j
public class BenchmarkRunner {

    private final int concurrency;
    private final int totalRequests;
    private final long durationSeconds;
    private final Supplier<BenchmarkTask> taskSupplier;

    public BenchmarkRunner(int concurrency, int totalRequests, Supplier<BenchmarkTask> taskSupplier) {
        this(concurrency, totalRequests, 0, taskSupplier);
    }

    public BenchmarkRunner(int concurrency, long durationSeconds, Supplier<BenchmarkTask> taskSupplier) {
        this(concurrency, 0, durationSeconds, taskSupplier);
    }

    private BenchmarkRunner(int concurrency, int totalRequests, long durationSeconds, Supplier<BenchmarkTask> taskSupplier) {
        this.concurrency = concurrency;
        this.totalRequests = totalRequests;
        this.durationSeconds = durationSeconds;
        this.taskSupplier = taskSupplier;
    }

    public BenchmarkResult run() throws InterruptedException {
        boolean isDurationMode = durationSeconds > 0;
        log.info("Starting benchmark with concurrency: {}, {}", concurrency, 
            isDurationMode ? "duration: " + durationSeconds + "s" : "total requests: " + totalRequests);

        LongAdder successCount = new LongAdder();
        LongAdder failureCount = new LongAdder();
        LongAdder totalBytes = new LongAdder();
        LongAdder totalLatencyNs = new LongAdder();
        
        final long[] minLatency = {Long.MAX_VALUE};
        final long[] maxLatency = {0};

        CountDownLatch latch = isDurationMode ? new CountDownLatch(concurrency) : new CountDownLatch(totalRequests);
        long startTime = System.nanoTime();
        long endTimeLimit = startTime + (durationSeconds * 1_000_000_000L);

        for (int i = 0; i < concurrency; i++) {
            Thread.ofVirtual().start(() -> {
                try {
                    if (isDurationMode) {
                        while (System.nanoTime() < endTimeLimit) {
                            executeOneTask(taskSupplier, successCount, failureCount, totalBytes, totalLatencyNs, minLatency, maxLatency);
                        }
                    } else {
                        int requestsPerThread = totalRequests / concurrency;
                        for (int j = 0; j < requestsPerThread; j++) {
                            executeOneTask(taskSupplier, successCount, failureCount, totalBytes, totalLatencyNs, minLatency, maxLatency);
                            latch.countDown();
                        }
                    }
                } finally {
                    if (isDurationMode) {
                        latch.countDown();
                    }
                }
            });
        }

        latch.await();
        long endTime = System.nanoTime();
        long actualDurationNs = endTime - startTime;
        double durationSec = actualDurationNs / 1_000_000_000.0;

        return new BenchmarkResult(
            successCount.sum() + failureCount.sum(),
            successCount.sum(),
            failureCount.sum(),
            totalBytes.sum(),
            minLatency[0] == Long.MAX_VALUE ? 0 : minLatency[0],
            maxLatency[0],
            successCount.sum() == 0 ? 0 : totalLatencyNs.sum() / successCount.sum(),
            (totalBytes.sum() / 1024.0 / 1024.0) / durationSec
        );
    }

    private void executeOneTask(Supplier<BenchmarkTask> taskSupplier, LongAdder successCount, LongAdder failureCount, 
                                LongAdder totalBytes, LongAdder totalLatencyNs, long[] minLatency, long[] maxLatency) {
        BenchmarkTask task = taskSupplier.get();
        long startTask = System.nanoTime();
        try {
            long bytes = task.execute();
            long endTask = System.nanoTime();
            long latency = endTask - startTask;
            
            successCount.increment();
            totalBytes.add(bytes);
            totalLatencyNs.add(latency);
            
            synchronized (minLatency) {
                if (latency < minLatency[0]) minLatency[0] = latency;
                if (latency > maxLatency[0]) maxLatency[0] = latency;
            }
        } catch (Exception e) {
            failureCount.increment();
            log.error("Task failed: {}", e.getMessage());
        }
    }
}
