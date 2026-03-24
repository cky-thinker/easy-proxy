package com.cky.proxy.client.context;

import java.net.Socket;
import java.io.IOException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * userId -> dataSocket 数据socket 上下文
 */
public class DataSocketContext {
    private static final Map<String, Socket> userIdSocketMap = new ConcurrentHashMap<>();

    public static void online(String userId, Socket dataSocket) {
        userIdSocketMap.put(userId, dataSocket);
    }

    public static void offline(String userId) {
        userIdSocketMap.remove(userId);
    }

    public static void closeAll() {
        for (Socket dataSocket : userIdSocketMap.values().toArray(new Socket[0])) {
            try {
                dataSocket.close();
            } catch (Exception e) {
                // ignore
            }
        }
        userIdSocketMap.clear();
    }

    public static void close(String userId) {
        Socket dataSocket = userIdSocketMap.remove(userId);
        if (dataSocket != null) {
            try {
                dataSocket.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    public static Socket getDataSocket(String userId) {
        return userIdSocketMap.get(userId);
    }
}
