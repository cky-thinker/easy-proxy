package com.cky.proxy.server.controller;

import java.util.List;

import com.cky.proxy.server.consts.AddGroup;
import com.cky.proxy.server.consts.UpdateGroup;
import com.cky.proxy.server.domain.dto.PageResult;
import com.cky.proxy.server.domain.dto.ProxyClientReq;
import com.cky.proxy.server.domain.entity.ProxyClient;
import com.cky.proxy.server.service.ProxyClientService;
import com.cky.proxy.server.util.RequestUtil;
import com.cky.proxy.server.util.ResponseUtil;
import com.cky.proxy.server.util.ValidateUtil;

import cn.hutool.db.Page;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class ProxyClientController {
    private final Router router;
    private final ProxyClientService proxyClientService;

    public ProxyClientController(Router router) {
        this.router = router;
        this.proxyClientService = new ProxyClientService();
        initRoutes();
    }

    private void initRoutes() {
        // 分页查询客户端
        router.get("/api/proxyClient").handler(this::getProxyClientsPageable);
        // 查询所有客户端
        router.get("/api/proxyClient/all").handler(this::getAllProxyClients);
        // 查询明细
        router.get("/api/proxyClient/detail").handler(this::getProxyClientDetail);
        // 添加客户端
        router.post("/api/proxyClient").handler(this::addProxyClient);
        // 更新客户端
        router.put("/api/proxyClient").handler(this::updateProxyClient);
        // 删除客户端
        router.delete("/api/proxyClient").handler(this::deleteProxyClient);
    }

    private void getAllProxyClients(RoutingContext ctx) {
        List<ProxyClient> list = proxyClientService.getProxyClients();
        ResponseUtil.success(ctx, list);
    }

    private void getProxyClientsPageable(RoutingContext ctx) {
        Page page = RequestUtil.getPage(ctx);
        ProxyClientReq req = RequestUtil.getParamsObj(ctx, ProxyClientReq.class);
        PageResult<ProxyClient> result = proxyClientService.getProxyClientsPageable(page, req.getQ(), req.getStatus(), req.getEnableFlag());
        ResponseUtil.success(ctx, result);
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
        ResponseUtil.success(ctx, null);
    }
}
