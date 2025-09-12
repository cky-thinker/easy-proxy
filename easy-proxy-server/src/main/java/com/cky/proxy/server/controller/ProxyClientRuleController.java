
package com.cky.proxy.server.controller;

import java.util.List;

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
            ctx.response()
                .putHeader("content-type", "application/json")
                .end(io.vertx.core.json.Json.encode(rules));
        } catch (Exception e) {
            ctx.response()
                .setStatusCode(500)
                .putHeader("content-type", "application/json")
                .end(new io.vertx.core.json.JsonObject()
                    .put("error", "Failed to get proxy client rules: " + e.getMessage())
                    .encode());
        }
    }
    
    private void getProxyClientRuleDetail(RoutingContext ctx) {
        try {
            // 获取ID参数
            String idParam = ctx.request().getParam("id");
            if (idParam == null || idParam.isEmpty()) {
                ctx.response()
                    .setStatusCode(400)
                    .putHeader("content-type", "application/json")
                    .end(new io.vertx.core.json.JsonObject()
                        .put("error", "Missing required parameter: id")
                        .encode());
                return;
            }
            
            Integer id = Integer.parseInt(idParam);
            com.cky.proxy.server.domain.entity.ProxyClientRule rule = proxyClientRuleDao.selectById(id);
            
            if (rule == null) {
                ctx.response()
                    .setStatusCode(404)
                    .putHeader("content-type", "application/json")
                    .end(new io.vertx.core.json.JsonObject()
                        .put("error", "ProxyClientRule not found with id: " + id)
                        .encode());
                return;
            }
            
            // 返回结果
            ctx.response()
                .putHeader("content-type", "application/json")
                .end(io.vertx.core.json.Json.encode(rule));
        } catch (NumberFormatException e) {
            ctx.response()
                .setStatusCode(400)
                .putHeader("content-type", "application/json")
                .end(new io.vertx.core.json.JsonObject()
                    .put("error", "Invalid id format")
                    .encode());
        } catch (Exception e) {
            ctx.response()
                .setStatusCode(500)
                .putHeader("content-type", "application/json")
                .end(new io.vertx.core.json.JsonObject()
                    .put("error", "Failed to get proxy client rule: " + e.getMessage())
                    .encode());
        }
    }

    private void addProxyClientRule(RoutingContext ctx) {
        try {
            // 从请求体获取JSON数据
            io.vertx.core.json.JsonObject body = ctx.getBodyAsJson();
            if (body == null) {
                ctx.response()
                    .setStatusCode(400)
                    .putHeader("content-type", "application/json")
                    .end(new io.vertx.core.json.JsonObject()
                        .put("error", "Request body is required")
                        .encode());
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
            ctx.response()
                .setStatusCode(201)
                .putHeader("content-type", "application/json")
                .end(io.vertx.core.json.Json.encode(rule));
        } catch (Exception e) {
            ctx.response()
                .setStatusCode(500)
                .putHeader("content-type", "application/json")
                .end(new io.vertx.core.json.JsonObject()
                    .put("error", "Failed to add proxy client rule: " + e.getMessage())
                    .encode());
        }
    }

    private void updateProxyClientRule(RoutingContext ctx) {
        try {
            // 从请求体获取JSON数据
            io.vertx.core.json.JsonObject body = ctx.getBodyAsJson();
            if (body == null || !body.containsKey("id")) {
                ctx.response()
                    .setStatusCode(400)
                    .putHeader("content-type", "application/json")
                    .end(new io.vertx.core.json.JsonObject()
                        .put("error", "Request body with id is required")
                        .encode());
                return;
            }
            
            Integer id = body.getInteger("id");
            com.cky.proxy.server.domain.entity.ProxyClientRule existingRule = proxyClientRuleDao.selectById(id);
            
            if (existingRule == null) {
                ctx.response()
                    .setStatusCode(404)
                    .putHeader("content-type", "application/json")
                    .end(new io.vertx.core.json.JsonObject()
                        .put("error", "ProxyClientRule not found with id: " + id)
                        .encode());
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
            ctx.response()
                .putHeader("content-type", "application/json")
                .end(io.vertx.core.json.Json.encode(existingRule));
        } catch (Exception e) {
            ctx.response()
                .setStatusCode(500)
                .putHeader("content-type", "application/json")
                .end(new io.vertx.core.json.JsonObject()
                    .put("error", "Failed to update proxy client rule: " + e.getMessage())
                    .encode());
        }
    }

    private void deleteProxyClientRule(RoutingContext ctx) {
        try {
            // 获取ID参数
            String idParam = ctx.request().getParam("id");
            if (idParam == null || idParam.isEmpty()) {
                ctx.response()
                    .setStatusCode(400)
                    .putHeader("content-type", "application/json")
                    .end(new io.vertx.core.json.JsonObject()
                        .put("error", "Missing required parameter: id")
                        .encode());
                return;
            }
            
            Integer id = Integer.parseInt(idParam);
            com.cky.proxy.server.domain.entity.ProxyClientRule existingRule = proxyClientRuleDao.selectById(id);
            
            if (existingRule == null) {
                ctx.response()
                    .setStatusCode(404)
                    .putHeader("content-type", "application/json")
                    .end(new io.vertx.core.json.JsonObject()
                        .put("error", "ProxyClientRule not found with id: " + id)
                        .encode());
                return;
            }
            
            // 从数据库删除
            proxyClientRuleDao.deleteById(id);
            
            // 返回成功响应
            ctx.response()
                .setStatusCode(204)
                .end();
        } catch (NumberFormatException e) {
            ctx.response()
                .setStatusCode(400)
                .putHeader("content-type", "application/json")
                .end(new io.vertx.core.json.JsonObject()
                    .put("error", "Invalid id format")
                    .encode());
        } catch (Exception e) {
            ctx.response()
                .setStatusCode(500)
                .putHeader("content-type", "application/json")
                .end(new io.vertx.core.json.JsonObject()
                    .put("error", "Failed to delete proxy client rule: " + e.getMessage())
                    .encode());
        }
    }
}
