package com.cky.proxy.server.web;

import com.cky.proxy.server.bean.dto.PageResult;
import com.cky.proxy.server.bean.entity.ProxyClient;
import com.cky.proxy.server.dao.ProxyClientDao;
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
        // 添加客户端
        router.post("/api/proxyClient").handler(this::addProxyClient);
        // 更新客户端
        router.put("/api/proxyClient/:id").handler(this::updateProxyClient);
        // 删除客户端
        router.delete("/api/proxyClient/:id").handler(this::deleteProxyClient);
    }

    private void getProxyClientsPageable(RoutingContext ctx) {
        // 获取分页参数
        int page = Integer.parseInt(ctx.request().getParam("page", "1"));
        int pageSize = Integer.parseInt(ctx.request().getParam("pageSize", "10"));
        
        // 创建分页对象
        cn.hutool.db.Page hutoolPage = new cn.hutool.db.Page(page, pageSize);
        
        // 获取排序参数
        String sortField = ctx.request().getParam("sortField");
        String sortOrder = ctx.request().getParam("sortOrder");
        if (sortField != null && !sortField.isEmpty()) {
            cn.hutool.db.sql.Direction direction = "desc".equalsIgnoreCase(sortOrder) ? 
                cn.hutool.db.sql.Direction.DESC : cn.hutool.db.sql.Direction.ASC;
            hutoolPage.addOrder(new cn.hutool.db.sql.Order(sortField, direction));
        }
        
        // 执行分页查询
        PageResult<ProxyClient> result = proxyClientDao.selectPage(
            hutoolPage, 
            where -> {
                // 获取查询参数
                String name = ctx.request().getParam("name");
                String token = ctx.request().getParam("token");
                Integer groupId = ctx.request().getParam("groupId") != null ? 
                    Integer.parseInt(ctx.request().getParam("groupId")) : null;
                
                // 构建查询条件
                if (name != null && !name.isEmpty()) {
                    where.like("name", "%" + name + "%");
                }
                if (token != null && !token.isEmpty()) {
                    where.eq("token", token);
                }
                if (groupId != null) {
                    where.eq("groupId", groupId);
                }
            }
        );
        
        // 返回结果
        ctx.response()
            .putHeader("content-type", "application/json")
            .end(io.vertx.core.json.Json.encode(result));
    }

    private void getProxyClientDetail(RoutingContext ctx) {
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
        
        try {
            Integer id = Integer.parseInt(idParam);
            com.cky.proxy.server.bean.entity.ProxyClient proxyClient = proxyClientDao.selectById(id);
            
            if (proxyClient == null) {
                ctx.response()
                    .setStatusCode(404)
                    .putHeader("content-type", "application/json")
                    .end(new io.vertx.core.json.JsonObject()
                        .put("error", "ProxyClient not found with id: " + id)
                        .encode());
                return;
            }
            
            ctx.response()
                .putHeader("content-type", "application/json")
                .end(io.vertx.core.json.Json.encode(proxyClient));
        } catch (NumberFormatException e) {
            ctx.response()
                .setStatusCode(400)
                .putHeader("content-type", "application/json")
                .end(new io.vertx.core.json.JsonObject()
                    .put("error", "Invalid id format: " + idParam)
                    .encode());
        }
    }

    private void addProxyClient(RoutingContext ctx) {
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
            
            // 创建ProxyClient对象
            com.cky.proxy.server.bean.entity.ProxyClient proxyClient = new com.cky.proxy.server.bean.entity.ProxyClient();
            proxyClient.setName(body.getString("name"));
            proxyClient.setToken(body.getString("token"));
            proxyClient.setGroupId(body.getInteger("groupId", 0));
            proxyClient.setStatus("offline"); // 默认离线状态
            proxyClient.setEnableFlag(body.getBoolean("enableFlag", true));
            proxyClient.setCreateBy(body.getString("createBy", "system"));
            proxyClient.setCreateTime(java.time.LocalDateTime.now());
            
            // 保存到数据库
            proxyClientDao.insert(proxyClient);
            
            // 返回成功响应
            ctx.response()
                .setStatusCode(201)
                .putHeader("content-type", "application/json")
                .end(io.vertx.core.json.Json.encode(proxyClient));
        } catch (Exception e) {
            ctx.response()
                .setStatusCode(500)
                .putHeader("content-type", "application/json")
                .end(new io.vertx.core.json.JsonObject()
                    .put("error", "Failed to add proxy client: " + e.getMessage())
                    .encode());
        }
    }

    private void updateProxyClient(RoutingContext ctx) {
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
            com.cky.proxy.server.bean.entity.ProxyClient existingClient = proxyClientDao.selectById(id);
            
            if (existingClient == null) {
                ctx.response()
                    .setStatusCode(404)
                    .putHeader("content-type", "application/json")
                    .end(new io.vertx.core.json.JsonObject()
                        .put("error", "ProxyClient not found with id: " + id)
                        .encode());
                return;
            }
            
            // 更新字段
            if (body.containsKey("name")) {
                existingClient.setName(body.getString("name"));
            }
            if (body.containsKey("token")) {
                existingClient.setToken(body.getString("token"));
            }
            if (body.containsKey("groupId")) {
                existingClient.setGroupId(body.getInteger("groupId"));
            }
            if (body.containsKey("enableFlag")) {
                existingClient.setEnableFlag(body.getBoolean("enableFlag"));
            }
            existingClient.setUpdateBy(body.getString("updateBy", "system"));
            existingClient.setUpdateTime(java.time.LocalDateTime.now());
            
            // 保存到数据库
            proxyClientDao.updateById(existingClient);
            
            // 返回成功响应
            ctx.response()
                .putHeader("content-type", "application/json")
                .end(io.vertx.core.json.Json.encode(existingClient));
        } catch (Exception e) {
            ctx.response()
                .setStatusCode(500)
                .putHeader("content-type", "application/json")
                .end(new io.vertx.core.json.JsonObject()
                    .put("error", "Failed to update proxy client: " + e.getMessage())
                    .encode());
        }
    }

    private void deleteProxyClient(RoutingContext ctx) {
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
            ProxyClient existingClient = proxyClientDao.selectById(id);
            
            if (existingClient == null) {
                ctx.response()
                    .setStatusCode(404)
                    .putHeader("content-type", "application/json")
                    .end(new io.vertx.core.json.JsonObject()
                        .put("error", "ProxyClient not found with id: " + id)
                        .encode());
                return;
            }
            
            // 从数据库删除
            proxyClientDao.deleteById(id);
            
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
                    .put("error", "Failed to delete proxy client: " + e.getMessage())
                    .encode());
        }
    }
}
