package com.cky.proxy.server;

import io.vertx.core.Future;
import io.vertx.core.Promise;

import java.time.LocalDateTime;

public class PromiseTest {
    public static void main(String[] args) {
        Promise<String> promise = Promise.promise();
        delay(() -> {
            promise.complete("----- 1 -----");
        }, 1000);
        Future<String> future = promise.future().transform(result -> {
            Promise<String> p2 = Promise.promise();
            String r1 = result.result();
            log(r1);
            delay(() -> {
                p2.complete("----- 2 -----");
            }, 1000);
            return p2.future();
        }).transform(result -> {
            Promise<String> p2 = Promise.promise();
            String r1 = result.result();
            log(r1);
            delay(() -> {
                p2.complete("----- 3 -----");
            }, 1000);
            if (true) {
                throw new RuntimeException("xxx");
            }
            return p2.future();
        }).transform(result -> {
            Promise<String> p2 = Promise.promise();
            if (result.succeeded()) {
                String r1 = result.result();
                log(r1);
                delay(() -> {
                    p2.complete("----- 4 -----");
                }, 1000);
            } else {
                result.cause().printStackTrace();
            }

            return p2.future();
        });

        future.onSuccess(msg -> {
            log(msg);
        });
        sleep(9000);
    }

    public static void log(String msg) {
        System.out.println(msg + " " + LocalDateTime.now());
    }

    public static void delay(Runnable task, long ms) {
        new Thread(() -> {
            sleep(ms);
            task.run();
        }).start();
    }

    public static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
