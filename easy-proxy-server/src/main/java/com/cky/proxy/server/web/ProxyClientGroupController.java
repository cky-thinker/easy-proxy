package com.cky.proxy.server.web;

import com.cky.proxy.server.dao.ProxyClientGroupDao;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class ProxyClientGroupController {
private final Router router;
    private final ProxyClientGroupDao proxyClientGroupDao;

    public ProxyClientGroupController(Router router) {
        this.router = router;
        this.proxyClientGroupDao = new ProxyClientGroupDao();
        initRoutes();
    }

    private void initRoutes() {
        // 查询客户端分组
        router.get("/api/proxyClientGroup/all").handler(this::getAllProxyClientGroup);
        // 新增客户端分组
        router.post("/api/proxyClientGroup").handler(this::addProxyClientGroup);
        // 修改客户端分组
        router.put("/api/proxyClientGroup").handler(this::updateProxyClientGroup);
        // 删除客户端分组
        router.delete("/api/proxyClientGroup").handler(this::deleteProxyClientGroup);
    }

    private void getAllProxyClientGroup(RoutingContext routingcontext1) {
    }

    private void addProxyClientGroup(RoutingContext routingcontext1) {
    }

    private void updateProxyClientGroup(RoutingContext routingcontext1) {
    }

    private void deleteProxyClientGroup(RoutingContext routingcontext1) {
    }
}
