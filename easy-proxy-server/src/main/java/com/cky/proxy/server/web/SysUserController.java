package com.cky.proxy.server.web;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.captcha.generator.RandomGenerator;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;

import com.cky.proxy.server.bean.dto.Result;
import com.cky.proxy.server.bean.entity.SysUser;
import com.cky.proxy.server.dao.SysUserDao;
import com.cky.proxy.server.util.VertxUtil;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SysUserController {
    private final Router router;
    private final JWTAuth jwtAuth;
    private final Vertx vertx;
    private final SysUserDao sysUserDao;
    // 验证码缓存，key为验证码ID，value为验证码文本
    private final Map<String, String> captchaCache = new ConcurrentHashMap<>();
    // 验证码有效期（毫秒）
    private static final long CAPTCHA_EXPIRE_TIME = 5 * 60 * 1000;

    public SysUserController(Router router, Vertx vertx) {
        this.router = router;
        this.vertx = vertx;
        this.sysUserDao = new SysUserDao();
        // 配置JWT
        JWTAuthOptions jwtAuthOptions = new JWTAuthOptions()
                .addPubSecKey(new io.vertx.ext.auth.PubSecKeyOptions()
                        .setAlgorithm("HS256")
                        .setBuffer("easy-proxy-secret-key-for-jwt-authentication"));
        this.jwtAuth = JWTAuth.create(vertx, jwtAuthOptions);

        initRoutes();
    }

    private void initRoutes() {
        // 生成验证码图片
        router.get("/api/captchaImage").handler(this::captchaImage);
        // 用户登录
        router.post("/api/loginUser").handler(this::loginUser);
    }

    private void loginUser(RoutingContext ctx) {
        try {
            // 从请求体获取JSON数据
            JsonObject body = ctx.getBodyAsJson();
            if (body == null) {
                VertxUtil.response(ctx, Result.error("请求体不能为空"));
                return;
            }

            // 获取用户名、密码和验证码信息
            String username = body.getString("username");
            String password = body.getString("password");
            String captchaId = body.getString("captchaId");
            String captchaCode = body.getString("captchaCode");

            // 验证参数
            if (StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
                VertxUtil.response(ctx, Result.error("用户名和密码不能为空"));
                return;
            }

            // 验证验证码
            if (StrUtil.isBlank(captchaId) || StrUtil.isBlank(captchaCode)) {
                VertxUtil.response(ctx, Result.error("验证码不能为空"));
                return;
            }

            // 从缓存中获取验证码
            String cachedCaptcha = captchaCache.get(captchaId);
            if (cachedCaptcha == null) {
                VertxUtil.response(ctx, Result.error("验证码已过期"));
                return;
            }

            // 验证码使用后立即删除
            captchaCache.remove(captchaId);

            // 验证码不匹配
            if (!cachedCaptcha.equalsIgnoreCase(captchaCode)) {
                VertxUtil.response(ctx, Result.error("验证码错误"));
                return;
            }

            // 查询用户
            SysUser user = null;
            try {
                user = sysUserDao.selectList(queryBuilder -> {
                    queryBuilder.where().eq("username", username);
                }).stream().findFirst().orElse(null);
            } catch (Exception e) {
                VertxUtil.response(ctx, Result.error("查询用户失败: " + e.getMessage()));
                return;
            }

            // 用户不存在或密码错误
            if (user == null || !password.equals(user.getPassword())) {
                VertxUtil.response(ctx, Result.error("用户名或密码错误"));
                return;
            }

            // 生成JWT令牌
            JWTOptions options = new JWTOptions()
                    .setExpiresInMinutes(60) // 令牌有效期60分钟
                    .setIssuer("easy-proxy");

            String token = jwtAuth.generateToken(
                    new JsonObject().put("userId", user.getId()).put("username", user.getUsername()),
                    options);

            // 构建用户信息响应
            JsonObject userInfo = new JsonObject()
                    .put("id", user.getId())
                    .put("username", user.getUsername())
                    .put("avatar", user.getAvatar())
                    .put("token", token);

            // 返回成功响应
            VertxUtil.response(ctx, Result.success(userInfo, "登录成功"));
        } catch (Exception e) {
            VertxUtil.response(ctx, Result.error("登录失败: " + e.getMessage()));
        }
    }

    private void captchaImage(RoutingContext ctx) {
        try {
            // 生成验证码ID
            String captchaId = IdUtil.simpleUUID();

            // 自定义验证码生成器，生成4位随机验证码
            RandomGenerator randomGenerator = new RandomGenerator("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ", 4);

            // 创建线条干扰的验证码图片，宽120，高40，验证码为4位字符
            LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 40);
            captcha.setGenerator(randomGenerator);

            // 生成验证码
            captcha.createCode();
            String code = captcha.getCode();

            // 将验证码存入缓存
            captchaCache.put(captchaId, code);

            // 设置定时任务，在验证码过期后从缓存中移除
            vertx.setTimer(CAPTCHA_EXPIRE_TIME, timerId -> captchaCache.remove(captchaId));

            // 构建响应数据
            JsonObject responseData = new JsonObject()
                    .put("captchaId", captchaId)
                    .put("img", "data:image/png;base64," + captcha.getImageBase64());

            // 返回验证码信息
            VertxUtil.response(ctx, Result.success(responseData, "获取验证码成功"));
        } catch (Exception e) {
            VertxUtil.response(ctx, Result.error("生成验证码失败: " + e.getMessage()));
        }
    }
}
