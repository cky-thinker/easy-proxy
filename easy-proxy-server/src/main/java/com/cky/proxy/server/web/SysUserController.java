package com.cky.proxy.server.web;

import io.vertx.ext.web.Router;

public class SysUserController {
    private final Router router;

    public SysUserController(Router router) {
        this.router = router;
        initRoutes();
    }

    private void initRoutes() {
        router.get("/api/sys-users").handler(ctx -> {
            ctx.response().end("获取所有系统用户");
        });
    }
}
