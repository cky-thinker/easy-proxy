package com.cky.proxy.client.context;

import io.vertx.core.net.NetSocket;

import java.util.HashMap;
import java.util.Map;

/**
 * userId -> proxySocket 应用socket 上下文
 */
public class ProxySocketContext {
    private final static Map<String, NetSocket> userIdProxySocketMap = new HashMap<>();

    public static NetSocket getProxySocket(String token) {
        return userIdProxySocketMap.get(token);
    }

    public static void online(String token, NetSocket mngSocket) {
        userIdProxySocketMap.put(token, mngSocket);
    }

    public static void offline(String token) {
        userIdProxySocketMap.remove(token);
    }

    public static void close(String userId) {
        NetSocket proxySocket = getProxySocket(userId);
        if (proxySocket != null) {
            proxySocket.close();
            offline(userId);
        }
    }

    public static void closeAll() {
        for (NetSocket proxySocket : userIdProxySocketMap.values()) {
            proxySocket.close();
        }
        userIdProxySocketMap.clear();
    }
}
