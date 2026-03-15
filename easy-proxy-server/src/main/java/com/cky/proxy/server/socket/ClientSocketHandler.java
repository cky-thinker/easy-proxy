package com.cky.proxy.server.socket;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cky.proxy.common.consts.OnlineStatus;
import com.cky.proxy.common.domain.Message;
import com.cky.proxy.common.util.SocketUtil;
import com.cky.proxy.server.domain.entity.ProxyClient;
import com.cky.proxy.server.domain.entity.ProxyClientRule;
import com.cky.proxy.server.service.ProxyClientRuleService;
import com.cky.proxy.server.service.ProxyClientService;
import com.cky.proxy.server.socket.manager.ClientDataSocketManager;
import com.cky.proxy.server.socket.manager.ClientSocketManager;
import com.cky.proxy.server.socket.manager.RuleListenSocketManager;
import com.cky.proxy.server.socket.manager.TrafficStatisticManager;
import com.cky.proxy.server.util.BeanContext;
import com.cky.proxy.server.util.EventBusUtil;

import cn.hutool.core.util.StrUtil;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;

public class ClientSocketHandler implements Handler<NetSocket> {
    private static final Logger log = LoggerFactory.getLogger(ClientSocketHandler.class);
    private final ProxyClientService proxyClientService = BeanContext.getProxyClientService();
    private final ProxyClientRuleService proxyRuleService = BeanContext.getProxyClientRuleService();
    private final Vertx vertx;

    public ClientSocketHandler(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public void handle(NetSocket clientSocket) {
        handleRead(clientSocket);
        handleClose(clientSocket);
    }

    private void handleRead(NetSocket clientSocket) {
        Message.decodeMsg(clientSocket, msg -> {
            log.debug("EP>>ServerMng>> Read msg {}", msg.getType());
            switch (msg.getType()) {
                case Message.AUTH:
                    processAuth(msg, clientSocket);
                    break;
                case Message.CONNECT:
                    processConnect(msg, clientSocket);
                    break;
                case Message.DATA:
                    processData(msg, clientSocket);
                    break;
                case Message.DISCONNECT:
                    processDisconnect(msg);
                    break;
                default:
                    break;
            }
        });
    }

    private void handleClose(NetSocket socket) {
        socket.closeHandler(v -> {
            if (ClientDataSocketManager.isDataSocket(socket)) {
                log.info("EP>>Client>> Data socket closed {}", SocketUtil.getSocketName(socket));
                // remove related user sockets
                String userId = ClientDataSocketManager.getUserId(socket);
                ClientDataSocketManager.offline(userId);
                RuleListenSocketManager.userConnectionClose(userId);
            } else {
                log.info("EP>>Client>> Client socket closed {}", SocketUtil.getSocketName(socket));
                // remove all related user sockets and data sockets
                String token = ClientSocketManager.offline(socket);
                // Update offline status
                if (StrUtil.isBlank(token)) {
                    log.warn("EP>>ServerMng>> Client token is blank");
                    return;
                }

                try {
                    ProxyClient client = proxyClientService.updateClientStatus(token, "offline");
                    if (client != null) {
                        EventBusUtil.publish(EventBusUtil.SOCKET_CLIENT_OFFLINE, client.getId());
                    }
                } catch (Exception e) {
                    log.error("EP>>ServerMng>> Update offline status error", e);
                }

                List<ProxyClientRule> rules = proxyRuleService.getAllProxyClientRules(token, null, null);
                // TODO 根据规则关闭用户连接通道
                for (ProxyClientRule rule : rules) {
                    Set<String> userIds = RuleListenSocketManager.getOnlineUsers(rule.getId());
                    if (userIds != null) {
                        for (String userId : userIds) {
                            NetSocket dataSocket = ClientDataSocketManager.getDataSocket(userId);
                            if (dataSocket != null) {
                                dataSocket.close();
                            }
                            ClientDataSocketManager.closeDataSocket(userId);
                            NetSocket proxySocket = RuleListenSocketManager.getProxySocket(userId);
                            if (proxySocket != null) {
                                proxySocket.close();
                            }
                            RuleListenSocketManager.userConnectionClose(userId);
                        }
                    }
                }
            }
        });
    }

    // client connection auth, register if success
    private void processAuth(Message msg, NetSocket sMngSocket) {
        log.debug("EP>>ServerMng>> Process auth");
        String token = msg.getToken();

        try {
            ProxyClient client = proxyClientService.selectByToken(token);

            if (client == null) {
                log.warn("EP>>ServerMng>> Client not found for token: {}", token);
                sMngSocket.close();
                return;
            }

            if (!Boolean.TRUE.equals(client.getEnableFlag())) {
                log.warn("EP>>ServerMng>> Client {} is disabled", client.getName());
                sMngSocket.close();
                return;
            }

            // Update online status
            client = proxyClientService.updateClientStatus(token, OnlineStatus.online.name());
            if (client != null) {
                EventBusUtil.publish(EventBusUtil.SOCKET_CLIENT_ONLINE, client.getId());
            }
        } catch (Exception e) {
            log.error("EP>>ServerMng>> Process auth error", e);
            sMngSocket.close();
            return;
        }

        NetSocket existedMngSocket = ClientSocketManager.getClientSocket(token);
        if (existedMngSocket != null) {
            log.info("EP>>ServerMng>> Socket {} is connected, Can't connect again {}",
                    SocketUtil.getSocketName(existedMngSocket), SocketUtil.getSocketName(sMngSocket));
            sMngSocket.close();
        }
        ClientSocketManager.online(token, sMngSocket);
        log.debug("EP>>ServerMng>> Process auth success");
    }

    private void processConnect(Message msg, NetSocket dataSocket) {
        log.debug("EP>>ServerMng>> Process connect");
        String userId = msg.getToken();
        ClientDataSocketManager.online(userId, dataSocket);
        NetSocket userProxySocket = RuleListenSocketManager.getProxySocket(userId);
        if (userProxySocket != null) {
            userProxySocket.resume();
            log.debug("EP>>ServerMng>> Process connect success");
        } else {
            log.debug("EP>>ServerMng>> Process connect fail");
        }
    }

    private void processData(Message msg, NetSocket clientSocket) {
        log.debug("EP>>ServerMng>> Process data");
        String userId = msg.getToken();
        NetSocket userSocket = RuleListenSocketManager.getProxySocket(userId);
        if (userSocket != null) {
            log.debug("EP>>ServerMng>> Process data success");
            byte[] data = msg.getData();

            // Check bandwidth limit
            Integer ruleId = TrafficStatisticManager.getRuleId(userId);
            if (ruleId != null && TrafficStatisticManager.isRateExceeded(ruleId, data.length)) {
                log.debug("EP>>ServerMng>> Rate limit exceeded, pausing client socket");
                clientSocket.pause();
                vertx.setTimer(1000, id -> clientSocket.resume());
            }

            TrafficStatisticManager.addDownload(userId, data.length);
            userSocket.write(Buffer.buffer(data));
        } else {
            log.debug("EP>>ServerMng>> Process data fail");
        }
    }

    private void processDisconnect(Message msg) {
        log.debug("EP>>ServerMng>> Process disconnect");
        String userId = msg.getToken();
        ClientDataSocketManager.closeDataSocket(userId);
        RuleListenSocketManager.userConnectionClose(userId);
    }
}
