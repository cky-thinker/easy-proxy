package com.cky.proxy.client;

import java.net.Socket;

import com.cky.proxy.client.config.ConfigProperty;
import com.cky.proxy.client.config.ServerProperty;
import com.cky.proxy.client.context.MngSocketContext;
import com.cky.proxy.client.handler.ClientMngSocketManager;
import com.cky.proxy.client.util.SslUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientRunner {
    private String serverIp;
    private Integer serverPort;
    private String token;
    private int waitTime = 1000;

    public void start() {
        ServerProperty server = ConfigProperty.getInstance().getServer();
        serverIp = server.getIp();
        serverPort = server.getPort();
        token = server.getToken();
        
        Thread.ofVirtual().start(this::connectMngServer);
    }

    private void connectMngServer() {
        log.debug("EP>>ClientMng>> Connect mng server");
        try {
            Socket mngSocket = SslUtil.getSslSocketFactory().createSocket(serverIp, serverPort);
            log.debug("EP>>ClientMng>> Connect success");
            this.waitTime = 1000; // 重试等待时间重置
            ClientMngSocketManager manager = new ClientMngSocketManager(token, mngSocket);
            manager.closeHandler(() -> {
                log.error("EP>>ClientMng>> Connect closed");
                retry();
            });
            manager.init();
        } catch (Exception e) {
            log.error("EP>>ClientMng>> Connect fail", e);
            MngSocketContext.offline();
            retry();
        }
    }

    // 失败重试
    private void retry() {
        if (waitTime > 60000) {
            waitTime = 1000;
        }
        waitTime = waitTime * 2;
        log.info("next connect wait time {}ms", waitTime);
        
        Thread.ofVirtual().start(() -> {
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Retry interrupted", e);
                return;
            }
            log.info("try connect {}:{}...", serverIp, serverPort);
            connectMngServer();
        });
    }
}
