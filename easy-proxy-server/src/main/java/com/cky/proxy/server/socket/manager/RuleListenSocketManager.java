package com.cky.proxy.server.socket.manager;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;

/**
 * 规则监听socket管理类
 */
public class RuleListenSocketManager {
    // 服务端规则监听socket
    private final static Map<Integer, NetServer> ruleListenSocketMap = new ConcurrentHashMap<>();
    // 服务端用户连接socket
    private final static Map<String, NetSocket> userIdUserSocketMap = new ConcurrentHashMap<>();
    private final static Map<String, String> userIdRuleIdMap = new ConcurrentHashMap<>();

    /**
     * 根据规则ID获取用户连接监听socket
     *
     * @param ruleId 规则ID
     * @return 用户连接监听socket
     */
    public static NetServer getRuleListenSocket(Integer ruleId) {
        return ruleListenSocketMap.get(ruleId);
    }

    /**
     * 根据规则ID移除用户连接监听socket
     *
     * @param ruleId 规则ID
     * @return 用户连接监听socket
     */
    public static NetServer removeRuleListenSocket(Integer ruleId) {
        return ruleListenSocketMap.remove(ruleId);
    }

    /**
     * 根据规则ID添加用户连接监听socket
     *
     * @param ruleId    规则ID
     * @param netServer 用户连接监听socket
     * @return 用户连接监听socket
     */
    public static NetServer addRuleListenSocket(Integer ruleId, NetServer netServer) {
        return ruleListenSocketMap.put(ruleId, netServer);
    }

    public static NetSocket getProxySocket(String userId) {
        return userIdUserSocketMap.get(userId);
    }

    public static void userConnectionOnline(Integer ruleId, String userId, NetSocket userProxySocket) {
        userIdUserSocketMap.put(userId, userProxySocket);
        userIdRuleIdMap.put(userId, ruleId.toString());
    }

    public static void userConnectionOffline(String userId) {
        userIdUserSocketMap.remove(userId);
        userIdRuleIdMap.remove(userId);
    }

    public static void userConnectionClose(String userId) {
        NetSocket userSocket = getProxySocket(userId);
        if (userSocket != null) {
            userSocket.close();
            userConnectionOffline(userId);
        }
    }

    public static Set<String> getOnlineUsers(Integer ruleId) {
        String ruleIdStr = ruleId.toString();
        return userIdRuleIdMap.entrySet().stream()
                .filter(entry -> entry.getValue().equals(ruleIdStr))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
}
