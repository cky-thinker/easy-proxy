package com.cky.proxy.server.controller;

import com.cky.proxy.server.domain.dto.PageResult;
import com.cky.proxy.server.domain.dto.SysLogReq;
import com.cky.proxy.server.domain.entity.SysLog;
import com.cky.proxy.server.service.SysLogService;
import com.cky.proxy.server.util.RequestUtil;
import com.cky.proxy.server.util.ResponseUtil;

import cn.hutool.db.Page;
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
        SysLogReq req = RequestUtil.getParamsObj(ctx, SysLogReq.class);
        PageResult<SysLog> result = sysLogService.getSysLogsPageable(page, req.getLogType(), req.getKeyword());
        ResponseUtil.success(ctx, result);
    }

    private void getSysLogDetail(RoutingContext ctx) {
        Integer id = RequestUtil.getParamInt(ctx, "id");
        if (id == null) {
            ResponseUtil.error(ctx, 400, "Missing required parameter: id");
            return;
        }
        SysLog sysLog = sysLogService.getSysLogById(id);
        if (sysLog == null) {
            ResponseUtil.error(ctx, 404, "SysLog not found with id: " + id);
            return;
        }
        ResponseUtil.success(ctx, sysLog);
    }

    private void addSysLog(RoutingContext ctx) {
        SysLog sysLog = RequestUtil.getBodyObj(ctx, SysLog.class);
        if (sysLog == null) {
            ResponseUtil.error(ctx, 400, "Request body is required");
            return;
        }
        sysLog = sysLogService.addSysLog(sysLog);
        ResponseUtil.success(ctx, sysLog);
    }

    private void deleteSysLog(RoutingContext ctx) {
        Integer id = RequestUtil.getParamInt(ctx, "id");
        if (id == null) {
            ResponseUtil.error(ctx, 400, "Missing required parameter: id");
            return;
        }
        boolean deleted = sysLogService.deleteSysLog(id);
        if (!deleted) {
            ResponseUtil.error(ctx, 404, "Sys log not found");
            return;
        }
        ResponseUtil.success(ctx, null);
    }
}