package com.cky.proxy.server.socket;

import com.cky.proxy.common.domain.Message;
import com.cky.proxy.common.util.SocketUtil;
import com.cky.proxy.server.context.DataSocketManager;
import com.cky.proxy.server.context.MngSocketManager;
import com.cky.proxy.server.context.UserSocketManager;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Slf4j
public class ServerMngSocketHandler implements Handler<NetSocket> {
    @Override
    public void handle(NetSocket sMngSocket) {
        handleRead(sMngSocket);
        handleClose(sMngSocket);
    }

    private void handleRead(NetSocket sMngSocket) {
        Message.decodeMsg(sMngSocket, msg -> {
            log.debug("EP>>ServerMng>> Read msg {}", msg.getType());
            switch (msg.getType()) {
                case Message.AUTH:
                    processAuth(msg, sMngSocket);
                    break;
                case Message.CONNECT:
                    processConnect(msg, sMngSocket);
                    break;
                case Message.DATA:
                    processData(msg);
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
            if (DataSocketManager.isDataSocket(socket)) {
                log.info("EP>>ServerMng>> Data mng socket closed {}", SocketUtil.getSocketName(socket));
                // remove related user sockets
                String userId = DataSocketManager.getUserId(socket);
                DataSocketManager.offline(userId);
                UserSocketManager.closeUserSocket(userId);
            } else {
                log.info("EP>>ServerMng>> Server mng socket closed {}", SocketUtil.getSocketName(socket));
                // remove all related user sockets and data sockets
                String token = MngSocketManager.offline(socket);
                Set<String> userIds = UserSocketManager.getOnlineUsers(token);
                if (userIds != null) {
                    for (String userId : userIds) {
                        DataSocketManager.closeDataSocket(userId);
                        UserSocketManager.closeUserSocket(userId);
                    }
                }
            }
        });
    }

    // client connection auth, register if success
    private void processAuth(Message msg, NetSocket sMngSocket) {
        log.debug("EP>>ServerMng>> Process auth");
        String token = msg.getToken();
        NetSocket existedMngSocket = MngSocketManager.getMngSocket(token);
        if (existedMngSocket != null) {
            log.info("EP>>ServerMng>> Socket {} is connected, Can't connect again {}", SocketUtil.getSocketName(existedMngSocket), SocketUtil.getSocketName(sMngSocket));
            sMngSocket.close();
        }
        MngSocketManager.online(token, sMngSocket);
        log.debug("EP>>ServerMng>> Process auth success");
    }

    private void processConnect(Message msg, NetSocket dataSocket) {
        log.debug("EP>>ServerMng>> Process connect");
        String userId = msg.getToken();
        DataSocketManager.online(userId, dataSocket);
        NetSocket userProxySocket = UserSocketManager.getProxySocket(userId);
        if (userProxySocket != null) {
            userProxySocket.resume();
            log.debug("EP>>ServerMng>> Process connect success");
        } else {
            log.debug("EP>>ServerMng>> Process connect fail");
        }
    }

    private void processData(Message msg) {
        log.debug("EP>>ServerMng>> Process data");
        String userId = msg.getToken();
        NetSocket userSocket = UserSocketManager.getProxySocket(userId);
        if (userSocket != null) {
            log.debug("EP>>ServerMng>> Process data success");
            userSocket.write(Buffer.buffer(msg.getData()));
        } else {
            log.debug("EP>>ServerMng>> Process data fail");
        }
    }

    private void processDisconnect(Message msg) {
        log.debug("EP>>ServerMng>> Process disconnect");
        String userId = msg.getToken();
        DataSocketManager.closeDataSocket(userId);
        UserSocketManager.closeUserSocket(userId);
    }
}
