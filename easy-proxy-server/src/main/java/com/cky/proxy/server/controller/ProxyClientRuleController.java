
package com.cky.proxy.server.controller;

import java.util.List;

import com.cky.proxy.server.domain.entity.ProxyClientRule;
import com.cky.proxy.server.service.ProxyClientRuleService;
import com.cky.proxy.server.util.VertxUtil;

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
        router.get("/api/proxyClientRule").handler(this::getAllProxyClientRules);
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
        try {
            // 获取查询参数
            String name = ctx.request().getParam("name");
            
            // 执行查询
            List<ProxyClientRule> rules = proxyClientRuleService.getAllProxyClientRules(name);
            
            // 返回结果
            VertxUtil.success(ctx, rules);
        } catch (Exception e) {
            VertxUtil.error(ctx, 500, "Failed to get proxy client rules: " + e.getMessage());
        }
    }
    
    private void getProxyClientRuleDetail(RoutingContext ctx) {
        try {
            // 获取ID参数
            String idParam = ctx.request().getParam("id");
            if (idParam == null || idParam.isEmpty()) {
                VertxUtil.error(ctx, 400, "Missing required parameter: id");
                return;
            }
            
            Integer id = Integer.parseInt(idParam);
            ProxyClientRule rule = proxyClientRuleService.getProxyClientRuleById(id);
            
            if (rule == null) {
                VertxUtil.error(ctx, 404, "ProxyClientRule not found with id: " + id);
                return;
            }
            
            // 返回结果
            VertxUtil.success(ctx, rule);
        } catch (NumberFormatException e) {
            VertxUtil.error(ctx, 400, "Invalid id format");
        } catch (Exception e) {
            VertxUtil.error(ctx, 500, "Failed to get proxy client rule: " + e.getMessage());
        }
    }

    private void addProxyClientRule(RoutingContext ctx) {
        try {
            // 从请求体获取JSON数据
            JsonObject body = ctx.body().asJsonObject();
            if (body == null) {
                VertxUtil.error(ctx, 400, "Request body is required");
                return;
            }
            
            // 创建ProxyClientRule对象
            ProxyClientRule rule = new ProxyClientRule();
            rule.setName(body.getString("name"));
            rule.setServerPort(body.getInteger("serverPort"));
            rule.setClientAddress(body.getString("clientAddress"));
            rule.setEnableFlag(body.getBoolean("enableFlag", true));
            
            // 保存到数据库
            ProxyClientRule newRule = proxyClientRuleService.addProxyClientRule(body);
            
            // 返回成功响应
            VertxUtil.success(ctx, newRule);
        } catch (Exception e) {
            VertxUtil.error(ctx, 500, "Failed to add proxy client rule: " + e.getMessage());
        }
    }

    private void updateProxyClientRule(RoutingContext ctx) {
        try {
            // 从请求体获取JSON数据
            JsonObject body = ctx.body().asJsonObject();
            if (body == null || !body.containsKey("id")) {
                VertxUtil.error(ctx, 400, "Request body with id is required");
                return;
            }
        
            ProxyClientRule rule = new ProxyClientRule();
            rule.setId(body.getInteger("id"));
            rule.setName(body.getString("name"));
            rule.setServerPort(body.getInteger("serverPort"));
            rule.setClientAddress(body.getString("clientAddress"));
            rule.setEnableFlag(body.getBoolean("enableFlag", true));
            ProxyClientRule existingRule = proxyClientRuleService.updateProxyClientRule(rule);
            
            // 返回成功响应
            VertxUtil.success(ctx, existingRule);
        } catch (Exception e) {
            VertxUtil.error(ctx, 500, "Failed to update proxy client rule: " + e.getMessage());
        }
    }

    private void deleteProxyClientRule(RoutingContext ctx) {
        try {
            // 获取ID参数
            String idParam = ctx.request().getParam("id");
            if (idParam == null || idParam.isEmpty()) {
                VertxUtil.error(ctx, 400, "Missing required parameter: id");
                return;
            }
            
            Integer id = Integer.parseInt(idParam);
            boolean deleted = proxyClientRuleService.deleteProxyClientRule(id);
            
            // 从数据库删除
            if (!deleted) {
                VertxUtil.error(ctx, 404, "Proxy client rule not found");
                return;
            }
            
            // 返回成功响应
            VertxUtil.success(ctx, null);
        } catch (NumberFormatException e) {
            VertxUtil.error(ctx, 400, "Invalid id format");
        } catch (Exception e) {
            VertxUtil.error(ctx, 500, "Failed to delete proxy client rule: " + e.getMessage());
        }
    }
}
