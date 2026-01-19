package com.cky.proxy.client.handler;

import com.cky.proxy.client.context.DataSocketContext;
import com.cky.proxy.client.context.MngSocketContext;
import com.cky.proxy.client.context.ProxySocketContext;
import com.cky.proxy.client.domain.Address;
import com.cky.proxy.client.util.CertDownloader;
import com.cky.proxy.common.domain.Message;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.PemTrustOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * client -> server 管理socket 业务
 */
@Slf4j
@RequiredArgsConstructor
public class ClientMngSocketManager {
    private final Vertx vertx;
    private final String token;
    private final NetSocket mngSocket;
    private Handler<Void> closeHandler = null;

    public void init() {
        MngSocketContext.online(mngSocket);
        sendAuth();
        handleRead();
        handleClose();
    }

    public void sendAuth() {
        Buffer buffer = Message.createAuthMsg(token);
        mngSocket.write(buffer);
    }

    public void handleRead() {
        Message.decodeMsg(mngSocket, msg -> {
            log.debug("EP>>ClientMng>> read");
            switch (msg.getType()) {
                // 连接
                case Message.CONNECT:
                    processConnect(msg);
                    break;
                // 断连
                case Message.DISCONNECT:
                    processDisconnect(msg);
                    break;
                default:
                    break;
            }
        });
    }

    private void processConnect(Message msg) {
        log.debug("EP>>ClientMng>> connect");
        String userId = msg.getToken();
        String proxyAddress = new String(msg.getData(), StandardCharsets.UTF_8);
        Address address = Address.parse(proxyAddress);
        if (!address.isValid()) {
            log.error("EP>>ClientMng>> Proxy address '{}' is not valid", proxyAddress);
            mngSocket.write(Message.createDisConnectMsg(userId));
            return;
        }

        log.debug("EP>>ClientMng>> Create app proxy socket");
        NetClientOptions options = new NetClientOptions()
                .setSsl(true)
                .setHostnameVerificationAlgorithm("")
                .setTrustOptions(new PemTrustOptions().addCertPath(CertDownloader.getPemCertPath()));
        vertx.createNetClient(options)
                .connect(address.getPort(), address.getIp())
                .onSuccess(proxySocket -> {
                    new ClientProxySocketManager(vertx, userId, proxySocket).init();
                }).onFailure(e -> {
                    log.error("EP>>ClientMng>> Proxy socket '{}' connect fail, {}", proxyAddress, e.getMessage());
                    mngSocket.write(Message.createDisConnectMsg(userId));
                });
    }

    private void processDisconnect(Message msg) {
        log.debug("EP>>ClientMng>> Disconnect");
        String userId = msg.getToken();
        ProxySocketContext.close(userId);
        DataSocketContext.close(userId);
    }

    private void handleClose() {
        mngSocket.closeHandler(v -> {
            log.info("EP>>ClientMng>> Mng socket closed");
            MngSocketContext.offline();
            DataSocketContext.closeAll();
            ProxySocketContext.closeAll();
            if (closeHandler != null) {
                closeHandler.handle(null);
            }
        });
    }

    public void closeHandler(Handler<Void> closeHandler) {
        this.closeHandler = closeHandler;
    }
}
