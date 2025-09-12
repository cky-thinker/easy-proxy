package com.cky.proxy.server.controller;

import java.util.List;

import com.cky.proxy.server.dao.ProxyClientGroupDao;
import com.cky.proxy.server.util.VertxUtil;

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
        router.delete("/api/proxyClientGroup/:id").handler(this::deleteProxyClientGroup);
    }

    private void getAllProxyClientGroup(RoutingContext ctx) {
        try {
            // 获取查询参数
            String name = ctx.request().getParam("name");
            
            // 执行查询
            List<com.cky.proxy.server.domain.entity.ProxyClientGroup> groups = proxyClientGroupDao.selectList(qb -> {
                if (name != null && !name.isEmpty()) {
                    qb.where().like("name", "%" + name + "%");
                }
            });
            
            // 返回结果
            VertxUtil.success(ctx, groups);
        } catch (Exception e) {
            VertxUtil.error(ctx, 500, "Failed to get proxy client groups: " + e.getMessage());
        }
    }

    private void addProxyClientGroup(RoutingContext ctx) {
        try {
            // 从请求体获取JSON数据
            io.vertx.core.json.JsonObject body = ctx.getBodyAsJson();
            if (body == null) {
                VertxUtil.error(ctx, 400, "Request body is required");
                return;
            }
            
            // 创建ProxyClientGroup对象
            com.cky.proxy.server.domain.entity.ProxyClientGroup group = new com.cky.proxy.server.domain.entity.ProxyClientGroup();
            group.setName(body.getString("name"));
            group.setCreateBy(body.getString("createBy", "system"));
            group.setCreateTime(new java.sql.Date(System.currentTimeMillis()));
            
            // 保存到数据库
            proxyClientGroupDao.insert(group);
            
            // 返回成功响应
            ctx.response()
                .setStatusCode(201)
                .putHeader("content-type", "application/json")
                .end(io.vertx.core.json.Json.encode(group));
        } catch (Exception e) {
            VertxUtil.error(ctx, 500, "Failed to add proxy client group: " + e.getMessage());
        }
    }

    private void updateProxyClientGroup(RoutingContext ctx) {
        try {
            // 从请求体获取JSON数据
            io.vertx.core.json.JsonObject body = ctx.getBodyAsJson();
            if (body == null || !body.containsKey("id")) {
                VertxUtil.error(ctx, 400, "Request body with id is required");
                return;
            }
            
            Integer id = body.getInteger("id");
            com.cky.proxy.server.domain.entity.ProxyClientGroup existingGroup = proxyClientGroupDao.selectById(id);
            
            if (existingGroup == null) {
                VertxUtil.error(ctx, 404, "ProxyClientGroup not found with id: " + id);
                return;
            }
            
            // 更新字段
            if (body.containsKey("name")) {
                existingGroup.setName(body.getString("name"));
            }
            existingGroup.setUpdateBy(body.getString("updateBy", "system"));
            existingGroup.setUpdateTime(new java.sql.Date(System.currentTimeMillis()));
            
            // 保存到数据库
            proxyClientGroupDao.updateById(existingGroup);
            
            // 返回成功响应
            VertxUtil.success(ctx, existingGroup);
        } catch (Exception e) {
            VertxUtil.error(ctx, 500, "Failed to update proxy client group: " + e.getMessage());
        }
    }

    private void deleteProxyClientGroup(RoutingContext ctx) {
        try {
            // 获取ID参数
            String idParam = ctx.request().getParam("id");
            if (idParam == null || idParam.isEmpty()) {
                VertxUtil.error(ctx, 400, "Missing required parameter: id");
                return;
            }
            
            Integer id = Integer.parseInt(idParam);
            com.cky.proxy.server.domain.entity.ProxyClientGroup existingGroup = proxyClientGroupDao.selectById(id);
            
            if (existingGroup == null) {
                VertxUtil.error(ctx, 404, "ProxyClientGroup not found with id: " + id);
                return;
            }
            
            // 从数据库删除
            proxyClientGroupDao.deleteById(id);
            
            // 返回成功响应
            VertxUtil.success(ctx, null);
        } catch (NumberFormatException e) {
            VertxUtil.error(ctx, 400, "Invalid id format");
        } catch (Exception e) {
            VertxUtil.error(ctx, 500, "Failed to delete proxy client group: " + e.getMessage());
        }
    }
}
