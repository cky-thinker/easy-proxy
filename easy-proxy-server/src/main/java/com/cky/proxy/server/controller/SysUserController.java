package com.cky.proxy.server.controller;

import cn.hutool.core.util.StrUtil;

import com.cky.proxy.server.bean.dto.CaptchaImage;
import com.cky.proxy.server.bean.dto.LoginReq;
import com.cky.proxy.server.bean.dto.Result;
import com.cky.proxy.server.bean.dto.UserInfo;
import com.cky.proxy.server.service.UserService;
import com.cky.proxy.server.util.JsonUtil;
import com.cky.proxy.server.util.VertxUtil;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class SysUserController {
    private final Router router;
    private final UserService authService;

    public SysUserController(Router router, Vertx vertx) {
        this.router = router;
        this.authService = new UserService(vertx);
        initRoutes();
    }

    private void initRoutes() {
        // 生成验证码图片
        router.get("/api/sys/captchaImage").handler(this::captchaImage);
        // 用户登录
        router.post("/api/sys/loginUser").handler(this::loginUser);
    }

    private void loginUser(RoutingContext ctx) {
        // 从请求体获取JSON数据
        String body = ctx.body().asString();
        if (StrUtil.isEmpty(body)) {
            VertxUtil.response(ctx, Result.error("请求体不能为空"));
            return;
        }
        // 获取用户名、密码和验证码信息
        LoginReq loginReq = JsonUtil.parseJson(body, LoginReq.class);

        try {
            UserInfo userInfo = authService.login(loginReq);
            VertxUtil.response(ctx, Result.success(userInfo, "登录成功"));
        } catch (Exception e) {
            VertxUtil.response(ctx, Result.error("登录失败: " + e.getMessage()));
        }
    }

    private void captchaImage(RoutingContext ctx) {
        try {
            CaptchaImage captchaImage = authService.captchaImage();
            // 返回验证码信息
            VertxUtil.response(ctx, Result.success(captchaImage, "获取验证码成功"));
        } catch (Exception e) {
            VertxUtil.response(ctx, Result.error("生成验证码失败: " + e.getMessage()));
        }
    }
}
