package com.cky.proxy.server.socket;

import cn.hutool.core.util.IdUtil;
import com.cky.proxy.common.domain.Message;
import com.cky.proxy.server.domain.entity.ProxyClient;
import com.cky.proxy.server.domain.entity.ProxyClientRule;
import com.cky.proxy.server.socket.manager.ClientDataSocketManager;
import com.cky.proxy.server.socket.manager.ClientSocketManager;
import com.cky.proxy.server.socket.manager.RuleListenSocketManager;
import com.cky.proxy.server.socket.manager.TrafficStatisticManager;
import com.cky.proxy.server.util.TokenBucket;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserProxySocketHandler implements Handler<NetSocket> {
    private static final Logger log = LoggerFactory.getLogger(UserProxySocketHandler.class);
    private final ProxyClient proxyClientConfig;
    private final ProxyClientRule proxyRule;
    private final Vertx vertx;

    public UserProxySocketHandler(ProxyClient proxyClientConfig, ProxyClientRule proxyRule, Vertx vertx) {
        this.proxyClientConfig = proxyClientConfig;
        this.proxyRule = proxyRule;
        this.vertx = vertx;
    }

    @Override
    public void handle(NetSocket userConnection) {
        NetSocket clientSocket = ClientSocketManager.getClientSocket(proxyClientConfig.getToken());
        if (clientSocket == null) {
            log.debug("EP>>UserProxy>> Can't found client socket {}:{}", proxyClientConfig.getName(),
                    proxyRule.getName());
            userConnection.close();
            return;
        }

        // Check connection limit
        if (proxyRule.getLimitConn() != null && proxyRule.getLimitConn() > 0) {
            long activeConns = TrafficStatisticManager.getActiveConnections(proxyRule.getId());
            if (activeConns >= proxyRule.getLimitConn()) {
                log.warn("EP>>UserProxy>> Connection limit exceeded for rule {}: {}/{}", proxyRule.getName(),
                        activeConns, proxyRule.getLimitConn());
                userConnection.close();
                return;
            }
        }

        String userId = String.valueOf(IdUtil.getSnowflakeNextId());
        RuleListenSocketManager.userConnectionOnline(proxyRule.getId(), userId, userConnection);
        TrafficStatisticManager.addConnection(userId, proxyClientConfig.getId(), proxyRule.getId());
        // reuse after client data connection create success
        log.debug("EP>>UserProxy>> User connected, Send connect msg");
        userConnection.pause();
        clientSocket.write(Message.createConnectMsg(userId, proxyRule.getClientAddress()));
        userConnection.handler(processRead(userConnection, userId));
        userConnection.closeHandler(processClose(userId));
    }

    private Handler<Buffer> processRead(NetSocket userProxySocket, String userId) {
        return buffer -> {
            log.debug("EP>>UserProxy>> User socket read");
            NetSocket dataSocket = ClientDataSocketManager.getDataSocket(userId);
            if (dataSocket == null) {
                log.error("EP>>UserProxy>> Can't found data socket by userId {}", userId);
                userProxySocket.close();
                return;
            }
            byte[] data = buffer.getBytes();
            // Check bandwidth limit
            Integer ruleId = proxyRule.getId();
            if (ruleId != null && TrafficStatisticManager.hasUpRateLimit(ruleId)) {
                // 获取上行令牌桶
                TokenBucket upBucket = TrafficStatisticManager.getUpRateLimitBucket(ruleId);
                // 限速分片写入
                upBucket.writeWithLimit(userProxySocket, data, chunk -> {
                    TrafficStatisticManager.addUpload(userId, chunk.length);
                    dataSocket.write(Message.createDataMsg(userId, chunk));
                });
            } else {
                TrafficStatisticManager.addUpload(userId, data.length);
                dataSocket.write(Message.createDataMsg(userId, data));
            }
        };
    }

    private Handler<Void> processClose(String userId) {
        return v -> {
            log.debug("EP>>UserProxy>> User proxy socket closed");
            NetSocket clientSocket = ClientSocketManager.getClientSocket(proxyClientConfig.getToken());
            if (clientSocket != null) {
                Integer ruleId = proxyRule.getId();
                if (ruleId != null && TrafficStatisticManager.hasUpRateLimit(ruleId)) {
                    TokenBucket upBucket = TrafficStatisticManager.getUpRateLimitBucket(ruleId);
                    if (upBucket != null) {
                        upBucket.acquire(0, ok -> {
                            clientSocket.write(Message.createDisConnectMsg(userId));
                            RuleListenSocketManager.userConnectionOffline(userId);
                            TrafficStatisticManager.removeConnection(userId);
                        });
                        return;
                    }
                }
                clientSocket.write(Message.createDisConnectMsg(userId));
                RuleListenSocketManager.userConnectionOffline(userId);
                TrafficStatisticManager.removeConnection(userId);
            } else {
                log.debug("EP>>UserProxy>> Mng proxy is null");
            }
        };
    }
}
