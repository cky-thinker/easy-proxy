package com.cky.proxy.benchmark;

import com.cky.proxy.benchmark.tasks.HttpDownloadTask;
import com.cky.proxy.benchmark.tasks.HttpHeadTask;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BenchmarkMain {

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            printUsage();
            return;
        }

        String type = args[0]; // throughput, latency, connection, stability
        int concurrency = Integer.parseInt(args[1]);
        int totalRequestsOrDuration = Integer.parseInt(args[2]);
        String url = args[3];

        BenchmarkRunner runner;
        if ("throughput".equalsIgnoreCase(type)) {
            runner = new BenchmarkRunner(concurrency, totalRequestsOrDuration, () -> new HttpDownloadTask(url));
        } else if ("latency".equalsIgnoreCase(type)) {
            runner = new BenchmarkRunner(concurrency, totalRequestsOrDuration, () -> new HttpHeadTask(url));
        } else if ("stability".equalsIgnoreCase(type)) {
            // 稳定性测试使用持续时间模式（单位：秒）
            runner = new BenchmarkRunner(concurrency, (long) totalRequestsOrDuration, () -> new HttpDownloadTask(url));
        } else if ("connection".equalsIgnoreCase(type)) {
            // 对于连接数测试，我们特殊处理
            runConnectionTest(concurrency, url);
            return;
        } else {
            printUsage();
            return;
        }

        BenchmarkResult result = runner.run();
        result.print();
    }

    private static void runConnectionTest(int targetConnections, String urlStr) throws InterruptedException {
        log.info("Starting connection test: aiming for {} connections to {}", targetConnections, urlStr);
        java.net.URI uri = java.net.URI.create(urlStr);
        String host = uri.getHost();
        int port = uri.getPort() == -1 ? 80 : uri.getPort();

        java.util.concurrent.atomic.LongAdder activeConnections = new java.util.concurrent.atomic.LongAdder();
        
        for (int i = 0; i < targetConnections; i++) {
            int index = i;
            Thread.ofVirtual().start(() -> {
                try (java.net.Socket socket = new java.net.Socket(host, port)) {
                    activeConnections.increment();
                    if (index % 100 == 0) {
                        log.info("Active connections: {}", activeConnections.sum());
                    }
                    // 保持连接
                    Thread.sleep(Long.MAX_VALUE);
                } catch (Exception e) {
                    log.error("Connection {} failed: {}", index, e.getMessage());
                } finally {
                    activeConnections.decrement();
                }
            });
            // 避免瞬间爆发压垮系统
            if (i % 100 == 0) Thread.sleep(50);
        }

        while (true) {
            log.info("Current active connections: {}", activeConnections.sum());
            Thread.sleep(5000);
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java -jar benchmark.jar <type> <concurrency> <totalRequests/durationSeconds> <url>");
        System.out.println("Types: throughput, latency, connection, stability");
        System.out.println("Example (Throughput): java -jar benchmark.jar throughput 100 1000 http://localhost:8080/test");
        System.out.println("Example (Stability): java -jar benchmark.jar stability 50 3600 http://localhost:8080/test (runs for 1 hour)");
    }
}
