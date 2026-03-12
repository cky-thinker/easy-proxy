package com.cky.proxy.server.controller;

import java.util.HashMap;

import com.cky.proxy.server.config.ConfigProperty;
import com.cky.proxy.server.domain.dto.CaptchaImage;
import com.cky.proxy.server.domain.dto.LoginReq;
import com.cky.proxy.server.domain.dto.PageResult;
import com.cky.proxy.server.domain.dto.Result;
import com.cky.proxy.server.domain.dto.UserInfo;
import com.cky.proxy.server.domain.entity.SysUser;
import com.cky.proxy.server.service.UserService;
import com.cky.proxy.server.util.JsonUtil;
import com.cky.proxy.server.util.RequestUtil;
import com.cky.proxy.server.util.ResponseUtil;

import lombok.SneakyThrows;

import cn.hutool.core.util.StrUtil;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class UserController {
    private final Router router;
    private final UserService authService;

    public UserController(Router router, Vertx vertx) {
        this.router = router;
        this.authService = new UserService(vertx);
        initRoutes();
    }

    private void initRoutes() {
        // 生成验证码图片
        router.get("/api/open/captchaImage").handler(this::captchaImage);
        // 系统初始化检查
        router.get("/api/open/checkInit").handler(this::checkInit);
        // 系统初始化
        router.post("/api/open/initUser").handler(this::initUser);
        // 用户登录
        router.post("/api/open/loginUser").handler(this::loginUser);
        router.get("/api/open/loginConfig").handler(this::getConfig);

        // 用户管理路由
        router.get("/api/users").handler(this::getUsersPageable);
        router.get("/api/users/detail").handler(this::getUserDetail);
        router.post("/api/users").handler(this::addUser);
        router.put("/api/users").handler(this::updateUser);
        router.delete("/api/users").handler(this::deleteUser);
        router.post("/api/users/reset-password").handler(this::resetPassword);
        router.put("/api/users/enableFlag").handler(this::updateEnableFlag);
        router.get("/api/users/permissions").handler(this::getPermissions);
    }

    @SneakyThrows
    private void loginUser(RoutingContext ctx) {
        // 从请求体获取JSON数据
        String body = ctx.body().asString();
        if (StrUtil.isEmpty(body)) {
            ResponseUtil.response(ctx, Result.error("请求体不能为空"));
            return;
        }
        // 获取用户名、密码和验证码信息
        LoginReq loginReq = JsonUtil.parseJson(body, LoginReq.class);

        UserInfo userInfo = authService.login(loginReq);
        ResponseUtil.response(ctx, Result.success(userInfo, "登录成功"));
    }

    @SneakyThrows
    private void captchaImage(RoutingContext ctx) {
        CaptchaImage captchaImage = authService.captchaImage();
        // 返回验证码信息
        ResponseUtil.response(ctx, Result.success(captchaImage, "获取验证码成功"));
    }

    @SneakyThrows
    private void checkInit(RoutingContext ctx) {
        boolean needInit = authService.checkInit();
        ResponseUtil.response(ctx, Result.success(needInit, "检查初始化状态成功"));
    }

    @SneakyThrows
    private void initUser(RoutingContext ctx) {
        SysUser user = RequestUtil.getBodyObj(ctx, SysUser.class);
        if (user == null) {
            ResponseUtil.error(ctx, 400, "请求体不能为空");
            return;
        }
        SysUser created = authService.initAdmin(user);
        ResponseUtil.response(ctx, Result.success(created, "初始化系统管理员成功"));
    }

    @SneakyThrows
    private void getConfig(RoutingContext routingcontext1) {
        ConfigProperty configProperty = ConfigProperty.getInstance();
        HashMap<String, Object> map = new HashMap<>();
        map.put("captchaImageEnable", configProperty.getServer().getCaptchaImageEnable());
        ResponseUtil.response(routingcontext1, Result.success(map, "获取配置成功"));
    }

    // ===== 账户管理 =====

    @SneakyThrows
    private void getUsersPageable(RoutingContext ctx) {
        Boolean enableFlag = RequestUtil.getParamBool(ctx, "enableFlag");
        PageResult<SysUser> pageResult = authService.getUsersPageable(RequestUtil.getPage(ctx),
                ctx.request().getParam("q"), enableFlag);

        ResponseUtil.success(ctx, pageResult);
    }

    @SneakyThrows
    private void getUserDetail(RoutingContext ctx) {
        Integer id = RequestUtil.getParamInt(ctx, "id");
        if (id == null) {
            ResponseUtil.error(ctx, 400, "缺少参数: id");
            return;
        }
        SysUser user = authService.getUserById(id);
        if (user == null) {
            ResponseUtil.error(ctx, 404, "账号不存在");
            return;
        }
        ResponseUtil.success(ctx, user);
    }

    @SneakyThrows
    private void addUser(RoutingContext ctx) {
        SysUser user = RequestUtil.getBodyObj(ctx, SysUser.class);
        if (user == null) {
            ResponseUtil.error(ctx, 400, "请求体不能为空");
            return;
        }
        SysUser created = authService.createUser(user);
        ResponseUtil.success(ctx, created);
    }

    @SneakyThrows
    private void updateUser(RoutingContext ctx) {
        SysUser user = RequestUtil.getBodyObj(ctx, SysUser.class);
        if (user == null) {
            ResponseUtil.error(ctx, 400, "请求体不能为空");
            return;
        }
        if (user.getId() == null) {
            ResponseUtil.error(ctx, 400, "请求体缺少 id");
            return;
        }
        SysUser updated = authService.updateUser(user);
        ResponseUtil.success(ctx, updated);
    }

    @SneakyThrows
    private void resetPassword(RoutingContext ctx) {
        SysUser user = RequestUtil.getBodyObj(ctx, SysUser.class);
        if (user == null) {
            ResponseUtil.error(ctx, 400, "请求体不能为空");
            return;
        }
        if (user.getId() == null || user.getPassword() == null) {
            ResponseUtil.error(ctx, 400, "请求体缺少 id 或 password");
            return;
        }
        SysUser updated = authService.resetPassword(user.getId(), user.getPassword());
        ResponseUtil.success(ctx, updated);
    }

    @SneakyThrows
    private void deleteUser(RoutingContext ctx) {
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
    private void updateEnableFlag(RoutingContext ctx) {
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
        SysUser user = authService.updateEnableFlag(id, enableFlag);
        ResponseUtil.success(ctx, user);
    }

    private void getPermissions(RoutingContext ctx) {
        // 返回静态权限列表，确保前端展示
        JsonArray list = new JsonArray();
        list.add(new JsonObject().put("name", "总览管理").put("description", "查看系统总览和统计信息")
                .put("actions", new JsonArray().add("查看").add("导出")));
        list.add(new JsonObject().put("name", "客户端管理").put("description", "管理代理客户端和配置").put(
                "actions", new JsonArray().add("查看").add("新增").add("编辑").add("删除").add("启用/禁用")));
        list.add(new JsonObject().put("name", "账号管理").put("description", "管理系统用户账号").put("actions",
                new JsonArray().add("查看").add("新增").add("编辑").add("删除").add("权限管理")));
        list.add(new JsonObject().put("name", "日志管理").put("description", "查看系统日志和审计记录")
                .put("actions", new JsonArray().add("查看").add("导出").add("清理")));
        list.add(new JsonObject().put("name", "系统设置").put("description", "管理系统配置和参数").put("actions",
                new JsonArray().add("查看").add("修改")));
        ResponseUtil.success(ctx, list);
    }
}
