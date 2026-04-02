package com.cky.proxy.client.handler;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cky.proxy.client.context.DataSocketContext;
import com.cky.proxy.client.context.MngSocketContext;
import com.cky.proxy.client.context.ProxySocketContext;
import com.cky.proxy.client.domain.Address;
import com.cky.proxy.common.domain.Message;

public class ClientMngSocketManager implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(ClientMngSocketManager.class);
    private final String token;
    private final Socket mngSocket;
    private Runnable closeHandler = null;

    public ClientMngSocketManager(String token, Socket mngSocket) {
        this.token = token;
        this.mngSocket = mngSocket;
    }

    public void init() {
        MngSocketContext.online(mngSocket);
        Thread.ofVirtual().start(this);
    }

    @Override
    public void run() {
        try {
            sendAuth();
            DataInputStream in = new DataInputStream(mngSocket.getInputStream());
            while (true) {
                Message msg = Message.readMsg(in);
                log.debug("EP>>ClientMng>> read {}", msg.getType());
                switch (msg.getType()) {
                case Message.CONNECT:
                    processConnect(msg);
                    break;
                case Message.DISCONNECT:
                    processDisconnect(msg);
                    break;
                default:
                    break;
                }
            }
        } catch (Exception e) {
            log.error("EP>>ClientMng>> Mng socket error: {}", e.getMessage());
        } finally {
            handleClose();
        }
    }

    private void sendAuth() throws IOException {
        Message.createAuthMsg(token).writeTo(mngSocket);
    }

    private void processConnect(Message msg) {
        log.debug("EP>>ClientMng>> connect");
        String userId = msg.getToken();
        String proxyAddress = new String(msg.getData(), StandardCharsets.UTF_8);
        Address address = Address.parse(proxyAddress);
        if (!address.isValid()) {
            log.error("EP>>ClientMng>> Proxy address '{}' is not valid", proxyAddress);
            sendDisconnect(userId);
            return;
        }

        log.debug("EP>>ClientMng>> Create app proxy socket");
        Thread.ofVirtual().start(() -> {
            try {
                Socket proxySocket = new Socket(address.getIp(), address.getPort());
                new ClientProxySocketManager(userId, proxySocket).init();
            } catch (Exception e) {
                log.error("EP>>ClientMng>> Proxy socket '{}' connect fail, {}", proxyAddress, e.getMessage());
                sendDisconnect(userId);
            }
        });
    }

    private void sendDisconnect(String userId) {
        try {
            Message.createDisConnectMsg(userId).writeTo(mngSocket);
        } catch (IOException e) {
            // ignore
        }
    }

    private void processDisconnect(Message msg) {
        log.debug("EP>>ClientMng>> Disconnect");
        String userId = msg.getToken();
        ProxySocketContext.close(userId);
        DataSocketContext.close(userId);
    }

    private void handleClose() {
        log.info("EP>>ClientMng>> Mng socket closed");
        try {
            mngSocket.close();
        } catch (Exception e) {
        }
        try {
            MngSocketContext.offline();
            DataSocketContext.closeAll();
            ProxySocketContext.closeAll();
        } catch (Exception e) {
            log.error("EP>>ClientMng>> Error during closeAll", e);
        } finally {
            if (closeHandler != null) {
                closeHandler.run();
            }
        }
    }

    public void closeHandler(Runnable closeHandler) {
        this.closeHandler = closeHandler;
    }
}