package com.cky.proxy.server.socket.manager;

import cn.hutool.core.map.BiMap;
import io.vertx.core.net.NetSocket;

import java.util.HashMap;

public class ClientSocketManager {
    private final static BiMap<String, NetSocket> clientSocketMap = new BiMap<>(new HashMap<>());

    public static NetSocket getClientSocket(String token) {
        return clientSocketMap.get(token);
    }

    public static void online(String token, NetSocket clientSocket) {
        clientSocketMap.put(token, clientSocket);
    }

    public static String offline(NetSocket clientSocket) {
        String token = clientSocketMap.getKey(clientSocket);
        clientSocketMap.remove(token);
        return token;
    }
}
