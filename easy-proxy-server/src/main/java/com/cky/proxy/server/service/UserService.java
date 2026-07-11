package com.cky.proxy.server.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cky.proxy.server.config.ConfigProperty;
import com.cky.proxy.server.config.ServerProperty;
import com.cky.proxy.server.domain.dto.CaptchaImage;
import com.cky.proxy.server.domain.dto.CreateUserReq;
import com.cky.proxy.server.domain.dto.InitUserReq;
import com.cky.proxy.server.domain.dto.LoginReq;
import com.cky.proxy.server.domain.dto.PageResult;
import com.cky.proxy.server.domain.dto.UserInfo;
import com.cky.proxy.server.domain.dto.UserView;
import com.cky.proxy.server.domain.entity.SysUser;
import com.cky.proxy.server.mapper.SysUserMapper;
import com.cky.proxy.server.util.BeanContext;
import com.cky.proxy.server.util.PageUtil;
import com.cky.proxy.server.util.PasswordSecurityUtil;
import com.cky.proxy.server.util.PasswordTransportCryptoUtil;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.captcha.generator.RandomGenerator;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Page;
import cn.hutool.jwt.JWT;

public class UserService {
    private static final long CAPTCHA_EXPIRE_TIME = 5 * 60 * 1000L;

    private final SysUserMapper userMapper;
    private final TimedCache<String, String> captchaCache = CacheUtil.newTimedCache(CAPTCHA_EXPIRE_TIME);
    private final Map<String, FailureState> userFailureStateMap = new ConcurrentHashMap<>();
    private final Map<String, FailureState> ipFailureStateMap = new ConcurrentHashMap<>();

    public UserService() {
        this.userMapper = BeanContext.getUserMapper();
        this.captchaCache.schedulePrune(CAPTCHA_EXPIRE_TIME);
    }

    private static final class FailureState {
        private int failureCount;
        private long windowStart;
        private long blockedUntil;
    }

    private void validateUniqueUserFields(String username, String mobile, String email, Integer excludeId) {
        if (username != null) {
            LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUser::getUsername, username);
            if (excludeId != null) {
                wrapper.ne(SysUser::getId, excludeId);
            }
            if (!userMapper.selectList(wrapper).isEmpty()) {
                throw new RuntimeException("账号已存在");
            }
        }
        if (mobile != null && !mobile.isEmpty()) {
            LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUser::getMobile, mobile);
            if (excludeId != null) {
                wrapper.ne(SysUser::getId, excludeId);
            }
            if (!userMapper.selectList(wrapper).isEmpty()) {
                throw new RuntimeException("手机号已存在");
            }
        }
        if (email != null && !email.isEmpty()) {
            LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUser::getEmail, email);
            if (excludeId != null) {
                wrapper.ne(SysUser::getId, excludeId);
            }
            if (!userMapper.selectList(wrapper).isEmpty()) {
                throw new RuntimeException("邮箱已存在");
            }
        }
    }

    private void validateRole(String role) {
        if (role == null || role.isEmpty()) {
            throw new RuntimeException("角色不能为空");
        }
        if (!"admin".equals(role) && !"user".equals(role) && !"viewer".equals(role)) {
            throw new RuntimeException("角色不合法");
        }
    }

    private void validateForCreate(String username, String mobile, String email, String role) {
        if (StrUtil.isBlank(username)) {
            throw new RuntimeException("账号不能为空");
        }
        validateRole(role);
        validateUniqueUserFields(username, mobile, email, null);
    }

    private void validateForUpdate(SysUser existing, SysUser patch) {
        String newUsername = patch.getUsername() != null && !patch.getUsername().equals(existing.getUsername())
                ? patch.getUsername()
                : null;
        String newMobile = patch.getMobile() != null && !patch.getMobile().equals(existing.getMobile())
                ? patch.getMobile()
                : null;
        String newEmail = patch.getEmail() != null && !patch.getEmail().equals(existing.getEmail())
                ? patch.getEmail()
                : null;
        validateUniqueUserFields(newUsername, newMobile, newEmail, patch.getId());
        if (patch.getRole() != null) {
            validateRole(patch.getRole());
        }
    }

    public UserInfo login(LoginReq loginReq, String clientIp) {
        try {
            if (loginReq == null || StrUtil.isBlank(loginReq.getUsername())
                    || StrUtil.isBlank(loginReq.getEncryptedPassword())) {
                throw new RuntimeException("用户名和密码不能为空");
            }

            String username = loginReq.getUsername().trim();
            String ip = normalizeKey(clientIp);

            assertIpNotBlocked(ip);
            assertUserNotLocked(username);
            validateCaptcha(loginReq);

            String plainPassword = PasswordTransportCryptoUtil.decryptPassword(loginReq.getEncryptedPassword());
            SysUser sysUser = userMapper
                    .selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));

            if (sysUser == null || !verifyPassword(sysUser, plainPassword)) {
                throw new RuntimeException(recordFailure(username, ip, "用户名或密码错误"));
            }

            if (!Boolean.TRUE.equals(sysUser.getEnableFlag())) {
                throw new RuntimeException("用户已被禁用");
            }

            clearFailures(username, ip);

            String token = JWT.create()
                    .setIssuer("easy-proxy")
                    .setExpiresAt(new Date(System.currentTimeMillis() + 60 * 60 * 1000))
                    .setPayload("userId", sysUser.getId())
                    .setPayload("username", sysUser.getUsername())
                    .setKey("easy-proxy-secret-key-for-jwt-authentication".getBytes())
                    .sign();

            sysUser.setLoginTime(new Date());
            sysUser.setUpdateTime(new Date());
            userMapper.updateById(sysUser);

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
        String captchaId = IdUtil.simpleUUID();

        RandomGenerator randomGenerator = new RandomGenerator("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ", 4);
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 40);
        captcha.setGenerator(randomGenerator);
        captcha.createCode();

        captchaCache.put(captchaId, captcha.getCode());

        CaptchaImage captchaImage = new CaptchaImage();
        captchaImage.setCaptchaId(captchaId);
        captchaImage.setImg("data:image/png;base64," + captcha.getImageBase64());
        return captchaImage;
    }

    public boolean checkInit() {
        try {
            return userMapper.selectCount(null) == 0;
        } catch (Exception e) {
            throw new RuntimeException("查询用户数量失败", e);
        }
    }

    public UserView initAdmin(InitUserReq req) {
        if (!checkInit()) {
            throw new RuntimeException("系统已初始化，禁止重复操作");
        }

        SysUser user = new SysUser();
        user.setUsername(req.getUsername());
        user.setMobile(req.getMobile());
        user.setEmail(req.getEmail());
        user.setRole("admin");
        user.setPassword(hashEncryptedPassword(req.getEncryptedPassword()));
        return createUser(user);
    }

    public PageResult<UserView> getUsersPageable(Page page, String q, Boolean enableFlag) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (q != null && !q.isEmpty()) {
            wrapper.and(w -> w.like(SysUser::getUsername, q).or().like(SysUser::getEmail, q));
        }
        if (enableFlag != null) {
            wrapper.eq(SysUser::getEnableFlag, enableFlag);
        }

        IPage<SysUser> mybatisPage = PageUtil.toMybatisPage(page);
        IPage<SysUser> result = userMapper.selectPage(mybatisPage, wrapper);
        List<UserView> list = new ArrayList<>();
        for (SysUser user : result.getRecords()) {
            list.add(toUserView(user));
        }
        return new PageResult<>(page.getPageNumber(), page.getPageSize(), (int) result.getPages(),
                (int) result.getTotal(), list);
    }

    public UserView getUserById(Integer id) {
        SysUser user = userMapper.selectById(id);
        return user == null ? null : toUserView(user);
    }

    public UserView createUser(CreateUserReq req) {
        SysUser user = new SysUser();
        user.setUsername(req.getUsername());
        user.setMobile(req.getMobile());
        user.setEmail(req.getEmail());
        user.setRole(req.getRole());
        user.setAvatar(req.getAvatar());
        user.setEnableFlag(req.getEnableFlag());
        user.setPassword(hashEncryptedPassword(req.getEncryptedPassword()));
        return createUser(user);
    }

    public UserView createUser(SysUser user) {
        validateForCreate(user.getUsername(), user.getMobile(), user.getEmail(), user.getRole());
        if (StrUtil.isBlank(user.getPassword())) {
            throw new RuntimeException("密码不能为空");
        }

        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());
        if (user.getEnableFlag() == null) {
            user.setEnableFlag(Boolean.TRUE);
        }
        userMapper.insert(user);
        return toUserView(user);
    }

    public UserView updateUser(SysUser user) {
        if (user == null || user.getId() == null) {
            throw new RuntimeException("请求体缺少 id");
        }
        SysUser db = userMapper.selectById(user.getId());
        if (db == null) {
            throw new RuntimeException("账号不存在");
        }
        validateForUpdate(db, user);

        if (user.getUsername() != null) {
            db.setUsername(user.getUsername());
        }
        if (user.getMobile() != null) {
            db.setMobile(user.getMobile());
        }
        if (user.getEmail() != null) {
            db.setEmail(user.getEmail());
        }
        if (user.getRole() != null) {
            db.setRole(user.getRole());
        }
        if (user.getAvatar() != null) {
            db.setAvatar(user.getAvatar());
        }
        if (user.getEnableFlag() != null) {
            db.setEnableFlag(user.getEnableFlag());
        }

        db.setUpdateTime(new Date());
        userMapper.updateById(db);
        return toUserView(userMapper.selectById(db.getId()));
    }

    public boolean deleteUser(Integer id) {
        userMapper.deleteById(id);
        return true;
    }

    public UserView resetPassword(Integer id, String encryptedPassword) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("账号不存在");
        }
        user.setPassword(hashEncryptedPassword(encryptedPassword));
        user.setUpdateTime(new Date());
        userMapper.updateById(user);
        return toUserView(user);
    }

    public UserView updateEnableFlag(Integer id, Boolean enableFlag) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("账号不存在");
        }
        user.setEnableFlag(enableFlag);
        user.setUpdateTime(new Date());
        userMapper.updateById(user);
        return toUserView(user);
    }

    public String getPasswordPublicKey() {
        return PasswordTransportCryptoUtil.getPublicKeyPem();
    }

    private String hashEncryptedPassword(String encryptedPassword) {
        String plainPassword = PasswordTransportCryptoUtil.decryptPassword(encryptedPassword);
        if (StrUtil.isBlank(plainPassword)) {
            throw new RuntimeException("密码不能为空");
        }
        return PasswordSecurityUtil.hashPassword(plainPassword);
    }

    private boolean verifyPassword(SysUser user, String plainPassword) {
        boolean matches = PasswordSecurityUtil.matches(plainPassword, user.getPassword());
        if (matches && !PasswordSecurityUtil.isHashed(user.getPassword())) {
            user.setPassword(PasswordSecurityUtil.hashPassword(plainPassword));
            user.setUpdateTime(new Date());
            userMapper.updateById(user);
        }
        return matches;
    }

    private void validateCaptcha(LoginReq loginReq) {
        if (!Boolean.TRUE.equals(ConfigProperty.getInstance().getServer().getCaptchaImageEnable())) {
            return;
        }

        if (StrUtil.isBlank(loginReq.getCaptchaId()) || StrUtil.isBlank(loginReq.getCaptchaCode())) {
            throw new RuntimeException("验证码不能为空");
        }

        String cachedCaptcha = captchaCache.get(loginReq.getCaptchaId());
        if (cachedCaptcha == null) {
            throw new RuntimeException("验证码已过期");
        }
        captchaCache.remove(loginReq.getCaptchaId());
        if (!cachedCaptcha.equalsIgnoreCase(loginReq.getCaptchaCode())) {
            throw new RuntimeException("验证码错误");
        }
    }

    private void assertUserNotLocked(String username) {
        FailureState state = userFailureStateMap.get(username);
        if (state == null) {
            return;
        }

        long now = System.currentTimeMillis();
        synchronized (state) {
            if (state.blockedUntil > now) {
                throw new RuntimeException("账号已锁定，请在 " + getRemainingMinutes(state.blockedUntil)
                        + " 分钟后重试");
            }
            if (state.blockedUntil > 0) {
                userFailureStateMap.remove(username);
            }
        }
    }

    private void assertIpNotBlocked(String clientIp) {
        FailureState state = ipFailureStateMap.get(clientIp);
        if (state == null) {
            return;
        }

        long now = System.currentTimeMillis();
        synchronized (state) {
            if (state.blockedUntil > now) {
                throw new RuntimeException("当前IP尝试过于频繁，请在 " + getRemainingMinutes(state.blockedUntil)
                        + " 分钟后重试");
            }
            if (state.blockedUntil > 0) {
                ipFailureStateMap.remove(clientIp);
            }
        }
    }

    private String recordFailure(String username, String clientIp, String defaultMessage) {
        ServerProperty server = ConfigProperty.getInstance().getServer();
        long now = System.currentTimeMillis();

        FailureState userState = updateFailureState(userFailureStateMap, username, now,
                server.getLoginFailureThreshold(), server.getLoginFailureWindowMinutes(), server.getLoginLockMinutes());
        FailureState ipState = updateFailureState(ipFailureStateMap, clientIp, now,
                server.getLoginIpFailureThreshold(), server.getLoginIpWindowMinutes(), server.getLoginIpBlockMinutes());

        if (ipState != null && ipState.blockedUntil > now) {
            return "当前IP尝试过于频繁，请在 " + getRemainingMinutes(ipState.blockedUntil) + " 分钟后重试";
        }
        if (userState != null && userState.blockedUntil > now) {
            return "账号已锁定，请在 " + getRemainingMinutes(userState.blockedUntil) + " 分钟后重试";
        }
        return defaultMessage;
    }

    private FailureState updateFailureState(Map<String, FailureState> stateMap, String key, long now,
            Integer threshold, Integer windowMinutes, Integer blockMinutes) {
        if (key == null || key.isBlank() || threshold == null || threshold <= 0) {
            return null;
        }

        FailureState state = stateMap.computeIfAbsent(key, ignored -> new FailureState());
        synchronized (state) {
            long windowMillis = Math.max(windowMinutes == null ? 0L : windowMinutes.longValue(), 1L) * 60_000L;
            long blockMillis = Math.max(blockMinutes == null ? 0L : blockMinutes.longValue(), 1L) * 60_000L;

            if (state.windowStart == 0L || now - state.windowStart > windowMillis) {
                state.windowStart = now;
                state.failureCount = 0;
            }

            state.failureCount++;
            if (state.failureCount >= threshold) {
                state.failureCount = 0;
                state.windowStart = now;
                state.blockedUntil = now + blockMillis;
            }
            return state;
        }
    }

    private void clearFailures(String username, String clientIp) {
        userFailureStateMap.remove(username);
        ipFailureStateMap.remove(clientIp);
    }

    private long getRemainingMinutes(long blockedUntil) {
        long remainingMillis = Math.max(blockedUntil - System.currentTimeMillis(), 0L);
        return Math.max(1L, (remainingMillis + 59_999L) / 60_000L);
    }

    private String normalizeKey(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim();
    }

    private UserView toUserView(SysUser user) {
        UserView view = new UserView();
        view.setId(user.getId());
        view.setUsername(user.getUsername());
        view.setMobile(user.getMobile());
        view.setEmail(user.getEmail());
        view.setRole(user.getRole());
        view.setAvatar(user.getAvatar());
        view.setEnableFlag(user.getEnableFlag());
        view.setLoginTime(user.getLoginTime());
        view.setCreateTime(user.getCreateTime());
        view.setUpdateTime(user.getUpdateTime());
        return view;
    }
}
