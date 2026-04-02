package com.cky.proxy.server.socket;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cky.proxy.common.domain.Message;
import com.cky.proxy.server.domain.entity.ProxyClient;
import com.cky.proxy.server.domain.entity.ProxyClientRule;
import com.cky.proxy.server.socket.manager.ClientDataSocketManager;
import com.cky.proxy.server.socket.manager.ClientSocketManager;
import com.cky.proxy.server.socket.manager.RuleListenSocketManager;
import com.cky.proxy.server.socket.manager.TrafficStatisticManager;
import com.cky.proxy.server.util.TokenBucket;

import cn.hutool.core.util.IdUtil;

public class UserProxySocketHandler implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(UserProxySocketHandler.class);
    private final ProxyClient proxyClientConfig;
    private final ProxyClientRule proxyRule;
    private final Socket userConnection;

    public UserProxySocketHandler(ProxyClient proxyClientConfig, ProxyClientRule proxyRule, Socket userConnection) {
        this.proxyClientConfig = proxyClientConfig;
        this.proxyRule = proxyRule;
        this.userConnection = userConnection;
    }

    @Override
    public void run() {
        Socket clientSocket = ClientSocketManager.getClientSocket(proxyClientConfig.getToken());
        if (clientSocket == null) {
            log.debug("EP>>UserProxy>> Can't found client socket {}:{}", proxyClientConfig.getName(), proxyRule.getName());
            closeUserConnection();
            return;
        }

        // Check connection limit
        if (proxyRule.getLimitConn() != null && proxyRule.getLimitConn() > 0) {
            long activeConns = TrafficStatisticManager.getActiveConnections(proxyRule.getId());
            if (activeConns >= proxyRule.getLimitConn()) {
                log.warn("EP>>UserProxy>> Connection limit exceeded for rule {}: {}/{}", proxyRule.getName(),
                        activeConns, proxyRule.getLimitConn());
                closeUserConnection();
                return;
            }
        }

        String userId = String.valueOf(IdUtil.getSnowflakeNextId());
        RuleListenSocketManager.userConnectionOnline(proxyRule.getId(), userId, userConnection);
        TrafficStatisticManager.addConnection(userId, proxyClientConfig.getId(), proxyRule.getId());

        log.debug("EP>>UserProxy>> User connected, Send connect msg");
        try {
            CompletableFuture<Socket> dataSocketFuture = ClientDataSocketManager.getWaitFuture(userId);
            Message.createConnectMsg(userId, proxyRule.getClientAddress()).writeTo(clientSocket);

            // Wait for data socket connection from client (timeout e.g., 10 seconds)
            Socket dataSocket = dataSocketFuture.get(10, TimeUnit.SECONDS);

            if (dataSocket == null) {
                log.error("EP>>UserProxy>> Data socket is null for userId {}", userId);
                closeUserConnectionGracefully(userId);
                return;
            }

            // Start reading from user socket and writing to data socket
            byte[] buffer = new byte[8192];
            InputStream in = userConnection.getInputStream();
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                log.debug("EP>>UserProxy>> User socket read");
                byte[] data = new byte[bytesRead];
                System.arraycopy(buffer, 0, data, 0, bytesRead);

                Integer ruleId = proxyRule.getId();
                if (ruleId != null && TrafficStatisticManager.hasUpRateLimit(ruleId)) {
                    TokenBucket upBucket = TrafficStatisticManager.getUpRateLimitBucket(ruleId);
                    if (upBucket != null) {
                        upBucket.acquire(bytesRead); // block until permits are available
                    }
                }

                TrafficStatisticManager.addUpload(userId, bytesRead);
                Message.createDataMsg(userId, data).writeTo(dataSocket);
            }

        } catch (TimeoutException e) {
            log.error(e.getMessage(), e);
            log.error("EP>>UserProxy>> Wait for data socket timeout userId {}", userId);
        } catch (Exception e) {
            log.error("EP>>UserProxy>> User proxy socket error: {}", e.getMessage());
        } finally {
            processClose(userId);
        }
    }

    private void processClose(String userId) {
        log.debug("EP>>UserProxy>> User proxy socket closed");
        Socket clientSocket = ClientSocketManager.getClientSocket(proxyClientConfig.getToken());
        if (clientSocket != null) {
            try {
                Message.createDisConnectMsg(userId).writeTo(clientSocket);
            } catch (IOException e) {
                // ignore
            }
        } else {
            log.debug("EP>>UserProxy>> Mng proxy is null");
        }
        RuleListenSocketManager.userConnectionOffline(userId);
        TrafficStatisticManager.removeConnection(userId);
        closeUserConnection();
    }

    private void closeUserConnection() {
        try {
            if (userConnection != null) {
                userConnection.close();
            }
        } catch (IOException e) {
            // ignore
        }
    }

    private void closeUserConnectionGracefully(String userId) {
        processClose(userId);
    }
}