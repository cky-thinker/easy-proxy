package com.cky.proxy.client.context;

import java.net.Socket;

/**
 * client -> server mngSocket 管理socket 上下文
 */
public class MngSocketContext {
    private static Socket mngSocket = null;

    public static void offline() {
        mngSocket = null;
    }

    public static void online(Socket socket) {
        mngSocket = socket;
    }

    public static Socket getMngSocket() {
        return mngSocket;
    }

}
