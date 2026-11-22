package com.cky.proxy.server.socket.manager;

import java.net.Socket;
import java.io.IOException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ClientDataSocketManager {
    private static final ConcurrentMap<String, Socket> userIdDataSocketMap = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Socket, String> dataSocketUserMap = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, CompletableFuture<Socket>> waitMap = new ConcurrentHashMap<>();

    private ClientDataSocketManager() {
    }

    public static Socket getDataSocket(String userId) {
        return userId == null ? null : userIdDataSocketMap.get(userId);
    }

    public static CompletableFuture<Socket> getWaitFuture(String userId) {
        return waitMap.computeIfAbsent(userId, ignored -> new CompletableFuture<>());
    }

    public static boolean isDataSocket(Socket socket) {
        return socket != null && dataSocketUserMap.containsKey(socket);
    }

    public static String getUserId(Socket socket) {
        return socket == null ? null : dataSocketUserMap.get(socket);
    }

    /**
     * Atomically registers a data socket and releases the matching waiter.
     *
     * @return true when the socket became the active data connection for the user
     */
    public static boolean online(String userId, Socket dataSocket) {
        if (userId == null || dataSocket == null) {
            return false;
        }
        Socket existingSocket = userIdDataSocketMap.putIfAbsent(userId, dataSocket);
        if (existingSocket != null) {
            return false;
        }

        String existingUserId = dataSocketUserMap.putIfAbsent(dataSocket, userId);
        if (existingUserId != null && !existingUserId.equals(userId)) {
            userIdDataSocketMap.remove(userId, dataSocket);
            return false;
        }

        CompletableFuture<Socket> future = waitMap.remove(userId);
        if (future != null) {
            future.complete(dataSocket);
        }
        return true;
    }

    public static void offline(String userId) {
        if (userId == null) {
            return;
        }
        Socket dataSocket = userIdDataSocketMap.remove(userId);
        if (dataSocket != null) {
            dataSocketUserMap.remove(dataSocket, userId);
        }
        cancelWaiter(userId);
    }

    /**
     * Removes a specific data socket without disturbing a newer mapping for the same user.
     */
    public static void offline(String userId, Socket dataSocket) {
        if (userId == null || dataSocket == null) {
            return;
        }
        boolean removed = userIdDataSocketMap.remove(userId, dataSocket);
        dataSocketUserMap.remove(dataSocket, userId);
        if (removed) {
            cancelWaiter(userId);
        }
    }

    public static void closeDataSocket(String userId) {
        Socket dataSocket = userIdDataSocketMap.get(userId);
        if (dataSocket != null) {
            try {
                dataSocket.close();
            } catch (IOException e) {
                // ignore
            }
        }
        offline(userId);
    }

    private static void cancelWaiter(String userId) {
        CompletableFuture<Socket> future = waitMap.remove(userId);
        if (future != null) {
            future.cancel(true);
        }
    }
}