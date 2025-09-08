package com.cky.proxy.server.verticle;

import cn.hutool.setting.dialect.Props;
import com.cky.proxy.common.util.ConfigUtil;
import com.cky.proxy.server.bean.entity.ProxyClient;
import com.cky.proxy.server.bean.entity.ProxyClientRule;
import com.cky.proxy.server.service.ProxyClientRuleService;
import com.cky.proxy.server.service.ProxyClientService;
import com.cky.proxy.server.socket.ServerMngSocketHandler;
import com.cky.proxy.server.socket.UserProxySocketHandler;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ProxyServerVerticle extends AbstractVerticle {
    private ProxyClientService proxyClientService = new ProxyClientService();
    private ProxyClientRuleService proxyClientRuleService = new ProxyClientRuleService();

    @Override
    public void start(Promise<Void> startPromise) {
        Props config = ConfigUtil.getConfig();
        Integer serverMngPort = config.getInt("server.port");
        log.info("Server starting {}", serverMngPort);
        vertx.createNetServer()
                .connectHandler(new ServerMngSocketHandler())
                .listen(serverMngPort)
                .onFailure(t -> log.error("Server start failed", t));
        // TODO SSL https://vertx.io/docs/vertx-core/java/#ssl
        flushServerProxySocket();
    }

    private void flushServerProxySocket() {
        List<ProxyClient> proxyClients = proxyClientService.getProxyClients();
        for (ProxyClient proxyClient : proxyClients) {
            log.debug("EP>> Init client {} ", proxyClient.getName());
            List<ProxyClientRule> proxyClientRules = proxyClientRuleService.getProxyClientRules(proxyClient.getId());
            for (ProxyClientRule proxyRule : proxyClientRules) {
                if (proxyRule.getEnableFlag()) {
                    vertx.createNetServer()
                        .connectHandler(new UserProxySocketHandler(proxyClient, proxyRule))
                        .listen(proxyRule.getServerPort())
                        .onFailure(t -> log.error("sMngServer 启动失败", t));
                    log.debug("EP>> Init rule {} {} -> {}", proxyRule.getName(), proxyRule.getServerPort(),
                        proxyRule.getClientAddress());
                }
            }
        }
    }
}
