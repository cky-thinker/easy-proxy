package com.cky.proxy.server.socket;

import cn.hutool.core.map.BiMap;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class UserSocketManager {
    // 服务端用户连接监听socket
    private final static Map<Integer, NetServer> userListenSocketMap = new ConcurrentHashMap<>();
    // 服务端用户连接socket
    private final static BiMap<String, NetSocket> userIdUserSocketMap = new BiMap<>(new HashMap<>());
    private final static HashMap<String, Set<String>> tokenOnlineUsersMap = new HashMap<>();
    private final static HashMap<String, String> onlineUserTokenMap = new HashMap<>();


    // 规则监听socket
    public static NetServer getRuleListenSocket(Integer ruleId) {
        return userListenSocketMap.get(ruleId);
    }

    // 规则监听socket
    public static NetServer removeRuleListenSocket(Integer ruleId) {
        return userListenSocketMap.remove(ruleId);
    }

    // 规则监听socket
    public static NetServer addRuleListenSocket(Integer ruleId, NetServer netServer) {
        return userListenSocketMap.put(ruleId, netServer);
    }

    public static NetSocket getProxySocket(String userId) {
        return userIdUserSocketMap.get(userId);
    }

    public static void online(String token, String userId, NetSocket userProxySocket) {
        userIdUserSocketMap.put(userId, userProxySocket);
        tokenOnlineUsersMap.computeIfAbsent(token, t -> new ConcurrentHashSet<>());
        tokenOnlineUsersMap.get(token).add(userId);
        onlineUserTokenMap.put(userId, token);
    }

    public static void offline(String userId) {
        userIdUserSocketMap.remove(userId);
        String token = onlineUserTokenMap.remove(userId);
        tokenOnlineUsersMap.get(token).remove(userId);
    }

    public static void closeUserSocket(String userId) {
        NetSocket userSocket = UserSocketManager.getProxySocket(userId);
        if (userSocket != null) {
            userSocket.close();
            UserSocketManager.offline(userId);
        }
    }

    public static Set<String> getOnlineUsers(String token) {
        return tokenOnlineUsersMap.get(token);
    }
}
