package com.cky.proxy.server.socket;

import cn.hutool.core.util.IdUtil;
import com.cky.proxy.common.domain.Message;
import com.cky.proxy.server.domain.entity.ProxyClient;
import com.cky.proxy.server.domain.entity.ProxyClientRule;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class UserProxySocketHandler implements Handler<NetSocket> {
    private final ProxyClient proxyClientConfig;
    private final ProxyClientRule proxyRule;

    @Override
    public void handle(NetSocket userProxySocket) {
        NetSocket mngSocket = MngSocketManager.getMngSocket(proxyClientConfig.getToken());
        if (mngSocket == null) {
            log.debug("EP>>UserProxy>> Can't found mng socket {}:{}", proxyClientConfig.getName(), proxyRule.getName());
            userProxySocket.close();
            return;
        }

        String userId = String.valueOf(IdUtil.getSnowflakeNextId());
        UserSocketManager.online(proxyClientConfig.getToken(), userId, userProxySocket);
        // reuse after client data connection create success
        log.debug("EP>>UserProxy>> User connected, Send connect msg");
        userProxySocket.pause();
        mngSocket.write(Message.createConnectMsg(userId, proxyRule.getClientAddress()));
        userProxySocket.handler(processRead(userProxySocket, userId));
        userProxySocket.closeHandler(processClose(userId));
    }

    private Handler<Buffer> processRead(NetSocket userProxySocket, String userId) {
        return buffer -> {
            log.debug("EP>>UserProxy>> User socket read");
            NetSocket dataSocket = DataSocketManager.getDataSocket(userId);
            if (dataSocket == null) {
                log.error("EP>>UserProxy>> Can't found data socket by userId {}", userId);
                userProxySocket.close();
                return;
            }
            byte[] data = buffer.getBytes();
            dataSocket.write(Message.createDataMsg(userId, data));
        };
    }

    private Handler<Void> processClose(String userId) {
        return v -> {
            log.debug("EP>>UserProxy>> User proxy socket closed");
            NetSocket mngSocket = MngSocketManager.getMngSocket(proxyClientConfig.getToken());
            if (mngSocket != null) {
                mngSocket.write(Message.createDisConnectMsg(userId));
                UserSocketManager.offline(userId);
            } else {
                log.debug("EP>>UserProxy>> Mng proxy is null");
            }
        };
    }
}
