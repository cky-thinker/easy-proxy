package com.cky.proxy.server.controller;

import cn.hutool.core.util.StrUtil;

import java.util.HashMap;

import com.cky.proxy.server.config.ConfigProperty;
import com.cky.proxy.server.domain.dto.CaptchaImage;
import com.cky.proxy.server.domain.dto.PageResult;
import com.cky.proxy.server.domain.dto.LoginReq;
import com.cky.proxy.server.domain.dto.Result;
import com.cky.proxy.server.domain.dto.UserInfo;
import com.cky.proxy.server.domain.entity.SysUser;
import com.cky.proxy.server.service.UserService;
import com.cky.proxy.server.util.JsonUtil;
import com.cky.proxy.server.util.PageUtil;
import com.cky.proxy.server.util.VertxUtil;

import io.vertx.core.Vertx;
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
        router.get("/api/sys/captchaImage").handler(this::captchaImage);
        // 用户登录
        router.post("/api/sys/loginUser").handler(this::loginUser);
        router.get("/api/sys/config").handler(this::getConfig);

        // 账号管理路由
        router.get("/api/accounts").handler(this::getAccountsPageable);
        router.get("/api/accounts/:id").handler(this::getAccountDetail);
        router.post("/api/accounts").handler(this::addAccount);
        router.put("/api/accounts/:id").handler(this::updateAccount);
        router.delete("/api/accounts/:id").handler(this::deleteAccount);
        router.post("/api/accounts/batch-delete").handler(this::batchDeleteAccounts);
        router.post("/api/accounts/:id/reset-password").handler(this::resetPassword);
        router.patch("/api/accounts/:id/status").handler(this::updateStatus);
        router.get("/api/permissions").handler(this::getPermissions);
        router.get("/api/accounts/search").handler(this::searchAccounts);
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

    private void getConfig(RoutingContext routingcontext1) {
        try {
            ConfigProperty configProperty = ConfigProperty.getInstance();
            HashMap<String, Object> map = new HashMap<>();
            map.put("captchaImageEnable", configProperty.getServer().getCatureImageEnable());
            VertxUtil.response(routingcontext1, Result.success(map, "获取配置成功"));
        } catch (Exception e) {
            VertxUtil.response(routingcontext1, Result.error("获取配置失败: " + e.getMessage()));
        }
    }

    // ===== 账户管理 =====

    private void getAccountsPageable(RoutingContext ctx) {
        try {
            PageResult<SysUser> pageResult = authService.getAccountsPageable(
                    PageUtil.getPage(ctx),
                    ctx.request().getParam("q"),
                    ctx.request().getParam("role"),
                    ctx.request().getParam("status"));

            VertxUtil.success(ctx, pageResult);
        } catch (Exception e) {
            VertxUtil.error(ctx, 500, "查询账户失败: " + e.getMessage());
        }
    }

    private void getAccountDetail(RoutingContext ctx) {
        String idParam = ctx.request().getParam("id");
        if (StrUtil.isEmpty(idParam)) {
            VertxUtil.error(ctx, 400, "缺少参数: id");
            return;
        }
        try {
            Integer id = Integer.parseInt(idParam);
            SysUser user = authService.getAccountById(id);
            if (user == null) {
                VertxUtil.error(ctx, 404, "账号不存在");
                return;
            }
            VertxUtil.success(ctx, user);
        } catch (NumberFormatException e) {
            VertxUtil.error(ctx, 400, "id 格式错误");
        } catch (Exception e) {
            VertxUtil.error(ctx, 500, "获取账户详情失败: " + e.getMessage());
        }
    }

    private void addAccount(RoutingContext ctx) {
        try {
            io.vertx.core.json.JsonObject body = ctx.body().asJsonObject();
            if (body == null) {
                VertxUtil.error(ctx, 400, "请求体不能为空");
                return;
            }
            SysUser user = new SysUser();
            user.setUsername(body.getString("username"));
            user.setEmail(body.getString("email"));
            user.setPassword(body.getString("password"));
            user.setRole(body.getString("role"));
            String status = body.getString("status", "active");
            user.setEnableFlag("active".equalsIgnoreCase(status));
            SysUser created = authService.createAccount(user);
            VertxUtil.success(ctx, created);
        } catch (Exception e) {
            VertxUtil.error(ctx, 500, "创建账户失败: " + e.getMessage());
        }
    }

    private void updateAccount(RoutingContext ctx) {
        try {
            String idParam = ctx.request().getParam("id");
            if (StrUtil.isEmpty(idParam)) {
                VertxUtil.error(ctx, 400, "缺少参数: id");
                return;
            }
            Integer id = Integer.parseInt(idParam);
            io.vertx.core.json.JsonObject body = ctx.body().asJsonObject();
            if (body == null) {
                VertxUtil.error(ctx, 400, "请求体不能为空");
                return;
            }
            SysUser user = authService.getAccountById(id);
            if (user == null) {
                VertxUtil.error(ctx, 404, "账号不存在");
                return;
            }
            if (body.getString("username") != null) user.setUsername(body.getString("username"));
            if (body.getString("email") != null) user.setEmail(body.getString("email"));
            if (body.getString("role") != null) user.setRole(body.getString("role"));
            if (body.getString("status") != null) user.setEnableFlag("active".equalsIgnoreCase(body.getString("status")));
            SysUser updated = authService.updateAccount(user);
            VertxUtil.success(ctx, updated);
        } catch (NumberFormatException e) {
            VertxUtil.error(ctx, 400, "id 格式错误");
        } catch (Exception e) {
            VertxUtil.error(ctx, 500, "更新账户失败: " + e.getMessage());
        }
    }

    private void deleteAccount(RoutingContext ctx) {
        try {
            String idParam = ctx.request().getParam("id");
            if (StrUtil.isEmpty(idParam)) {
                VertxUtil.error(ctx, 400, "缺少参数: id");
                return;
            }
            Integer id = Integer.parseInt(idParam);
            boolean ok = authService.deleteAccount(id);
            if (!ok) {
                VertxUtil.error(ctx, 404, "删除失败");
                return;
            }
            VertxUtil.success(ctx, null);
        } catch (NumberFormatException e) {
            VertxUtil.error(ctx, 400, "id 格式错误");
        } catch (Exception e) {
            VertxUtil.error(ctx, 500, "删除账户失败: " + e.getMessage());
        }
    }

    private void batchDeleteAccounts(RoutingContext ctx) {
        try {
            io.vertx.core.json.JsonObject body = ctx.body().asJsonObject();
            if (body == null || body.getJsonArray("ids") == null) {
                VertxUtil.error(ctx, 400, "请求体缺少 ids");
                return;
            }
            java.util.List<Integer> ids = body.getJsonArray("ids").stream()
                    .map(Object::toString)
                    .map(Integer::parseInt)
                    .collect(java.util.stream.Collectors.toList());
            authService.batchDeleteAccounts(ids);
            VertxUtil.success(ctx, null);
        } catch (Exception e) {
            VertxUtil.error(ctx, 500, "批量删除失败: " + e.getMessage());
        }
    }

    private void resetPassword(RoutingContext ctx) {
        try {
            String idParam = ctx.request().getParam("id");
            if (StrUtil.isEmpty(idParam)) {
                VertxUtil.error(ctx, 400, "缺少参数: id");
                return;
            }
            Integer id = Integer.parseInt(idParam);
            io.vertx.core.json.JsonObject body = ctx.body().asJsonObject();
            if (body == null || StrUtil.isEmpty(body.getString("password"))) {
                VertxUtil.error(ctx, 400, "请求体缺少 password");
                return;
            }
            SysUser user = authService.resetPassword(id, body.getString("password"));
            VertxUtil.success(ctx, user);
        } catch (NumberFormatException e) {
            VertxUtil.error(ctx, 400, "id 格式错误");
        } catch (Exception e) {
            VertxUtil.error(ctx, 500, "重置密码失败: " + e.getMessage());
        }
    }

    private void updateStatus(RoutingContext ctx) {
        try {
            String idParam = ctx.request().getParam("id");
            if (StrUtil.isEmpty(idParam)) {
                VertxUtil.error(ctx, 400, "缺少参数: id");
                return;
            }
            Integer id = Integer.parseInt(idParam);
            io.vertx.core.json.JsonObject body = ctx.body().asJsonObject();
            String status = body == null ? null : body.getString("status");
            if (StrUtil.isEmpty(status)) {
                VertxUtil.error(ctx, 400, "请求体缺少 status");
                return;
            }
            SysUser user = authService.updateStatus(id, status);
            VertxUtil.success(ctx, user);
        } catch (NumberFormatException e) {
            VertxUtil.error(ctx, 400, "id 格式错误");
        } catch (Exception e) {
            VertxUtil.error(ctx, 500, "更新状态失败: " + e.getMessage());
        }
    }

    private void getPermissions(RoutingContext ctx) {
        // 返回静态权限列表，确保前端展示
        io.vertx.core.json.JsonArray list = new io.vertx.core.json.JsonArray();
        list.add(new io.vertx.core.json.JsonObject().put("name", "总览管理").put("description", "查看系统总览和统计信息").put("actions", new io.vertx.core.json.JsonArray().add("查看").add("导出")));
        list.add(new io.vertx.core.json.JsonObject().put("name", "客户端管理").put("description", "管理代理客户端和配置").put("actions", new io.vertx.core.json.JsonArray().add("查看").add("新增").add("编辑").add("删除").add("启用/禁用"))));
        list.add(new io.vertx.core.json.JsonObject().put("name", "账号管理").put("description", "管理系统用户账号").put("actions", new io.vertx.core.json.JsonArray().add("查看").add("新增").add("编辑").add("删除").add("权限管理"))));
        list.add(new io.vertx.core.json.JsonObject().put("name", "日志管理").put("description", "查看系统日志和审计记录").put("actions", new io.vertx.core.json.JsonArray().add("查看").add("导出").add("清理"))));
        list.add(new io.vertx.core.json.JsonObject().put("name", "系统设置").put("description", "管理系统配置和参数").put("actions", new io.vertx.core.json.JsonArray().add("查看").add("修改"))));
        VertxUtil.success(ctx, list);
    }

    private void searchAccounts(RoutingContext ctx) {
        // 复用分页接口逻辑
        getAccountsPageable(ctx);
    }
}
