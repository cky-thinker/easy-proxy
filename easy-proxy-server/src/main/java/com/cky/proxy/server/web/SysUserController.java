package com.cky.proxy.server.web;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class SysUserController {
    private final Router router;

    public SysUserController(Router router) {
        this.router = router;
        initRoutes();
    }

    private void initRoutes() {
        // 生成验证码图片
        router.get("/api/captchaImage").handler(this::captchaImage);
        // 用户登录
        router.get("/api/loginUser").handler(this::loginUser);
    }

    private void loginUser(RoutingContext routingcontext1) {
    }

    private void captchaImage(RoutingContext routingcontext1) {
    }
}
