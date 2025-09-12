package com.cky.proxy.server.controller;

import java.util.Date;

import com.cky.proxy.server.dao.ProxyClientDao;
import com.cky.proxy.server.domain.dto.PageResult;
import com.cky.proxy.server.domain.dto.Result;
import com.cky.proxy.server.domain.entity.ProxyClient;
import com.cky.proxy.server.util.PageUtil;
import com.cky.proxy.server.util.VertxUtil;

import cn.hutool.core.date.DateUtil;
import cn.hutool.db.Page;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
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
        // 查询明细
        router.get("/api/proxyClient/:id").handler(this::getProxyClientDetail);
        // 添加客户端
        router.post("/api/proxyClient").handler(this::addProxyClient);
        // 更新客户端
        router.put("/api/proxyClient/:id").handler(this::updateProxyClient);
        // 删除客户端
        router.delete("/api/proxyClient/:id").handler(this::deleteProxyClient);
    }

    private void getProxyClientsPageable(RoutingContext ctx) {
        // 创建分页对象
        Page hutoolPage = PageUtil.getPage(ctx);

        // 执行分页查询
        PageResult<ProxyClient> result = proxyClientDao.selectPage(
                hutoolPage,
                where -> {
                    // 获取查询参数
                    String name = ctx.request().getParam("name");
                    String token = ctx.request().getParam("token");
                    Integer groupId = ctx.request().getParam("groupId") != null
                            ? Integer.parseInt(ctx.request().getParam("groupId"))
                            : null;

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
                });

        // 返回结果
        ctx.response()
                .putHeader("content-type", "application/json")
                .end(Json.encode(result));
    }

    private void getProxyClientDetail(RoutingContext ctx) {
        // 获取ID参数
        String idParam = ctx.request().getParam("id");
        if (idParam == null || idParam.isEmpty()) {
            ctx.response()
                    .setStatusCode(400)
                    .putHeader("content-type", "application/json")
                    .end(new JsonObject()
                            .put("error", "Missing required parameter: id")
                            .encode());
            return;
        }

        try {
            Integer id = Integer.parseInt(idParam);
            ProxyClient proxyClient = proxyClientDao.selectById(id);

            if (proxyClient == null) {
                ctx.response()
                        .setStatusCode(404)
                        .putHeader("content-type", "application/json")
                        .end(new JsonObject()
                                .put("error", "ProxyClient not found with id: " + id)
                                .encode());
                return;
            }

            VertxUtil.response(ctx, Result.success(proxyClient));
        } catch (NumberFormatException e) {
            ctx.response()
                    .setStatusCode(400)
                    .putHeader("content-type", "application/json")
                    .end(new JsonObject()
                            .put("error", "Invalid id format: " + idParam)
                            .encode());

        }
    }

    private void addProxyClient(RoutingContext ctx) {
        try {
            // 从请求体获取JSON数据
            JsonObject body = ctx.body().asJsonObject();
            if (body == null) {
                VertxUtil.error(ctx, 400, "Request body is required");
                return;
            }

            // 创建ProxyClient对象
            ProxyClient proxyClient = new ProxyClient();
            proxyClient.setName(body.getString("name"));
            proxyClient.setToken(body.getString("token"));
            proxyClient.setGroupId(body.getInteger("groupId", 0));
            proxyClient.setStatus("offline"); // 默认离线状态
            proxyClient.setEnableFlag(body.getBoolean("enableFlag", true));
            proxyClient.setCreateBy(body.getString("createBy", "system"));
            proxyClient.setCreateTime(new Date());

            // 保存到数据库
            proxyClientDao.insert(proxyClient);

            // 返回成功响应
            VertxUtil.success(ctx, proxyClient);
        } catch (Exception e) {
            VertxUtil.error(ctx, "Failed to add proxy client: " + e.getMessage());
        }
    }

    private void updateProxyClient(RoutingContext ctx) {
        try {
            // 从请求体获取JSON数据
            JsonObject body = ctx.body().asJsonObject();
            if (body == null || !body.containsKey("id")) {
                VertxUtil.error(ctx, 400, "Request body with id is required");
                return;
            }

            Integer id = body.getInteger("id");
            ProxyClient existingClient = proxyClientDao.selectById(id);

            if (existingClient == null) {
                VertxUtil.error(ctx, 404, "ProxyClient not found with id: " + id);
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
            existingClient.setUpdateTime(new Date());

            // 保存到数据库
            proxyClientDao.updateById(existingClient);

            // 返回成功响应
            VertxUtil.success(ctx, existingClient);
        } catch (Exception e) {
            VertxUtil.error(ctx, 500, "Failed to update proxy client: " + e.getMessage());
        }
    }

    private void deleteProxyClient(RoutingContext ctx) {
        try {
            // 获取ID参数
            String idParam = ctx.request().getParam("id");
            if (idParam == null || idParam.isEmpty()) {
                VertxUtil.error(ctx, 400, "Missing required parameter: id");
                return;
            }

            Integer id = Integer.parseInt(idParam);
            ProxyClient existingClient = proxyClientDao.selectById(id);

            if (existingClient == null) {
                VertxUtil.error(ctx, 404, "ProxyClient not found with id: " + id);
                return;
            }

            // 从数据库删除
            proxyClientDao.deleteById(id);

            // 返回成功响应
            ctx.response()
                    .setStatusCode(204)
                    .end();
        } catch (NumberFormatException e) {
            VertxUtil.error(ctx, 400, "Invalid id format");
        } catch (Exception e) {
            VertxUtil.error(ctx, 500, "Failed to delete proxy client: " + e.getMessage());
        }
    }
}
