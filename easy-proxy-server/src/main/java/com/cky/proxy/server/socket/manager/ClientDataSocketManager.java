package com.cky.proxy.server.socket.manager;

import cn.hutool.core.map.BiMap;
import io.vertx.core.net.NetSocket;

import java.util.HashMap;

public class ClientDataSocketManager {
    private final static BiMap<String, NetSocket> userIdDataSocketMap = new BiMap<>(new HashMap<>());

    public static NetSocket getDataSocket(String userId) {
        return userIdDataSocketMap.get(userId);
    }

    public static boolean isDataSocket(NetSocket socket) {
        return userIdDataSocketMap.getKey(socket) != null;
    }

    public static String getUserId(NetSocket socket) {
        return userIdDataSocketMap.getKey(socket);
    }

    public static void online(String userId, NetSocket dataSocket) {
        userIdDataSocketMap.put(userId, dataSocket);
    }

    public static void offline(String userId) {
        userIdDataSocketMap.remove(userId);
    }

    public static void closeDataSocket(String userId) {
        NetSocket dataSocket = getDataSocket(userId);
        if (dataSocket != null) {
            dataSocket.close();
            ClientDataSocketManager.offline(userId);
        }
    }
}
