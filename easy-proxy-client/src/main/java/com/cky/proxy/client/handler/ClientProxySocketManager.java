package com.cky.proxy.client.handler;

import com.cky.proxy.client.config.ConfigProperty;
import com.cky.proxy.client.config.ServerProperty;
import com.cky.proxy.client.context.DataSocketContext;
import com.cky.proxy.client.context.MngSocketContext;
import com.cky.proxy.client.context.ProxySocketContext;
import com.cky.proxy.client.util.CertDownloader;
import com.cky.proxy.common.domain.Message;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.PemTrustOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * client -> app 应用socket 业务
 */
@Slf4j
@RequiredArgsConstructor
public class ClientProxySocketManager {
    private final Vertx vertx;
    private final String userId;
    private final NetSocket proxySocket;

    public void init() {
        log.debug("EP>>ClientProxy>> Proxy socket {} create success", proxySocket.remoteAddress());
        ProxySocketContext.online(userId, proxySocket);
        handleRead();
        handleClose();
        // pause util data socket success
        proxySocket.pause();

        ServerProperty server = ConfigProperty.getInstance().getServer();
        log.debug("EP>>ClientProxy>> Create data socket");
        vertx.createNetClient()
                .connect(server.getPort(), server.getIp())
                .onSuccess(dataSocket -> {
                    ClientDataSocketManager manager = new ClientDataSocketManager(userId, dataSocket, proxySocket);
                    manager.init();
                }).onFailure(e -> {
                    log.error("EP>>ClientProxy>> Create data socket failed", e);
                    MngSocketContext.getMngSocket().write(Message.createDisConnectMsg(userId));
                });
    }

    private void handleClose() {
        proxySocket.closeHandler(v -> {
            log.debug("EP>>ClientProxy>> Socket closed");
            ProxySocketContext.offline(userId);
            DataSocketContext.close(userId);
            NetSocket mngSocket = MngSocketContext.getMngSocket();
            if (mngSocket != null) {
                mngSocket.write(Message.createDisConnectMsg(userId));
            } else {
                log.error("EP>>ClientProxy>> Mng socket is null");
            }
        });
    }

    private void handleRead() {
        proxySocket.handler(buffer -> {
            log.debug("EP>>ClientProxy>> Process read");
            NetSocket dataSocket = DataSocketContext.getDataSocket(userId);
            if (dataSocket != null) {
                log.debug("EP>>ClientProxy>> Send proxy data to data socket");
                dataSocket.write(Message.createDataMsg(userId, buffer.getBytes()));
            } else {
                proxySocket.close();
                log.error("EP>>ClientProxy>> Data socket is null");
            }
        });
    }
}
