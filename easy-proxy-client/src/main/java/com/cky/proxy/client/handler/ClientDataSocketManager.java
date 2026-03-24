package com.cky.proxy.client.handler;

import com.cky.proxy.client.context.DataSocketContext;
import com.cky.proxy.client.context.ProxySocketContext;
import com.cky.proxy.common.domain.Message;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * client -> server 数据socket 业务
 */
@Slf4j
@RequiredArgsConstructor
public class ClientDataSocketManager {
    private final String userId;
    private final NetSocket dataSocket;
    private final NetSocket proxySocket;

    public void init() {
        log.debug("EP>>ClientData>> Create data socket success");
        DataSocketContext.online(userId, dataSocket);
        dataSocket.write(Message.createConnectMsg(userId));
        handleRead();
        handleClose();
        handleException();
        proxySocket.resume();
    }

    private void handleException() {
        dataSocket.exceptionHandler(t -> {
            log.error("EP>>ClientData>> Data socket error: {}", t.getMessage());
            dataSocket.close();
        });
    }

    private void handleRead() {
        Message.decodeMsg(dataSocket, msg -> {
            log.debug("EP>>ClientData>> Read data {}", msg.getType());
            switch (msg.getType()) {
                // 数据传输
                case Message.DATA:
                    processData(msg);
                    break;
                default:
                    break;
            }
        });
    }

    private void handleClose() {
        dataSocket.closeHandler(v -> {
            log.debug("EP>>ClientData>> Socket closed");
            DataSocketContext.offline(userId);
            ProxySocketContext.close(userId);
        });
    }

    private void processData(Message msg) {
        log.debug("EP>>ClientData>> Send data to proxy socket");
        String userId = msg.getToken();
        NetSocket proxySocket = ProxySocketContext.getProxySocket(userId);
        if (proxySocket != null) {
            proxySocket.write(Buffer.buffer(msg.getData()));
        } else {
            log.error("EP>>ClientData>> Proxy socket is null");
            dataSocket.close();
        }
    }
}
