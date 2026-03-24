package com.cky.proxy.client.context;

import io.vertx.core.net.NetSocket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * userId -> proxySocket 应用socket 上下文
 */
public class ProxySocketContext {
    private final static Map<String, NetSocket> userIdProxySocketMap = new ConcurrentHashMap<>();

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
        NetSocket proxySocket = userIdProxySocketMap.remove(userId);
        if (proxySocket != null) {
            try {
                proxySocket.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    public static void closeAll() {
        for (NetSocket proxySocket : userIdProxySocketMap.values().toArray(new NetSocket[0])) {
            try {
                proxySocket.close();
            } catch (Exception e) {
                // ignore
            }
        }
        userIdProxySocketMap.clear();
    }
}
