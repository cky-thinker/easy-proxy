package com.cky.proxy.server.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.cky.proxy.server.dao.UserDao;
import com.cky.proxy.server.domain.dto.CaptchaImage;
import com.cky.proxy.server.domain.dto.LoginReq;
import com.cky.proxy.server.domain.dto.UserInfo;
import com.cky.proxy.server.domain.entity.User;
import com.cky.proxy.server.util.BeanContext;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.captcha.generator.RandomGenerator;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;

public class UserService {
    private final UserDao userDao;
    private final JWTAuth jwtAuth;
    private final Vertx vertx;
    // 验证码缓存，key为验证码ID，value为验证码文本
    private final Map<String, String> captchaCache = new ConcurrentHashMap<>();
    // 验证码有效期（毫秒）
    private static final long CAPTCHA_EXPIRE_TIME = 5 * 60 * 1000;

    public UserService(Vertx vertx) {
        this.userDao = BeanContext.getUserDao();
        this.vertx = vertx;
        // 配置JWT
        JWTAuthOptions jwtAuthOptions = new JWTAuthOptions()
                .addPubSecKey(new io.vertx.ext.auth.PubSecKeyOptions()
                        .setAlgorithm("HS256")
                        .setBuffer("easy-proxy-secret-key-for-jwt-authentication"));
        this.jwtAuth = JWTAuth.create(vertx, jwtAuthOptions);
    }

    public UserInfo login(LoginReq loginReq) {
        try {
            // 验证参数
            if (StrUtil.isBlank(loginReq.getUsername()) || StrUtil.isBlank(loginReq.getPassword())) {
                throw new RuntimeException("用户名和密码不能为空");
            }

            // 验证验证码
            if (StrUtil.isBlank(loginReq.getCaptchaId()) || StrUtil.isBlank(loginReq.getCaptchaCode())) {
                throw new RuntimeException("验证码不能为空");
            }

            // 从缓存中获取验证码
            String cachedCaptcha = captchaCache.get(loginReq.getCaptchaId());
            if (cachedCaptcha == null) {
                throw new RuntimeException("验证码已过期");
            }

            // 验证码使用后立即删除
            captchaCache.remove(loginReq.getCaptchaId());

            // 验证码不匹配
            if (!cachedCaptcha.equalsIgnoreCase(loginReq.getCaptchaCode())) {
                throw new RuntimeException("验证码错误");
            }

            // 查询用户
            User user = null;
            try {
                user = userDao.selectList(queryBuilder -> {
                    queryBuilder.where().eq("username", loginReq.getUsername());
                }).stream().findFirst().orElse(null);
            } catch (Exception e) {
                throw new RuntimeException("查询用户失败: " + e.getMessage());
            }

            // 用户不存在或密码错误
            if (user == null || !loginReq.getPassword().equals(user.getPassword())) {
                throw new RuntimeException("用户名或密码错误");
            }

            // 生成JWT令牌
            JWTOptions options = new JWTOptions()
                    .setExpiresInMinutes(60) // 令牌有效期60分钟
                    .setIssuer("easy-proxy");

            String token = jwtAuth.generateToken(
                    new JsonObject().put("userId", user.getId()).put("username", user.getUsername()),
                    options);

            // 构建用户信息响应
            UserInfo userInfo = new UserInfo();
            userInfo.setUserId(user.getId());
            userInfo.setUsername(user.getUsername());
            userInfo.setAvatar(user.getAvatar());
            userInfo.setToken(token);
            return userInfo;
        } catch (Exception e) {
            throw new RuntimeException("登录失败: " + e.getMessage());
        }
    }

    public CaptchaImage captchaImage() {
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
        CaptchaImage captchaImage = new CaptchaImage();
        captchaImage.setCaptchaId(captchaId);
        captchaImage.setImg("data:image/png;base64," + captcha.getImageBase64());
        return captchaImage;
    }
}
