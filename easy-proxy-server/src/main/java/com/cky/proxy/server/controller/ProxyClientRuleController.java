
package com.cky.proxy.server.controller;

import java.util.List;

import com.cky.proxy.server.dao.ProxyClientRuleDao;
import com.cky.proxy.server.util.VertxUtil;

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
        // 查询转发规则
        router.get("/api/proxyClientRule").handler(this::getAllProxyClientRules);
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
            List<com.cky.proxy.server.domain.entity.ProxyClientRule> rules = proxyClientRuleDao.selectList(qb -> {
                if (name != null && !name.isEmpty()) {
                    qb.where().like("name", "%" + name + "%");
                }
            });
            
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
            com.cky.proxy.server.domain.entity.ProxyClientRule rule = proxyClientRuleDao.selectById(id);
            
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
            io.vertx.core.json.JsonObject body = ctx.getBodyAsJson();
            if (body == null) {
                VertxUtil.error(ctx, 400, "Request body is required");
                return;
            }
            
            // 创建ProxyClientRule对象
            com.cky.proxy.server.domain.entity.ProxyClientRule rule = new com.cky.proxy.server.domain.entity.ProxyClientRule();
            rule.setName(body.getString("name"));
            rule.setServerPort(body.getInteger("serverPort"));
            rule.setClientAddress(body.getString("clientAddress"));
            rule.setEnableFlag(body.getBoolean("enableFlag", true));
            rule.setCreateBy(body.getString("createBy", "system"));
            rule.setCreateTime(new java.sql.Date(System.currentTimeMillis()));
            
            // 保存到数据库
            proxyClientRuleDao.insert(rule);
            
            // 返回成功响应
            VertxUtil.success(ctx, rule);
        } catch (Exception e) {
            VertxUtil.error(ctx, 500, "Failed to add proxy client rule: " + e.getMessage());
        }
    }

    private void updateProxyClientRule(RoutingContext ctx) {
        try {
            // 从请求体获取JSON数据
            io.vertx.core.json.JsonObject body = ctx.getBodyAsJson();
            if (body == null || !body.containsKey("id")) {
                VertxUtil.error(ctx, 400, "Request body with id is required");
                return;
            }
            
            Integer id = body.getInteger("id");
            com.cky.proxy.server.domain.entity.ProxyClientRule existingRule = proxyClientRuleDao.selectById(id);
            
            if (existingRule == null) {
                VertxUtil.error(ctx, 404, "ProxyClientRule not found with id: " + id);
                return;
            }
            
            // 更新字段
            if (body.containsKey("name")) {
                existingRule.setName(body.getString("name"));
            }
            if (body.containsKey("serverPort")) {
                existingRule.setServerPort(body.getInteger("serverPort"));
            }
            if (body.containsKey("clientAddress")) {
                existingRule.setClientAddress(body.getString("clientAddress"));
            }
            if (body.containsKey("enableFlag")) {
                existingRule.setEnableFlag(body.getBoolean("enableFlag"));
            }
            existingRule.setUpdateBy(body.getString("updateBy", "system"));
            existingRule.setUpdateTime(new java.sql.Date(System.currentTimeMillis()));
            
            // 保存到数据库
            proxyClientRuleDao.updateById(existingRule);
            
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
            com.cky.proxy.server.domain.entity.ProxyClientRule existingRule = proxyClientRuleDao.selectById(id);
            
            if (existingRule == null) {
                VertxUtil.error(ctx, 404, "ProxyClientRule not found with id: " + id);
                return;
            }
            
            // 从数据库删除
            proxyClientRuleDao.deleteById(id);
            
            // 返回成功响应
            VertxUtil.success(ctx, null);
        } catch (NumberFormatException e) {
            VertxUtil.error(ctx, 400, "Invalid id format");
        } catch (Exception e) {
            VertxUtil.error(ctx, 500, "Failed to delete proxy client rule: " + e.getMessage());
        }
    }
}
