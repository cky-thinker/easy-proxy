package com.cky.proxy.client.context;

import java.net.Socket;
import java.io.IOException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * userId -> proxySocket 应用socket 上下文
 */
public class ProxySocketContext {
    private final static Map<String, Socket> userIdProxySocketMap = new ConcurrentHashMap<>();

    public static Socket getProxySocket(String token) {
        return userIdProxySocketMap.get(token);
    }

    public static void online(String token, Socket mngSocket) {
        userIdProxySocketMap.put(token, mngSocket);
    }

    public static void offline(String token) {
        userIdProxySocketMap.remove(token);
    }

    public static void close(String userId) {
        Socket proxySocket = userIdProxySocketMap.remove(userId);
        if (proxySocket != null) {
            try {
                proxySocket.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    public static void closeAll() {
        for (Socket proxySocket : userIdProxySocketMap.values().toArray(new Socket[0])) {
            try {
                proxySocket.close();
            } catch (Exception e) {
                // ignore
            }
        }
        userIdProxySocketMap.clear();
    }
}
