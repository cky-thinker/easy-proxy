package com.cky.proxy.server.socket.manager;

import cn.hutool.core.map.BiMap;
import io.vertx.core.net.NetSocket;

import java.util.HashMap;

public class MngSocketManager {
    private final static BiMap<String, NetSocket> tokenMngSocketMap = new BiMap<>(new HashMap<>());

    public static NetSocket getMngSocket(String token) {
        return tokenMngSocketMap.get(token);
    }

    public static void online(String token, NetSocket mngSocket) {
        tokenMngSocketMap.put(token, mngSocket);
    }

    public static String offline(NetSocket mngSocket) {
        String token = tokenMngSocketMap.getKey(mngSocket);
        tokenMngSocketMap.remove(token);
        return token;
    }
}
