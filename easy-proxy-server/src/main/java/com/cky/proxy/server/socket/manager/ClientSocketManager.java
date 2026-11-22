package com.cky.proxy.server.socket.manager;

import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ClientSocketManager {
    private static final ConcurrentMap<String, Socket> tokenSocketMap = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Socket, String> socketTokenMap = new ConcurrentHashMap<>();

    private ClientSocketManager() {
    }

    public static Socket getClientSocket(String token) {
        return token == null ? null : tokenSocketMap.get(token);
    }

    /**
     * Atomically registers a management socket for a token.
     *
     * @return true when the socket became the active management connection
     */
    public static boolean online(String token, Socket clientSocket) {
        if (token == null || clientSocket == null) {
            return false;
        }
        Socket existingSocket = tokenSocketMap.putIfAbsent(token, clientSocket);
        if (existingSocket != null) {
            return false;
        }

        String existingToken = socketTokenMap.putIfAbsent(clientSocket, token);
        if (existingToken != null && !existingToken.equals(token)) {
            tokenSocketMap.remove(token, clientSocket);
            return false;
        }
        return true;
    }

    /**
     * Removes the socket only when it is still the active socket for its token.
     */
    public static String offline(Socket clientSocket) {
        if (clientSocket == null) {
            return null;
        }
        String token = socketTokenMap.remove(clientSocket);
        if (token != null) {
            tokenSocketMap.remove(token, clientSocket);
        }
        return token;
    }
}
