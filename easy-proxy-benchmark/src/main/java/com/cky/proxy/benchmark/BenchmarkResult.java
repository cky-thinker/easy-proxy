package com.cky.proxy.benchmark;

import java.util.concurrent.atomic.LongAdder;

/**
 * 测试结果统计
 */
public record BenchmarkResult(
    long totalRequests,
    long successRequests,
    long failureRequests,
    long totalBytes,
    long minLatencyNs,
    long maxLatencyNs,
    long avgLatencyNs,
    double throughputMbPerSec
) {
    public void print() {
        System.out.println("================ Benchmark Report ================");
        System.out.println("Total Requests: " + totalRequests);
        System.out.println("Success: " + successRequests);
        System.out.println("Failure: " + failureRequests);
        System.out.println("Total Transferred: " + (totalBytes / 1024 / 1024) + " MB");
        System.out.println("Min Latency: " + (minLatencyNs / 1_000_000.0) + " ms");
        System.out.println("Max Latency: " + (maxLatencyNs / 1_000_000.0) + " ms");
        System.out.println("Avg Latency: " + (avgLatencyNs / 1_000_000.0) + " ms");
        System.out.println("Throughput: " + String.format("%.2f", throughputMbPerSec) + " MB/s");
        System.out.println("==================================================");
    }
}
