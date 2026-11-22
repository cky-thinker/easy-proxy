package com.cky.proxy.server.socket.manager;

import java.io.IOException;
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
     * <p>A reconnecting client may present a new authenticated socket while the
     * server still holds a stale socket for the same token. The newest socket
     * becomes active and the stale socket is closed so a temporary network or
     * process failure cannot permanently lock the client out.</p>
     *
     * @return true when the socket became the active management connection
     */
    public static boolean online(String token, Socket clientSocket) {
        if (token == null || clientSocket == null) {
            return false;
        }

        while (true) {
            Socket existingSocket = tokenSocketMap.get(token);
            if (existingSocket == null) {
                if (tokenSocketMap.putIfAbsent(token, clientSocket) == null) {
                    break;
                }
                continue;
            }
            if (existingSocket == clientSocket) {
                return true;
            }
            if (tokenSocketMap.replace(token, existingSocket, clientSocket)) {
                socketTokenMap.put(clientSocket, token);
                socketTokenMap.remove(existingSocket, token);
                closeQuietly(existingSocket);
                return true;
            }
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
     *
     * @return the token only when this call removed the active mapping
     */
    public static String offline(Socket clientSocket) {
        if (clientSocket == null) {
            return null;
        }
        String token = socketTokenMap.remove(clientSocket);
        if (token != null && tokenSocketMap.remove(token, clientSocket)) {
            return token;
        }
        return null;
    }

    private static void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
            // Expected when replacing an already-closed stale connection.
        }
    }
}