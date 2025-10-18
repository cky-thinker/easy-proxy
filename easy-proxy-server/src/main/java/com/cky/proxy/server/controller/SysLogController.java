package com.cky.proxy.server.controller;

import com.cky.proxy.server.domain.dto.PageResult;
import com.cky.proxy.server.domain.dto.Result;
import com.cky.proxy.server.domain.entity.SysLog;
import com.cky.proxy.server.service.SysLogService;
import com.cky.proxy.server.util.RequestUtil;
import com.cky.proxy.server.util.ResponseUtil;

import cn.hutool.db.Page;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class SysLogController {
    private final Router router;
    private final SysLogService sysLogService;

    public SysLogController(Router router) {
        this.router = router;
        this.sysLogService = new SysLogService();
        initRoutes();
    }

    private void initRoutes() {
        // 分页查询系统日志
        router.get("/api/sysLog").handler(this::getSysLogsPageable);
        // 查询日志详情
        router.get("/api/sysLog/:id").handler(this::getSysLogDetail);
        // 添加系统日志
        router.post("/api/sysLog").handler(this::addSysLog);
        // 删除系统日志
        router.delete("/api/sysLog/:id").handler(this::deleteSysLog);
    }

    private void getSysLogsPageable(RoutingContext ctx) {
        Page page = RequestUtil.getPage(ctx);
        String logType = ctx.request().getParam("logType");
        String keyword = ctx.request().getParam("keyword");
        PageResult<SysLog> result = sysLogService.getSysLogsPageable(page, logType, keyword);
        ResponseUtil.success(ctx, result);
    }

    private void getSysLogDetail(RoutingContext ctx) {
        String idParam = ctx.request().getParam("id");
        if (idParam == null || idParam.isEmpty()) {
            ResponseUtil.error(ctx, 400, "Missing required parameter: id");
            return;
        }
        try {
            Integer id = Integer.parseInt(idParam);
            SysLog sysLog = sysLogService.getSysLogById(id);
            if (sysLog == null) {
                ResponseUtil.error(ctx, 404, "SysLog not found with id: " + id);
                return;
            }
            ResponseUtil.response(ctx, Result.success(sysLog));
        } catch (NumberFormatException e) {
            ResponseUtil.error(ctx, 400, "Invalid id format: " + idParam);
        }
    }

    private void addSysLog(RoutingContext ctx) {
        try {
            JsonObject body = ctx.body().asJsonObject();
            if (body == null) {
                ResponseUtil.error(ctx, 400, "Request body is required");
                return;
            }
            SysLog sysLog = new SysLog();
            sysLog.setLogType(body.getString("logType"));
            sysLog.setLogContent(body.getString("logContent"));
            // createTime 在服务层默认填充
            sysLog = sysLogService.addSysLog(sysLog);
            ResponseUtil.success(ctx, sysLog);
        } catch (Exception e) {
            ResponseUtil.error(ctx, 500, "Failed to add sys log: " + e.getMessage());
        }
    }

    private void deleteSysLog(RoutingContext ctx) {
        try {
            String idParam = ctx.request().getParam("id");
            if (idParam == null || idParam.isEmpty()) {
                ResponseUtil.error(ctx, 400, "Missing required parameter: id");
                return;
            }
            Integer id = Integer.parseInt(idParam);
            boolean deleted = sysLogService.deleteSysLog(id);
            if (!deleted) {
                ResponseUtil.error(ctx, 404, "Sys log not found");
                return;
            }
            ResponseUtil.success(ctx, null);
        } catch (NumberFormatException e) {
            ResponseUtil.error(ctx, 400, "Invalid id format");
        } catch (Exception e) {
            ResponseUtil.error(ctx, 500, "Failed to delete sys log: " + e.getMessage());
        }
    }
}