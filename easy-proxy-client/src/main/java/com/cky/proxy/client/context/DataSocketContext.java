package com.cky.proxy.client.context;

import io.vertx.core.net.NetSocket;

import java.util.HashMap;

/**
 * userId -> dataSocket 数据socket 上下文
 */
public class DataSocketContext {
    private static final HashMap<String, NetSocket> userIdSocketMap = new HashMap<>();

    public static void online(String userId, NetSocket dataSocket) {
        userIdSocketMap.put(userId, dataSocket);
    }

    public static void offline(String userId) {
        userIdSocketMap.remove(userId);
    }

    public static void closeAll() {
        for (NetSocket dataSocket : userIdSocketMap.values()) {
            dataSocket.close();
        }
        userIdSocketMap.clear();
    }

    public static void close(String userId) {
        NetSocket dataSocket = userIdSocketMap.get(userId);
        if (dataSocket != null) {
            dataSocket.close();
            offline(userId);
        }
    }

    public static NetSocket getDataSocket(String userId) {
        return userIdSocketMap.get(userId);
    }
}
