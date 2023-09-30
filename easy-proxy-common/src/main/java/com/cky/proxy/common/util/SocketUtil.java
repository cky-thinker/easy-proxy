package com.cky.proxy.common.util;

import io.vertx.core.net.NetSocket;

public class SocketUtil {
    public static String getSocketName(NetSocket socket) {
        return socket.localAddress() + ":" + socket.remoteAddress();
    }
}
