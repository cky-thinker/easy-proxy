package com.cky.proxy.server.controller;

import cn.hutool.db.Page;
import com.cky.proxy.server.domain.dto.PageResult;
import com.cky.proxy.server.domain.entity.SysLog;
import com.cky.proxy.server.service.SysLogService;
import com.cky.proxy.server.util.BeanContext;
import com.cky.proxy.server.util.RequestUtil;
import com.cky.proxy.server.util.ResponseUtil;
import com.cky.proxy.server.http.HttpContext;
import com.cky.proxy.server.http.HttpRouter;

import static com.cky.proxy.server.util.RequestUtil.getParam;

public class SysLogController {
    private final HttpRouter router;
    private final SysLogService sysLogService;

    public SysLogController(HttpRouter router) {
        this.router = router;
        this.sysLogService = BeanContext.getSysLogService();
        initRoutes();
    }

    private void initRoutes() {
        // 分页查询日志
        router.get("/api/sysLog", this::getSysLogsPageable);
    }

    private void getSysLogsPageable(HttpContext ctx) {
        Page page = RequestUtil.getPage(ctx);
        PageResult<SysLog> result = sysLogService.getSysLogsPageable(page, getParam(ctx, "logType"), getParam(ctx, "keyword"));
        ResponseUtil.success(ctx, result);
    }
}
