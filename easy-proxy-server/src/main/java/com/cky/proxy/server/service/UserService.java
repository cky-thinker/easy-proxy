package com.cky.proxy.server.service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.cky.proxy.server.config.ConfigProperty;
import com.cky.proxy.server.dao.UserDao;
import com.cky.proxy.server.domain.dto.CaptchaImage;
import com.cky.proxy.server.domain.dto.LoginReq;
import com.cky.proxy.server.domain.dto.PageResult;
import com.cky.proxy.server.domain.dto.UserInfo;
import com.cky.proxy.server.domain.entity.SysUser;
import com.cky.proxy.server.util.BeanContext;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.captcha.generator.RandomGenerator;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Page;
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
            // 基础校验
            if (StrUtil.isBlank(loginReq.getUsername()) || StrUtil.isBlank(loginReq.getPassword())) {
                throw new RuntimeException("用户名和密码不能为空");
            }
            // 验证验证码
            if (ConfigProperty.getInstance().getServer().getCaptchaImageEnable()) {
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
            }
            // 验证密码
            SysUser sysUser = null;
            try {
                sysUser = userDao.selectList(queryBuilder -> {
                    queryBuilder.where().eq("username", loginReq.getUsername());
                }).stream().findFirst().orElse(null);
            } catch (Exception e) {
                throw new RuntimeException("查询用户失败: " + e.getMessage());
            }

            // 用户不存在或密码错误
            if (sysUser == null || !loginReq.getPassword().equals(sysUser.getPassword())) {
                throw new RuntimeException("用户名或密码错误");
            }

            // 生成JWT令牌
            JWTOptions options = new JWTOptions()
                    .setExpiresInMinutes(60) // 令牌有效期60分钟
                    .setIssuer("easy-proxy");

            String token = jwtAuth.generateToken(
                    new JsonObject().put("userId", sysUser.getId()).put("username", sysUser.getUsername()),
                    options);

            // 检查用户是否启用
            if (!sysUser.getEnableFlag()) {
                throw new RuntimeException("用户已被禁用");
            }

            // 更新上次登录时间
            sysUser.setLoginTime(new Date());
            sysUser.setUpdateTime(new Date());
            userDao.updateById(sysUser);

            // 构建用户信息响应
            UserInfo userInfo = new UserInfo();
            userInfo.setUserId(sysUser.getId());
            userInfo.setUsername(sysUser.getUsername());
            userInfo.setAvatar(sysUser.getAvatar());
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

    // ===== 账户管理方法 =====

    /**
     * 分页查询账户
     */
    public PageResult<SysUser> getUsersPageable(Page page, String q, Boolean enableFlag) {
        return userDao.selectPage(page, where -> {
            boolean hasWhere = false;
            if (q != null && !q.isEmpty()) {
                // 模糊匹配用户名或邮箱
                where.like("username", "%" + q + "%").or().like("email", "%" + q + "%");
                hasWhere = true;
            }
            if (enableFlag != null) {
                if (hasWhere)
                    where.and();
                where.eq("enable_flag", enableFlag);
            }
        });
    }

    public SysUser getUserById(Integer id) {
        return userDao.selectById(id);
    }

    public SysUser createUser(SysUser user) {
        user.setCreateTime(new Date());
        if (user.getEnableFlag() == null)
            user.setEnableFlag(Boolean.TRUE);
        userDao.insert(user);
        return user;
    }

    public SysUser updateUser(SysUser user) {
        if (user == null || user.getId() == null) {
            throw new RuntimeException("请求体缺少 id");
        }
        SysUser db = userDao.selectById(user.getId());
        if (db == null) {
            throw new RuntimeException("账号不存在");
        }

        // 唯一性校验：用户名、手机号、邮箱（仅当传入且与原值不同时）
        if (StrUtil.isNotBlank(user.getUsername()) && !StrUtil.equals(user.getUsername(), db.getUsername())) {
            boolean exists = !userDao.selectList(qb -> {
                qb.where().eq("username", user.getUsername()).and().ne("id", user.getId());
            }).isEmpty();
            if (exists) throw new RuntimeException("账号已存在");
        }
        if (StrUtil.isNotBlank(user.getMobile()) && !StrUtil.equals(user.getMobile(), db.getMobile())) {
            boolean exists = !userDao.selectList(qb -> {
                qb.where().eq("mobile", user.getMobile()).and().ne("id", user.getId());
            }).isEmpty();
            if (exists) throw new RuntimeException("手机号已存在");
        }
        if (StrUtil.isNotBlank(user.getEmail()) && !StrUtil.equals(user.getEmail(), db.getEmail())) {
            boolean exists = !userDao.selectList(qb -> {
                qb.where().eq("email", user.getEmail()).and().ne("id", user.getId());
            }).isEmpty();
            if (exists) throw new RuntimeException("邮箱已存在");
        }

        // 只更新基础字段，不允许通过此接口更新密码、创建时间等
        if (user.getUsername() != null) db.setUsername(user.getUsername());
        if (user.getMobile() != null) db.setMobile(user.getMobile());
        if (user.getEmail() != null) db.setEmail(user.getEmail());
        if (user.getRole() != null) db.setRole(user.getRole());
        if (user.getAvatar() != null) db.setAvatar(user.getAvatar());
        if (user.getEnableFlag() != null) db.setEnableFlag(user.getEnableFlag());

        db.setUpdateTime(new Date());
        userDao.updateById(db);
        return userDao.selectById(db.getId());
    }

    public boolean deleteUser(Integer id) {
        userDao.deleteById(id);
        return true;
    }

    public void batchDeleteUsers(List<Integer> ids) {
        if (ids == null)
            return;
        for (Integer id : ids) {
            userDao.deleteById(id);
        }
    }

    public SysUser resetPassword(Integer id, String newPassword) {
        SysUser user = userDao.selectById(id);
        if (user == null)
            throw new RuntimeException("账号不存在");
        user.setPassword(newPassword);
        user.setUpdateTime(new Date());
        userDao.updateById(user);
        return user;
    }

    public SysUser updateEnableFlag(Integer id, Boolean enableFlag) {
        SysUser user = userDao.selectById(id);
        if (user == null)
            throw new RuntimeException("账号不存在");
        user.setEnableFlag(enableFlag);
        user.setUpdateTime(new Date());
        userDao.updateById(user);
        return user;
    }
}
