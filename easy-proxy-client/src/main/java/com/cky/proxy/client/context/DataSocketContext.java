package com.cky.proxy.client.context;

import io.vertx.core.net.NetSocket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * userId -> dataSocket 数据socket 上下文
 */
public class DataSocketContext {
    private static final Map<String, NetSocket> userIdSocketMap = new ConcurrentHashMap<>();

    public static void online(String userId, NetSocket dataSocket) {
        userIdSocketMap.put(userId, dataSocket);
    }

    public static void offline(String userId) {
        userIdSocketMap.remove(userId);
    }

    public static void closeAll() {
        for (NetSocket dataSocket : userIdSocketMap.values().toArray(new NetSocket[0])) {
            try {
                dataSocket.close();
            } catch (Exception e) {
                // ignore
            }
        }
        userIdSocketMap.clear();
    }

    public static void close(String userId) {
        NetSocket dataSocket = userIdSocketMap.remove(userId);
        if (dataSocket != null) {
            try {
                dataSocket.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    public static NetSocket getDataSocket(String userId) {
        return userIdSocketMap.get(userId);
    }
}
