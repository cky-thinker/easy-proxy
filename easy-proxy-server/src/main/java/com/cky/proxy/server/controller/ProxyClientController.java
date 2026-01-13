package com.cky.proxy.server.controller;

import static com.cky.proxy.server.util.RequestUtil.getParamBool;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.cky.proxy.server.consts.AddGroup;
import com.cky.proxy.server.consts.UpdateGroup;
import com.cky.proxy.server.domain.dto.ExtendedProxyClient;
import com.cky.proxy.server.domain.dto.PageResult;
import com.cky.proxy.server.domain.entity.ProxyClient;
import com.cky.proxy.server.domain.entity.ProxyClientRule;
import com.cky.proxy.server.service.ProxyClientService;
import com.cky.proxy.server.util.EventBusUtil;
import com.cky.proxy.server.util.JsonUtil;
import com.cky.proxy.server.util.RequestUtil;
import com.cky.proxy.server.util.ResponseUtil;
import com.cky.proxy.server.util.ValidateUtil;

import cn.hutool.db.Page;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class ProxyClientController {
    private final Router router;
    private final Vertx vertx;
    private final ProxyClientService proxyClientService;
    private final com.cky.proxy.server.service.ProxyClientRuleService proxyClientRuleService;
    private final Set<HttpServerResponse> sseConnections = ConcurrentHashMap.newKeySet();

    public ProxyClientController(Router router, Vertx vertx) {
        this.router = router;
        this.vertx = vertx;
        this.proxyClientService = new ProxyClientService();
        this.proxyClientRuleService = new com.cky.proxy.server.service.ProxyClientRuleService();
        initRoutes();
        initEventBus();
    }

    private void initRoutes() {
        // 分页查询客户端
        router.get("/api/proxyClient").handler(this::getProxyClientsPageable);
        // 查询所有客户端
        router.get("/api/proxyClient/all").handler(this::getAllProxyClients);
        // 查询明细
        router.get("/api/proxyClient/detail").handler(this::getProxyClientDetail);
        // SSE 订阅
        router.get("/api/proxyClient/subscribe").handler(this::subscribeStatus);
        // 添加客户端
        router.post("/api/proxyClient").handler(this::addProxyClient);
        // 更新客户端
        router.put("/api/proxyClient").handler(this::updateProxyClient);
        // 删除客户端
        router.delete("/api/proxyClient").handler(this::deleteProxyClient);
    }

    private void initEventBus() {
        Handler<Message<Object>> handler = msg -> {
            Object body = msg.body();
            if (body != null) {
                String json = JsonUtil.toJson(body);
                String event = "data: " + json + "\n\n";
                for (HttpServerResponse resp : sseConnections) {
                    try {
                        resp.write(event);
                    } catch (Exception e) {
                        // Ignore write errors, connection might be closed
                        sseConnections.remove(resp);
                    }
                }
            }
        };
        EventBusUtil.subscribe(EventBusUtil.SOCKET_CLIENT_ONLINE, handler);
        EventBusUtil.subscribe(EventBusUtil.SOCKET_CLIENT_OFFLINE, handler);
    }

    private void subscribeStatus(RoutingContext ctx) {
        HttpServerResponse response = ctx.response();
        response.putHeader("Content-Type", "text/event-stream")
                .putHeader("Cache-Control", "no-cache")
                .putHeader("Connection", "keep-alive")
                .setChunked(true);

        sseConnections.add(response);

        response.closeHandler(v -> sseConnections.remove(response));
    }

    private void getAllProxyClients(RoutingContext ctx) {
        List<ProxyClient> list = proxyClientService.getProxyClients();
        ResponseUtil.success(ctx, list);
    }

    private void getProxyClientsPageable(RoutingContext ctx) {
        Page page = RequestUtil.getPage(ctx);
        MultiMap params = ctx.request().params();
        PageResult<ProxyClient> result = proxyClientService.getProxyClientsPageable(page, params.get("q"),
                params.get("status"), getParamBool(ctx, "enableFlag"));

        java.util.List<ExtendedProxyClient> extendedList = new java.util.ArrayList<>();
        for (ProxyClient client : result.getList()) {
            ExtendedProxyClient ext = new ExtendedProxyClient();
            ext.setId(client.getId());
            ext.setName(client.getName());
            ext.setToken(client.getToken());
            ext.setStatus(client.getStatus());
            ext.setEnableFlag(client.getEnableFlag());
            ext.setCreateBy(client.getCreateBy());
            ext.setCreateTime(client.getCreateTime());
            ext.setUpdateBy(client.getUpdateBy());
            ext.setUpdateTime(client.getUpdateTime());
            java.util.List<ProxyClientRule> rules = proxyClientRuleService.getProxyClientRules(client.getId());
            ext.setProxyRules(rules);
            extendedList.add(ext);
        }

        PageResult<ExtendedProxyClient> extendedPage = new PageResult<>(
                result.getPage(),
                result.getPageSize(),
                result.getTotalPage(),
                result.getTotal(),
                extendedList);
        ResponseUtil.success(ctx, extendedPage);
    }

    private void getProxyClientDetail(RoutingContext ctx) {
        Integer id = RequestUtil.getParamInt(ctx, "id");
        if (id == null) {
            ResponseUtil.error(ctx, 400, "Missing required parameter: id");
            return;
        }
        ProxyClient proxyClient = proxyClientService.getProxyClientById(id);
        if (proxyClient == null) {
            ResponseUtil.error(ctx, 404, "ProxyClient not found with id: " + id);
            return;
        }
        ResponseUtil.success(ctx, proxyClient);
    }

    private void addProxyClient(RoutingContext ctx) {
        ProxyClient proxyClient = RequestUtil.getBodyObj(ctx, ProxyClient.class);
        if (proxyClient == null) {
            ResponseUtil.error(ctx, 400, "Request body is required");
            return;
        }
        ValidateUtil.validate(proxyClient, AddGroup.class);
        ProxyClient newClient = proxyClientService.addProxyClient(proxyClient);
        ResponseUtil.success(ctx, newClient);
    }

    private void updateProxyClient(RoutingContext ctx) {
        ProxyClient proxyClient = RequestUtil.getBodyObj(ctx, ProxyClient.class);
        if (proxyClient == null) {
            ResponseUtil.error(ctx, 400, "Request body is required");
            return;
        }
        if (proxyClient.getId() == null) {
            ResponseUtil.error(ctx, 400, "Request body missing id");
            return;
        }
        ValidateUtil.validate(proxyClient, UpdateGroup.class);
        ProxyClient updated = proxyClientService.updateProxyClient(proxyClient);
        if (updated != null) {
            EventBusUtil.publish(EventBusUtil.DB_CLIENT_UPDATE, updated.getId());
        }
        ResponseUtil.success(ctx, updated);
    }

    private void deleteProxyClient(RoutingContext ctx) {
        Integer id = RequestUtil.getParamInt(ctx, "id");
        if (id == null) {
            ResponseUtil.error(ctx, 400, "Missing required parameter: id");
            return;
        }
        boolean deleted = proxyClientService.deleteProxyClient(id);
        if (!deleted) {
            ResponseUtil.error(ctx, 404, "Proxy client not found");
            return;
        }
        EventBusUtil.publish(EventBusUtil.DB_CLIENT_DELETE, id);
        ResponseUtil.success(ctx, null);
    }
}
