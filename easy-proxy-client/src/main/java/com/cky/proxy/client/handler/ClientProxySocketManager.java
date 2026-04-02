package com.cky.proxy.client.handler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cky.proxy.client.config.ConfigProperty;
import com.cky.proxy.client.config.ServerProperty;
import com.cky.proxy.client.context.DataSocketContext;
import com.cky.proxy.client.context.MngSocketContext;
import com.cky.proxy.client.context.ProxySocketContext;
import com.cky.proxy.client.util.SslUtil;
import com.cky.proxy.common.domain.Message;

public class ClientProxySocketManager {
    private static final Logger log = LoggerFactory.getLogger(ClientProxySocketManager.class);
    private final String userId;
    private final Socket proxySocket;

    public ClientProxySocketManager(String userId, Socket proxySocket) {
        this.userId = userId;
        this.proxySocket = proxySocket;
    }

    public void init() {
        log.debug("EP>>ClientProxy>> Proxy socket {} create success", proxySocket.getRemoteSocketAddress());
        ProxySocketContext.online(userId, proxySocket);

        ServerProperty server = ConfigProperty.getInstance().getServer();
        log.debug("EP>>ClientProxy>> Create data socket");
        try {
            Socket dataSocket = SslUtil.getSslSocketFactory().createSocket(server.getIp(), server.getPort());
            DataSocketContext.online(userId, dataSocket);

            // Send CONNECT message
            Message.createConnectMsg(userId).writeTo(dataSocket);

            // Start proxy -> data
            Thread.ofVirtual().start(() -> {
                try {
                    InputStream in = proxySocket.getInputStream();
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        byte[] data = new byte[bytesRead];
                        System.arraycopy(buffer, 0, data, 0, bytesRead);
                        Message.createDataMsg(userId, data).writeTo(dataSocket);
                    }
                } catch (IOException e) {
                    log.debug("EP>>ClientProxy>> Proxy -> Data error: {}", e.getMessage());
                } finally {
                    handleClose();
                }
            });

            // Start data -> proxy
            Thread.ofVirtual().start(() -> {
                try {
                    DataInputStream in = new DataInputStream(dataSocket.getInputStream());
                    while (true) {
                        Message msg = Message.readMsg(in);
                        if (msg.getType() == Message.DATA) {
                            proxySocket.getOutputStream().write(msg.getData());
                            proxySocket.getOutputStream().flush();
                        }
                    }
                } catch (IOException e) {
                    log.debug("EP>>ClientProxy>> Data -> Proxy error: {}", e.getMessage());
                } finally {
                    handleClose();
                }
            });

        } catch (Exception e) {
            log.error("EP>>ClientProxy>> Create data socket failed", e);
            handleClose();
        }
    }

    private void handleClose() {
        log.debug("EP>>ClientProxy>> Socket closed");
        ProxySocketContext.close(userId);
        DataSocketContext.close(userId);
        Socket mngSocket = MngSocketContext.getMngSocket();
        if (mngSocket != null) {
            try {
                Message.createDisConnectMsg(userId).writeTo(mngSocket);
            } catch (IOException e) {
                // ignore
            }
        }
    }
}