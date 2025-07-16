
package com.cky.proxy.server.web;

import com.cky.proxy.server.dao.ProxyClientRuleDao;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class ProxyClientRuleController {
    private final Router router;
    private final ProxyClientRuleDao proxyClientRuleDao;

    public ProxyClientRuleController(Router router) {
        this.router = router;
        this.proxyClientRuleDao = new ProxyClientRuleDao();
        initRoutes();
    }

    private void initRoutes() {
        // 查询客户端分组
        router.get("/api/proxyClientRule/all").handler(this::getAllProxyClientGroup);
        // 新增客户端分组
        router.post("/api/proxyClientRule").handler(this::addProxyClientGroup);
        // 修改客户端分组
        router.put("/api/proxyClientRule").handler(this::updateProxyClientGroup);
        // 删除客户端分组
        router.delete("/api/proxyClientRule").handler(this::deleteProxyClientGroup);
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
