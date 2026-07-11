package com.cky.proxy.server.controller;

import com.cky.proxy.server.config.ConfigProperty;
import com.cky.proxy.server.domain.dto.CaptchaImage;
import com.cky.proxy.server.domain.dto.CreateUserReq;
import com.cky.proxy.server.domain.dto.InitUserReq;
import com.cky.proxy.server.domain.dto.LoginConfigResp;
import com.cky.proxy.server.domain.dto.LoginReq;
import com.cky.proxy.server.domain.dto.PageResult;
import com.cky.proxy.server.domain.dto.Result;
import com.cky.proxy.server.domain.dto.ResetPasswordReq;
import com.cky.proxy.server.domain.dto.UserInfo;
import com.cky.proxy.server.domain.dto.UserView;
import com.cky.proxy.server.domain.entity.SysUser;
import com.cky.proxy.server.http.HttpContext;
import com.cky.proxy.server.http.HttpRouter;
import com.cky.proxy.server.service.UserService;
import com.cky.proxy.server.util.BeanContext;
import com.cky.proxy.server.util.JsonUtil;
import com.cky.proxy.server.util.RequestUtil;
import com.cky.proxy.server.util.ResponseUtil;

import lombok.SneakyThrows;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;

public class UserController {
    private final HttpRouter router;
    private final UserService authService;

    public UserController(HttpRouter router) {
        this.router = router;
        this.authService = BeanContext.getUserService();
        initRoutes();
    }

    private void initRoutes() {
        router.get("/api/open/captchaImage", this::captchaImage);
        router.get("/api/open/checkInit", this::checkInit);
        router.post("/api/open/initUser", this::initUser);
        router.post("/api/open/loginUser", this::loginUser);
        router.get("/api/open/loginConfig", this::getConfig);

        router.get("/api/users", this::getUsersPageable);
        router.get("/api/users/detail", this::getUserDetail);
        router.post("/api/users", this::addUser);
        router.put("/api/users", this::updateUser);
        router.delete("/api/users", this::deleteUser);
        router.post("/api/users/reset-password", this::resetPassword);
        router.put("/api/users/enableFlag", this::updateEnableFlag);
        router.get("/api/users/permissions", this::getPermissions);
    }

    @SneakyThrows
    private void loginUser(HttpContext ctx) {
        String body = ctx.getBodyAsString();
        if (StrUtil.isEmpty(body)) {
            ResponseUtil.response(ctx, Result.error("请求体不能为空"));
            return;
        }

        LoginReq loginReq = JsonUtil.parseJson(body, LoginReq.class);
        UserInfo userInfo = authService.login(loginReq, ctx.getClientIp());
        ResponseUtil.response(ctx, Result.success(userInfo, "登录成功"));
    }

    @SneakyThrows
    private void captchaImage(HttpContext ctx) {
        CaptchaImage captchaImage = authService.captchaImage();
        ResponseUtil.response(ctx, Result.success(captchaImage, "获取验证码成功"));
    }

    @SneakyThrows
    private void checkInit(HttpContext ctx) {
        boolean needInit = authService.checkInit();
        ResponseUtil.response(ctx, Result.success(needInit, "检查初始化状态成功"));
    }

    @SneakyThrows
    private void initUser(HttpContext ctx) {
        InitUserReq user = RequestUtil.getBodyObj(ctx, InitUserReq.class);
        if (user == null) {
            ResponseUtil.error(ctx, 400, "请求体不能为空");
            return;
        }
        UserView created = authService.initAdmin(user);
        ResponseUtil.response(ctx, Result.success(created, "初始化系统管理员成功"));
    }

    @SneakyThrows
    private void getConfig(HttpContext ctx) {
        LoginConfigResp resp = new LoginConfigResp();
        resp.setCaptchaImageEnable(ConfigProperty.getInstance().getServer().getCaptchaImageEnable());
        resp.setPasswordEncryptEnable(Boolean.TRUE);
        resp.setPasswordPublicKey(authService.getPasswordPublicKey());
        ResponseUtil.response(ctx, Result.success(resp, "获取配置成功"));
    }

    @SneakyThrows
    private void getUsersPageable(HttpContext ctx) {
        Boolean enableFlag = RequestUtil.getParamBool(ctx, "enableFlag");
        PageResult<UserView> pageResult = authService.getUsersPageable(RequestUtil.getPage(ctx), ctx.getParam("q"),
                enableFlag);
        ResponseUtil.success(ctx, pageResult);
    }

    @SneakyThrows
    private void getUserDetail(HttpContext ctx) {
        Integer id = RequestUtil.getParamInt(ctx, "id");
        if (id == null) {
            ResponseUtil.error(ctx, 400, "缺少参数: id");
            return;
        }
        UserView user = authService.getUserById(id);
        if (user == null) {
            ResponseUtil.error(ctx, 404, "账号不存在");
            return;
        }
        ResponseUtil.success(ctx, user);
    }

    @SneakyThrows
    private void addUser(HttpContext ctx) {
        CreateUserReq user = RequestUtil.getBodyObj(ctx, CreateUserReq.class);
        if (user == null) {
            ResponseUtil.error(ctx, 400, "请求体不能为空");
            return;
        }
        UserView created = authService.createUser(user);
        ResponseUtil.success(ctx, created);
    }

    @SneakyThrows
    private void updateUser(HttpContext ctx) {
        SysUser user = RequestUtil.getBodyObj(ctx, SysUser.class);
        if (user == null) {
            ResponseUtil.error(ctx, 400, "请求体不能为空");
            return;
        }
        if (user.getId() == null) {
            ResponseUtil.error(ctx, 400, "请求体缺少 id");
            return;
        }
        UserView updated = authService.updateUser(user);
        ResponseUtil.success(ctx, updated);
    }

    @SneakyThrows
    private void resetPassword(HttpContext ctx) {
        ResetPasswordReq req = RequestUtil.getBodyObj(ctx, ResetPasswordReq.class);
        if (req == null) {
            ResponseUtil.error(ctx, 400, "请求体不能为空");
            return;
        }
        if (req.getId() == null || req.getEncryptedPassword() == null) {
            ResponseUtil.error(ctx, 400, "请求体缺少 id 或 encryptedPassword");
            return;
        }
        UserView updated = authService.resetPassword(req.getId(), req.getEncryptedPassword());
        ResponseUtil.success(ctx, updated);
    }

    @SneakyThrows
    private void deleteUser(HttpContext ctx) {
        Integer id = RequestUtil.getParamInt(ctx, "id");
        if (id == null) {
            ResponseUtil.error(ctx, 400, "缺少参数: id");
            return;
        }
        boolean ok = authService.deleteUser(id);
        if (!ok) {
            ResponseUtil.error(ctx, 404, "删除失败");
            return;
        }
        ResponseUtil.success(ctx, null);
    }

    @SneakyThrows
    private void updateEnableFlag(HttpContext ctx) {
        SysUser sysUser = RequestUtil.getBodyObj(ctx, SysUser.class);
        if (sysUser == null) {
            ResponseUtil.error(ctx, 400, "请求体不能为空");
            return;
        }
        Integer id = sysUser.getId();
        if (id == null) {
            ResponseUtil.error(ctx, 400, "请求体缺少 id");
            return;
        }
        Boolean enableFlag = sysUser.getEnableFlag();
        if (enableFlag == null) {
            ResponseUtil.error(ctx, 400, "请求体缺少 enableFlag");
            return;
        }
        UserView user = authService.updateEnableFlag(id, enableFlag);
        ResponseUtil.success(ctx, user);
    }

    private void getPermissions(HttpContext ctx) {
        JSONArray list = new JSONArray();
        list.add(new JSONObject().set("name", "总览管理").set("description", "查看系统总览和统计信息")
                .set("actions", new JSONArray().set("查看").set("导出")));
        list.add(new JSONObject().set("name", "客户端管理").set("description", "管理代理客户端和配置")
                .set("actions", new JSONArray().set("查看").set("新增").set("编辑").set("删除").set("启用/禁用")));
        list.add(new JSONObject().set("name", "账号管理").set("description", "管理系统用户账号")
                .set("actions", new JSONArray().set("查看").set("新增").set("编辑").set("删除").set("权限管理")));
        list.add(new JSONObject().set("name", "日志管理").set("description", "查看系统日志和审计记录")
                .set("actions", new JSONArray().set("查看").set("导出").set("清理")));
        list.add(new JSONObject().set("name", "系统设置").set("description", "管理系统配置和参数")
                .set("actions", new JSONArray().set("查看").set("修改")));
        ResponseUtil.success(ctx, list);
    }
}
