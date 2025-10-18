
package com.cky.proxy.server.controller;

import java.util.List;

import com.cky.proxy.server.domain.dto.PageResult;
import com.cky.proxy.server.domain.dto.ProxyClientReq;
import com.cky.proxy.server.domain.entity.ProxyClientRule;
import com.cky.proxy.server.service.ProxyClientRuleService;
import com.cky.proxy.server.util.JsonUtil;
import com.cky.proxy.server.util.RequestUtil;
import com.cky.proxy.server.util.ResponseUtil;

import cn.hutool.db.Page;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class ProxyClientRuleController {
    private final Router router;
    private final ProxyClientRuleService proxyClientRuleService;

    public ProxyClientRuleController(Router router) {
        this.router = router;
        this.proxyClientRuleService = new ProxyClientRuleService();
        initRoutes();
    }

    private void initRoutes() {
        // 查询转发规则
        router.get("/api/proxyClientRule/all").handler(this::getAllProxyClientRules);
        // 分页查询转发规则
        router.get("/api/proxyClientRule").handler(this::getProxyClientRulesPageable);
        // 查询转发规则详情
        router.get("/api/proxyClientRule/:id").handler(this::getProxyClientRuleDetail);
        // 新增转发规则
        router.post("/api/proxyClientRule").handler(this::addProxyClientRule);
        // 修改转发规则
        router.put("/api/proxyClientRule").handler(this::updateProxyClientRule);
        // 删除转发规则
        router.delete("/api/proxyClientRule/:id").handler(this::deleteProxyClientRule);
    }

    private void getAllProxyClientRules(RoutingContext ctx) {
        // 获取查询参数
        ProxyClientReq proxyClientReq = RequestUtil.getParamsObj(ctx, ProxyClientReq.class);

        // 执行查询
        List<ProxyClientRule> rules = proxyClientRuleService.getAllProxyClientRules(proxyClientReq.getQ(),
                proxyClientReq.getServerPort(), proxyClientReq.getProxyClientId());

        // 返回结果
        ResponseUtil.success(ctx, rules);
    }

    private void getProxyClientRulesPageable(RoutingContext ctx) {
        // 创建分页对象
        Page page = RequestUtil.getPage(ctx);
        // 获取查询参数
        ProxyClientReq proxyClientReq = RequestUtil.getParamsObj(ctx, ProxyClientReq.class);

        // 执行分页查询
        PageResult<ProxyClientRule> result = proxyClientRuleService.getProxyClientRulesPageable(page,
                proxyClientReq.getQ(), proxyClientReq.getServerPort(),
                proxyClientReq.getProxyClientId());

        // 返回结果
        ResponseUtil.success(ctx, result);
    }

    private void getProxyClientRuleDetail(RoutingContext ctx) {
        // 获取ID参数
        Integer id = RequestUtil.getParamInt(ctx, "id");
        if (id == null) {
            ResponseUtil.error(ctx, 400, "Missing required parameter: id");
            return;
        }

        ProxyClientRule rule = proxyClientRuleService.getProxyClientRuleById(id);

        if (rule == null) {
            ResponseUtil.error(ctx, 404, "ProxyClientRule not found with id: " + id);
            return;
        }

        // 返回结果
        ResponseUtil.success(ctx, rule);
    }

    private void addProxyClientRule(RoutingContext ctx) {
        ProxyClientRule rule = RequestUtil.getBodyObj(ctx, ProxyClientRule.class);
        if (rule == null) {
            ResponseUtil.error(ctx, 400, "Request body is required");
            return;
        }
        // 保存到数据库
        ProxyClientRule newRule = proxyClientRuleService.addProxyClientRule(rule);

        // 返回成功响应
        ResponseUtil.success(ctx, newRule);
    }

    private void updateProxyClientRule(RoutingContext ctx) {
        ProxyClientRule rule = RequestUtil.getBodyObj(ctx, ProxyClientRule.class);
        if (rule == null) {
            ResponseUtil.error(ctx, 400, "Request body is required");
            return;
        }
        ProxyClientRule existingRule = proxyClientRuleService.updateProxyClientRule(rule);

        // 返回成功响应
        ResponseUtil.success(ctx, existingRule);
    }

    private void deleteProxyClientRule(RoutingContext ctx) {
        // 获取ID参数
        Integer id = RequestUtil.getParamInt(ctx, "id");
        if (id == null) {
            ResponseUtil.error(ctx, 400, "Missing required parameter: id");
            return;
        }
        boolean deleted = proxyClientRuleService.deleteProxyClientRule(id);

        // 从数据库删除
        if (!deleted) {
            ResponseUtil.error(ctx, 404, "Proxy client rule not found");
            return;
        }

        // 返回成功响应
        ResponseUtil.success(ctx, null);
    }
}
