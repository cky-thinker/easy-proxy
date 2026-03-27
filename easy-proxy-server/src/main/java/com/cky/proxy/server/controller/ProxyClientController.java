package com.cky.proxy.server.controller;

import static com.cky.proxy.server.util.RequestUtil.getParamBool;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.cky.proxy.common.consts.OnlineStatus;
import com.cky.proxy.server.consts.AddGroup;
import com.cky.proxy.server.consts.UpdateGroup;
import com.cky.proxy.server.domain.dto.PageResult;
import com.cky.proxy.server.domain.dto.ProxyClientImportReq;
import com.cky.proxy.server.domain.dto.SseEvent;
import com.cky.proxy.server.domain.entity.ProxyClient;
import com.cky.proxy.server.service.ProxyClientService;
import com.cky.proxy.server.util.BeanContext;
import com.cky.proxy.server.util.EventBusUtil;
import com.cky.proxy.server.util.JsonUtil;
import com.cky.proxy.server.util.RequestUtil;
import com.cky.proxy.server.util.ResponseUtil;
import com.cky.proxy.server.util.ValidateUtil;

import cn.hutool.db.Page;
import com.cky.proxy.server.http.HttpContext;
import com.cky.proxy.server.http.HttpRouter;
import java.io.OutputStream;

public class ProxyClientController {
    private final HttpRouter router;
    private final ProxyClientService proxyClientService;
    private final Set<OutputStream> sseConnections = ConcurrentHashMap.newKeySet();

    public ProxyClientController(HttpRouter router) {
        this.router = router;
        this.proxyClientService = BeanContext.getProxyClientService();
        initRoutes();
        initEventBus();
    }

    private void initRoutes() {
        // 分页查询客户端
        router.get("/api/proxyClient", this::getProxyClientsPageable);
        // 查询所有客户端
        router.get("/api/proxyClient/all", this::getAllProxyClients);
        // 查询明细
        router.get("/api/proxyClient/detail", this::getProxyClientDetail);
        // SSE 订阅
        router.get("/api/proxyClient/subscribe", this::subscribeStatus);
        // 添加客户端
        router.post("/api/proxyClient", this::addProxyClient);
        // 导入客户端
        router.post("/api/proxyClient/import", this::importProxyClients);
        // 更新客户端
        router.put("/api/proxyClient", this::updateProxyClient);
        // 删除客户端
        router.delete("/api/proxyClient", this::deleteProxyClient);
    }

    private void initEventBus() {
        EventBusUtil.<String>subscribe(EventBusUtil.SOCKET_CLIENT_ONLINE, token -> {
            ProxyClient proxyClient = proxyClientService.updateClientStatus(token, OnlineStatus.online.name());
            if (proxyClient != null) {
                sendSseEvent(EventBusUtil.SOCKET_CLIENT_ONLINE, proxyClient.getId().toString());
            }
        });
        EventBusUtil.<String>subscribe(EventBusUtil.SOCKET_CLIENT_OFFLINE, token -> {
            ProxyClient proxyClient = proxyClientService.updateClientStatus(token, OnlineStatus.offline.name());
            if (proxyClient != null) {
                sendSseEvent(EventBusUtil.SOCKET_CLIENT_OFFLINE, proxyClient.getId().toString());
            }
        });
    }

    private void sendSseEvent(String eventType, String data) {
        SseEvent eventBody = new SseEvent();
        eventBody.setEventType(eventType);
        eventBody.setData(data);
        String json = JsonUtil.toJson(eventBody);
        String event = "data: " + json + "\n\n";
        byte[] bytes = event.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        for (OutputStream resp : sseConnections) {
            try {
                resp.write(bytes);
                resp.flush();
            } catch (Exception e) {
                // Ignore write errors, connection might be closed
                sseConnections.remove(resp);
            }
        }
    }

    private void subscribeStatus(HttpContext ctx) {
        com.sun.net.httpserver.HttpExchange exchange = ctx.getExchange();
        exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
        exchange.getResponseHeaders().add("Cache-Control", "no-cache");
        exchange.getResponseHeaders().add("Connection", "keep-alive");
        
        try {
            exchange.sendResponseHeaders(200, 0);
            OutputStream response = exchange.getResponseBody();
            sseConnections.add(response);
            
            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(10000);
                response.write(":\n\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                response.flush();
            }
        } catch (Exception e) {
            // disconnected
        }
    }

    private void getAllProxyClients(HttpContext ctx) {
        List<ProxyClient> list = proxyClientService.getProxyClients();
        ResponseUtil.success(ctx, list);
    }

    private void getProxyClientsPageable(HttpContext ctx) {
        Page page = RequestUtil.getPage(ctx);
        PageResult<ProxyClient> result = proxyClientService.getProxyClientsPageable(page, ctx.getParam("q"),
                ctx.getParam("status"), getParamBool(ctx, "enableFlag"));

        PageResult<ProxyClient> extendedPage = new PageResult<>(
                result.getPage(),
                result.getPageSize(),
                result.getTotalPage(),
                result.getTotal(),
                result.getList());
        ResponseUtil.success(ctx, extendedPage);
    }

    private void getProxyClientDetail(HttpContext ctx) {
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

    private void addProxyClient(HttpContext ctx) {
        ProxyClient proxyClient = RequestUtil.getBodyObj(ctx, ProxyClient.class);
        if (proxyClient == null) {
            ResponseUtil.error(ctx, 400, "Request body is required");
            return;
        }
        ValidateUtil.validate(proxyClient, AddGroup.class);
        ProxyClient newClient = proxyClientService.addProxyClient(proxyClient);
        ResponseUtil.success(ctx, newClient);
    }

    private void importProxyClients(HttpContext ctx) {
        ProxyClientImportReq req = RequestUtil.getBodyObj(ctx, ProxyClientImportReq.class);
        if (req == null || req.getClients() == null) {
            ResponseUtil.error(ctx, 400, "Request body is required and must contain clients");
            return;
        }
        try {
            proxyClientService.importClients(req);
            ResponseUtil.success(ctx, null);
        } catch (Exception e) {
            ResponseUtil.error(ctx, 400, "导入失败: " + e.getMessage());
        }
    }

    private void updateProxyClient(HttpContext ctx) {
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
        ResponseUtil.success(ctx, updated);
    }

    private void deleteProxyClient(HttpContext ctx) {
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
        ResponseUtil.success(ctx, null);
    }
}
