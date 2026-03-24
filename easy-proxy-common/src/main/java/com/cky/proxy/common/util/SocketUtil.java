package com.cky.proxy.common.util;

import java.net.Socket;

public class SocketUtil {
    public static String getSocketName(Socket socket) {
        return socket.getLocalSocketAddress() + ":" + socket.getRemoteSocketAddress();
    }
}
