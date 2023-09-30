package com.cky.proxy.client.context;

import io.vertx.core.net.NetSocket;

public class MngSocketContext {
    private static NetSocket mngSocket = null;

    public static void offline() {
        mngSocket = null;
    }

    public static void online(NetSocket socket) {
        mngSocket = socket;
    }

    public static NetSocket getMngSocket() {
        return mngSocket;
    }

}
