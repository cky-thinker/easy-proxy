package com.cky.proxy.server.util;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 自定义事件总线，管理事件类型与业务事件的分发订阅
 */
public class EventBusUtil {
    // 数据库 相关事件
    // 客户端禁用
    public static final String DB_CLIENT_DISABLE = "db.client.disable";
    // 客户端删除
    public static final String DB_CLIENT_DELETE = "db.client.delete";
    // 客户端更新
    public static final String DB_CLIENT_UPDATE = "db.client.update";
    // 客户端新增
    public static final String DB_CLIENT_ADD = "db.client.add";
    // 客户端启用
    public static final String DB_CLIENT_ENABLE = "db.client.enable";
    // 规则禁用
    public static final String DB_RULE_DISABLE = "db.rule.disable";
    // 规则删除
    public static final String DB_RULE_DELETE = "db.rule.delete";
    // 规则更新
    public static final String DB_RULE_UPDATE = "db.rule.update";
    // 规则新增
    public static final String DB_RULE_ADD = "db.rule.add";
    // 规则启用
    public static final String DB_RULE_ENABLE = "db.rule.enable";

    // Socket 相关事件
    // 客户端离线
    public static final String SOCKET_CLIENT_OFFLINE = "socket.client.offline";
    // 客户端在线
    public static final String SOCKET_CLIENT_ONLINE = "socket.client.online";

    private static final Map<String, List<Consumer<Object>>> subscribers = new ConcurrentHashMap<>();

    public static void init() {
        // initialization logic if any
    }

    public static void publish(String topic, Object message) {
        List<Consumer<Object>> list = subscribers.get(topic);
        if (list != null) {
            for (Consumer<Object> consumer : list) {
                Thread.ofVirtual().start(() -> consumer.accept(message));
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> void subscribe(String topic, Consumer<T> handler) {
        subscribers.computeIfAbsent(topic, k -> new CopyOnWriteArrayList<>())
                .add(obj -> handler.accept((T) obj));
    }
}
