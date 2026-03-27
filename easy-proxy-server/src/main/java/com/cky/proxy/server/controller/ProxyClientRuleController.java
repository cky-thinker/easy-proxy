
package com.cky.proxy.server.controller;

import cn.hutool.db.Page;
import com.cky.proxy.server.consts.AddGroup;
import com.cky.proxy.server.consts.UpdateGroup;
import com.cky.proxy.server.domain.dto.PageResult;
import com.cky.proxy.server.domain.entity.ProxyClientRule;
import com.cky.proxy.server.service.ProxyClientRuleService;
import com.cky.proxy.server.util.BeanContext;
import com.cky.proxy.server.util.RequestUtil;
import com.cky.proxy.server.util.ResponseUtil;
import com.cky.proxy.server.util.ValidateUtil;
import com.cky.proxy.server.util.EventBusUtil;
import com.cky.proxy.server.http.HttpContext;
import com.cky.proxy.server.http.HttpRouter;

import java.util.List;

import static com.cky.proxy.server.util.RequestUtil.getParam;
import static com.cky.proxy.server.util.RequestUtil.getParamInt;
import static com.cky.proxy.server.util.RequestUtil.getParamBool;

public class ProxyClientRuleController {
    private final HttpRouter router;
    private final ProxyClientRuleService proxyClientRuleService;

    public ProxyClientRuleController(HttpRouter router) {
        this.router = router;
        this.proxyClientRuleService = BeanContext.getProxyClientRuleService();
        initRoutes();
    }

    private void initRoutes() {
        // 查询转发规则
        router.get("/api/proxyClientRule/all", this::getAllProxyClientRules);
        // 分页查询转发规则
        router.get("/api/proxyClientRule", this::getProxyClientRulesPageable);
        // 查询转发规则详情
        router.get("/api/proxyClientRule/detail", this::getProxyClientRuleDetail);
        // 新增转发规则
        router.post("/api/proxyClientRule", this::addProxyClientRule);
        // 修改转发规则（使用请求参数 id）
        router.put("/api/proxyClientRule", this::updateProxyClientRule);
        // 删除转发规则（使用请求参数 id）
        router.delete("/api/proxyClientRule", this::deleteProxyClientRule);
    }

    private void getAllProxyClientRules(HttpContext ctx) {
        // 执行查询
        List<ProxyClientRule> rules = proxyClientRuleService.getAllProxyClientRules(getParam(ctx, "name"), getParamInt(ctx, "serverPort"), getParamInt(ctx, "proxyClientId"), getParamBool(ctx, "enableFlag"));
        // 返回结果
        ResponseUtil.success(ctx, rules);
    }

    private void getProxyClientRulesPageable(HttpContext ctx) {
        // 创建分页对象
        Page page = RequestUtil.getPage(ctx);
        // 执行分页查询
        PageResult<ProxyClientRule> result = proxyClientRuleService.getProxyClientRulesPageable(page, getParam(ctx, "name"),
            getParamInt(ctx, "serverPort"),
            getParamInt(ctx, "proxyClientId"),
            getParamBool(ctx, "enableFlag"));

        // 返回结果
        ResponseUtil.success(ctx, result);
    }

    private void getProxyClientRuleDetail(HttpContext ctx) {
        // 获取ID参数
        Integer id = getParamInt(ctx, "id");
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

    private void addProxyClientRule(HttpContext ctx) {
        ProxyClientRule rule = RequestUtil.getBodyObj(ctx, ProxyClientRule.class);
        if (rule == null) {
            ResponseUtil.error(ctx, 400, "Request body is required");
            return;
        }
        ValidateUtil.validate(rule, AddGroup.class);
        // 保存到数据库
        ProxyClientRule newRule = proxyClientRuleService.addProxyClientRule(rule);

        // 返回成功响应
        ResponseUtil.success(ctx, newRule);
    }

    private void updateProxyClientRule(HttpContext ctx) {
        ProxyClientRule rule = RequestUtil.getBodyObj(ctx, ProxyClientRule.class);
        if (rule == null) {
            ResponseUtil.error(ctx, 400, "Request body is required");
            return;
        }
        ValidateUtil.validate(rule, UpdateGroup.class);
        ProxyClientRule existingRule = proxyClientRuleService.updateProxyClientRule(rule);

        // 返回成功响应
        ResponseUtil.success(ctx, existingRule);
    }

    private void deleteProxyClientRule(HttpContext ctx) {
        // 获取ID参数
        Integer id = getParamInt(ctx, "id");
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
