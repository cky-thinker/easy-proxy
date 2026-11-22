package com.cky.proxy.server.socket;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.cky.proxy.server.util.TokenBucket;

import cn.hutool.core.util.StrUtil;

public class ClientSocketHandler implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(ClientSocketHandler.class);
    private final ProxyClientService proxyClientService = BeanContext.getProxyClientService();
    private final ProxyClientRuleService proxyRuleService = BeanContext.getProxyClientRuleService();

    private final Socket clientSocket;

    public ClientSocketHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            clientSocket.setTcpNoDelay(true);
            DataInputStream in = new DataInputStream(clientSocket.getInputStream());
            while (true) {
                Message msg = Message.readMsg(in);
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
            }
        } catch (IOException e) {
            log.debug("EP>>Client>> Client socket closed: {}", e.getMessage());
        } finally {
            handleClose(clientSocket);
        }
    }

    private void handleClose(Socket socket) {
        if (ClientDataSocketManager.isDataSocket(socket)) {
            log.info("EP>>Client>> Data socket closed {}", SocketUtil.getSocketName(socket));
            String userId = ClientDataSocketManager.getUserId(socket);
            ClientDataSocketManager.offline(userId, socket);
            if (StrUtil.isNotBlank(userId)) {
                closeUserConnectionGracefully(userId);
            }
        } else {
            log.info("EP>>Client>> Client socket closed {}", SocketUtil.getSocketName(socket));
            // remove all related user sockets and data sockets
            String token = ClientSocketManager.offline(socket);
            // Update offline status
            if (StrUtil.isBlank(token)) {
                log.debug("EP>>ServerMng>> Client token is blank");
                closeSocket(socket);
                return;
            }

            EventBusUtil.publish(EventBusUtil.SOCKET_CLIENT_OFFLINE, token);

            List<ProxyClientRule> rules = proxyRuleService.getAllProxyClientRules(token, null, null, null);
            // 根据规则关闭用户连接通道
            for (ProxyClientRule rule : rules) {
                Set<String> userIds = RuleListenSocketManager.getOnlineUsers(rule.getId());
                if (userIds != null) {
                    for (String userId : userIds) {
                        Socket dataSocket = ClientDataSocketManager.getDataSocket(userId);
                        if (dataSocket != null) {
                            closeSocket(dataSocket);
                        }
                        ClientDataSocketManager.closeDataSocket(userId);
                        RuleListenSocketManager.userConnectionClose(userId);
                    }
                }
            }
        }
        closeSocket(socket);
    }

    private void closeSocket(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            // ignore
        }
    }

    // client connection auth, register if success
    private void processAuth(Message msg, Socket sMngSocket) throws IOException {
        log.debug("EP>>ServerMng>> Process auth");
        String token = msg.getToken();

        try {
            ProxyClient client = proxyClientService.selectByToken(token);

            if (client == null) {
                log.warn("EP>>ServerMng>> Client not found for supplied token");
                sMngSocket.close();
                return;
            }

            if (!Boolean.TRUE.equals(client.getEnableFlag())) {
                log.warn("EP>>ServerMng>> Client {} is disabled", client.getName());
                sMngSocket.close();
                return;
            }

        } catch (Exception e) {
            log.error("EP>>ServerMng>> Process auth error", e);
            sMngSocket.close();
            return;
        }

        if (!ClientSocketManager.online(token, sMngSocket)) {
            Socket existedMngSocket = ClientSocketManager.getClientSocket(token);
            log.info("EP>>ServerMng>> Socket {} is already connected, rejecting {}",
                    SocketUtil.getSocketName(existedMngSocket), SocketUtil.getSocketName(sMngSocket));
            sMngSocket.close();
            return;
        }
        EventBusUtil.publish(EventBusUtil.SOCKET_CLIENT_ONLINE, token);
        log.debug("EP>>ServerMng>> Process auth success");
    }

    private void processConnect(Message msg, Socket dataSocket) {
        log.debug("EP>>ServerMng>> Process connect");
        String userId = msg.getToken();
        if (!ClientDataSocketManager.online(userId, dataSocket)) {
            log.warn("EP>>ServerMng>> Duplicate data socket for user {}", userId);
            closeSocket(dataSocket);
            return;
        }
        log.debug("EP>>ServerMng>> Process connect success");
    }

    private void processData(Message msg, Socket clientSocket) throws IOException {
        log.debug("EP>>ServerMng>> Process data");
        String userId = msg.getToken();
        Socket userSocket = RuleListenSocketManager.getProxySocket(userId);
        if (userSocket != null && !userSocket.isClosed()) {
            log.debug("EP>>ServerMng>> Process data success");
            byte[] data = msg.getData();

            // Check bandwidth limit
            Integer ruleId = TrafficStatisticManager.getRuleId(userId);
            if (ruleId != null && TrafficStatisticManager.hasDownRateLimit(ruleId)) {
                TokenBucket downBucket = TrafficStatisticManager.getDownRateLimitBucket(ruleId);
                downBucket.writeWithLimit(userSocket.getOutputStream(), data);
                TrafficStatisticManager.addDownload(userId, data.length);
            } else {
                TrafficStatisticManager.addDownload(userId, data.length);
                java.io.OutputStream os = userSocket.getOutputStream();
                os.write(data);
                os.flush();
            }
        } else {
            log.debug("EP>>ServerMng>> Process data fail");
        }
    }

    private void processDisconnect(Message msg) {
        log.debug("EP>>ServerMng>> Process disconnect");
        String userId = msg.getToken();

        Socket dataSocket = ClientDataSocketManager.getDataSocket(userId);
        if (dataSocket != null && !dataSocket.isClosed()) {
            log.debug("EP>>ServerMng>> Data socket is active, deferring close to data socket EOF");
            return;
        }

        ClientDataSocketManager.closeDataSocket(userId);
        closeUserConnectionGracefully(userId);
    }

    private void closeUserConnectionGracefully(String userId) {
        RuleListenSocketManager.userConnectionClose(userId);
    }
}