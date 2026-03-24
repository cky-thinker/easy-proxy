package com.cky.proxy.server.socket.manager;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;

/**
 * 规则监听socket管理类
 */
public class RuleListenSocketManager {
    // 服务端规则监听socket
    private final static Map<Integer, ServerSocket> ruleListenSocketMap = new ConcurrentHashMap<>();
    // 服务端用户连接socket
    private final static Map<String, Socket> userIdUserSocketMap = new ConcurrentHashMap<>();
    private final static Map<String, String> userIdRuleIdMap = new ConcurrentHashMap<>();

    /**
     * 根据规则ID获取用户连接监听socket
     *
     * @param ruleId 规则ID
     * @return 用户连接监听socket
     */
    public static ServerSocket getRuleListenSocket(Integer ruleId) {
        return ruleListenSocketMap.get(ruleId);
    }

    /**
     * 根据规则ID移除用户连接监听socket
     *
     * @param ruleId 规则ID
     * @return 用户连接监听socket
     */
    public static ServerSocket removeRuleListenSocket(Integer ruleId) {
        return ruleListenSocketMap.remove(ruleId);
    }

    /**
     * 根据规则ID添加用户连接监听socket
     *
     * @param ruleId    规则ID
     * @param netServer 用户连接监听socket
     * @return 用户连接监听socket
     */
    public static ServerSocket addRuleListenSocket(Integer ruleId, ServerSocket netServer) {
        return ruleListenSocketMap.put(ruleId, netServer);
    }

    public static Socket getProxySocket(String userId) {
        return userIdUserSocketMap.get(userId);
    }

    public static void userConnectionOnline(Integer ruleId, String userId, Socket userProxySocket) {
        userIdUserSocketMap.put(userId, userProxySocket);
        userIdRuleIdMap.put(userId, ruleId.toString());
    }

    public static void userConnectionOffline(String userId) {
        userIdUserSocketMap.remove(userId);
        userIdRuleIdMap.remove(userId);
    }

    public static void userConnectionClose(String userId) {
        Socket userSocket = getProxySocket(userId);
        if (userSocket != null) {
            try {
                userSocket.close();
            } catch (IOException e) {
                // ignore
            }
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
