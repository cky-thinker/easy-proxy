package com.cky.proxy.server.socket.manager;

import cn.hutool.core.map.BiMap;
import java.net.Socket;

import java.util.HashMap;

public class ClientSocketManager {
    private final static BiMap<String, Socket> clientSocketMap = new BiMap<>(new HashMap<>());

    public static Socket getClientSocket(String token) {
        return clientSocketMap.get(token);
    }

    public static void online(String token, Socket clientSocket) {
        clientSocketMap.put(token, clientSocket);
    }

    public static String offline(Socket clientSocket) {
        String token = clientSocketMap.getKey(clientSocket);
        clientSocketMap.remove(token);
        return token;
    }
}
