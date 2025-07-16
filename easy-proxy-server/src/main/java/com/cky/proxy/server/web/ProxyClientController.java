package com.cky.proxy.server.web;

import com.cky.proxy.server.dao.ProxyClientDao;
import com.cky.proxy.server.dao.ProxyClientGroupDao;
import com.cky.proxy.server.dao.ProxyClientRuleDao;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class ProxyClientController {
    private final Router router;
    private final ProxyClientDao proxyClientDao;

    public ProxyClientController(Router router) {
        this.router = router;
        this.proxyClientDao = new ProxyClientDao();
        initRoutes();
    }

    private void initRoutes() {
        // 分页查询客户端
        router.get("/api/proxyClient").handler(this::getProxyClientsPageable);
        // 查询客户端详情
        router.get("/api/proxyClient").handler(this::getProxyClientDetail);
        // 添加客户端
        router.post("/api/proxyClient").handler(this::addProxyClient);
        // 更新客户端
        router.put("/api/proxyClient").handler(this::updateProxyClient);
        
    }

    private void getProxyClientsPageable(RoutingContext routingcontext1) {
    }

    private void getProxyClientDetail(RoutingContext routingcontext1) {
    }

    private void addProxyClient(RoutingContext routingcontext1) {
    }

    private void updateProxyClient(RoutingContext routingcontext1) {
    }
}
