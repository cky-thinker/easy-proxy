package com.cky.proxy.client.verticle;

import cn.hutool.setting.dialect.Props;
import com.cky.proxy.client.context.MngSocketContext;
import com.cky.proxy.client.handler.ClientMngSocketManager;
import com.cky.proxy.common.util.ConfigUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MainVerticle extends AbstractVerticle {
    private String serverIp;
    private Integer serverPort;
    private String token;
    private int waitTime = 1000;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        Props config = ConfigUtil.getConfig();
        serverIp = config.getStr("server.ip");
        serverPort = config.getInt("server.port");
        token = config.getStr("client.token");
        connectMngServer();
    }

    private void connectMngServer() {
        log.debug("EP>>ClientMng>> Connect mng server");
        vertx.createNetClient()
            .connect(serverPort, serverIp)
            .onSuccess((mngSocket) -> {
                log.debug("EP>>ClientMng>> Connect success");
                this.waitTime = 1000;  // 重试等待时间重置
                ClientMngSocketManager manager = new ClientMngSocketManager(vertx, token, mngSocket);
                manager.init();
                manager.closeHandler(v -> {
                    log.error("EP>>ClientMng>> Connect closed");
                    retry();
                });
            }).onFailure(e -> {
                log.error("EP>>ClientMng>> Connect fail", e);
                MngSocketContext.offline();
                retry();
            });
    }

    // 失败重试
    private void retry() {
        if (waitTime > 60000) {
            waitTime = 1000;
        }
        waitTime = waitTime * 2;
        log.info("next connect wait time {}ms", waitTime);
        vertx.setTimer(waitTime, (time) -> {
            log.info("try connect {}:{}...", serverIp, serverPort);
            connectMngServer();
        });
    }
}
