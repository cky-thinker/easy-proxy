package com.cky.proxy.server.util;

import io.reactivex.rxjava3.annotations.Nullable;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;

/**
 * 对Vertx事件总线的封装，管理事件类型与业务事件的分发订阅
 */
public class EventBusUtil {
    // 数据库 相关事件
    // 客户端禁用
    public static final String DB_CLIENT_DISABLE = "db.client.disable";
    // 客户端删除
    public static final String DB_CLIENT_DELETE = "db.client.delete";
    // 客户端更新
    public static final String DB_CLIENT_UPDATE = "db.client.update";
    // 客户端启用
    public static final String DB_CLIENT_ENABLE = "db.client.enable";
    // 规则禁用
    public static final String DB_RULE_DISABLE = "db.rule.disable";
    // 规则删除
    public static final String DB_RULE_DELETE = "db.rule.delete";
    // 规则更新
    public static final String DB_RULE_UPDATE = "db.rule.update";
    // 规则启用
    public static final String DB_RULE_ENABLE = "db.rule.enable";

    // Socket 相关事件
    // 客户端离线
    public static final String SOCKET_CLIENT_OFFLINE = "socket.client.offline";
    // 客户端在线
    public static final String SOCKET_CLIENT_ONLINE = "socket.client.online";

    private static Vertx vertx;

    public static void setup(Vertx vertx) {
        EventBusUtil.vertx = vertx;
    }

    public static void publish(String var1, @Nullable Object var2) {
        vertx.eventBus().publish(var1, var2);
    }

    public static <T> void subscribe(String var1, Handler<Message<T>> var2) {
        vertx.eventBus().consumer(var1, var2);
    }
}
