package com.cky.proxy.server.controller;

import cn.hutool.db.Page;
import com.cky.proxy.server.domain.dto.PageResult;
import com.cky.proxy.server.domain.entity.SysLog;
import com.cky.proxy.server.service.SysLogService;
import com.cky.proxy.server.util.RequestUtil;
import com.cky.proxy.server.util.ResponseUtil;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import static com.cky.proxy.server.util.RequestUtil.getParam;

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
    }

    private void getSysLogsPageable(RoutingContext ctx) {
        Page page = RequestUtil.getPage(ctx);
        PageResult<SysLog> result = sysLogService.getSysLogsPageable(page, getParam(ctx, "logType"), getParam(ctx, "keyword"));
        ResponseUtil.success(ctx, result);
    }
}
