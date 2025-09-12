package com.cky.proxy.server.controller;

import java.util.Date;

import com.cky.proxy.server.service.ProxyClientService;
import com.cky.proxy.server.domain.dto.PageResult;
import com.cky.proxy.server.domain.dto.Result;
import com.cky.proxy.server.domain.entity.ProxyClient;
import com.cky.proxy.server.util.PageUtil;
import com.cky.proxy.server.util.VertxUtil;

import cn.hutool.db.Page;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class ProxyClientController {
    private final Router router;
    private final ProxyClientService proxyClientService;

    public ProxyClientController(Router router) {
        this.router = router;
        this.proxyClientService = new ProxyClientService();
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
        Page page = PageUtil.getPage(ctx);

        // 获取查询参数
        String name = ctx.request().getParam("name");
        Integer groupId = ctx.request().getParam("groupId") != null
                ? Integer.parseInt(ctx.request().getParam("groupId"))
                : null;

        // 执行分页查询
        PageResult<ProxyClient> result = proxyClientService.getProxyClientsPageable(page, name, groupId);

        // 返回结果
        VertxUtil.success(ctx, result);
    }

    private void getProxyClientDetail(RoutingContext ctx) {
        // 获取ID参数
        String idParam = ctx.request().getParam("id");
        if (idParam == null || idParam.isEmpty()) {
            VertxUtil.error(ctx, 400, "Missing required parameter: id");
            return;
        }

        try {
            Integer id = Integer.parseInt(idParam);
            ProxyClient proxyClient = proxyClientService.getProxyClientById(id);

            if (proxyClient == null) {
                VertxUtil.error(ctx, 404, "ProxyClient not found with id: " + id);
                return;
            }

            VertxUtil.response(ctx, Result.success(proxyClient));
        } catch (NumberFormatException e) {
            VertxUtil.error(ctx, 400, "Invalid id format: " + idParam);
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
            proxyClient.setStatus("offline"); // 默认离线状态
            proxyClient.setEnableFlag(body.getBoolean("enableFlag", true));
            proxyClient.setCreateBy(body.getString("createBy", "system"));
            proxyClient.setCreateTime(new Date());

            // 保存到数据库
            proxyClientService.addProxyClient(proxyClient);

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

            ProxyClient proxyClient = new ProxyClient();
            proxyClient.setId(body.getInteger("id"));
            proxyClient.setName(body.getString("name"));
            proxyClient.setToken(body.getString("token"));
            proxyClient.setEnableFlag(body.getBoolean("enableFlag", true));
            // 保存到数据库
            proxyClient = proxyClientService.updateProxyClient(proxyClient);
            // 返回成功响应
            VertxUtil.success(ctx, proxyClient);
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
            // 从数据库删除
            boolean deleted = proxyClientService.deleteProxyClient(id);
            if (!deleted) {
                VertxUtil.error(ctx, 404, "Proxy client not found");
                return;
            }

            // 返回成功响应
            VertxUtil.success(ctx, null);
        } catch (NumberFormatException e) {
            VertxUtil.error(ctx, 400, "Invalid id format");
        } catch (Exception e) {
            VertxUtil.error(ctx, 500, "Failed to delete proxy client: " + e.getMessage());
        }
    }
}
