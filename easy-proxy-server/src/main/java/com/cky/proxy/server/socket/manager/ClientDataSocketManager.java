package com.cky.proxy.server.socket.manager;

import cn.hutool.core.map.BiMap;
import java.net.Socket;
import java.io.IOException;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ClientDataSocketManager {
    private final static BiMap<String, Socket> userIdDataSocketMap = new BiMap<>(new HashMap<>());
    private final static ConcurrentHashMap<String, CompletableFuture<Socket>> waitMap = new ConcurrentHashMap<>();

    public static Socket getDataSocket(String userId) {
        return userIdDataSocketMap.get(userId);
    }

    public static CompletableFuture<Socket> getWaitFuture(String userId) {
        return waitMap.computeIfAbsent(userId, k -> new CompletableFuture<>());
    }

    public static boolean isDataSocket(Socket socket) {
        return userIdDataSocketMap.getKey(socket) != null;
    }

    public static String getUserId(Socket socket) {
        return userIdDataSocketMap.getKey(socket);
    }

    public static void online(String userId, Socket dataSocket) {
        userIdDataSocketMap.put(userId, dataSocket);
        CompletableFuture<Socket> future = waitMap.remove(userId);
        if (future != null) {
            future.complete(dataSocket);
        }
    }

    public static void offline(String userId) {
        userIdDataSocketMap.remove(userId);
        CompletableFuture<Socket> future = waitMap.remove(userId);
        if (future != null) {
            future.cancel(true);
        }
    }

    public static void closeDataSocket(String userId) {
        Socket dataSocket = getDataSocket(userId);
        if (dataSocket != null) {
            try {
                dataSocket.close();
            } catch (IOException e) {
                // ignore
            }
            ClientDataSocketManager.offline(userId);
        }
    }
}
