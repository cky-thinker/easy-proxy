package com.cky.proxy.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cky.proxy.server.config.ConfigProperty;
import com.cky.proxy.server.config.DatabaseProperty;
import com.cky.proxy.server.config.ServerProperty;
import com.cky.proxy.server.domain.dto.CreateUserReq;
import com.cky.proxy.server.domain.dto.LoginReq;
import com.cky.proxy.server.domain.dto.UserInfo;
import com.cky.proxy.server.domain.entity.SysUser;
import com.cky.proxy.server.util.BeanContext;
import com.cky.proxy.server.util.CertGenerator;
import com.cky.proxy.server.util.JwtTokenUtil;
import com.cky.proxy.server.util.PasswordSecurityUtil;
import com.cky.proxy.server.util.PasswordTransportCryptoUtil;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.signers.JWTSignerUtil;

class UserServiceSecurityTest {

    private static final String TEST_JWT_SECRET = "test-only-jwt-secret-with-at-least-32-bytes-2026";
    private static final String OLD_HARDCODED_SECRET = "easy-proxy-secret-key-for-jwt-authentication";

    @BeforeAll
    static void initEnvironment() throws Exception {
        DatabaseProperty db = new DatabaseProperty();
        db.setUrl("jdbc:h2:mem:easy-proxy-tests;DB_CLOSE_DELAY=-1");
        db.setUsername("test");
        db.setPassword("test");

        ServerProperty server = new ServerProperty();
        server.setProxyPort(21090);
        server.setWebPort(21091);
        server.setCaptchaImageEnable(false);
        server.setPublicHost("localhost");
        server.setCertValidityDays(3650);
        server.setCertPassword("easyproxy@2008");
        server.setWebhook("");
        server.setJwtSecret(TEST_JWT_SECRET);
        server.setJwtExpireMinutes(60);
        server.setLoginFailureThreshold(5);
        server.setLoginFailureWindowMinutes(15);
        server.setLoginLockMinutes(15);
        server.setLoginIpFailureThreshold(20);
        server.setLoginIpWindowMinutes(10);
        server.setLoginIpBlockMinutes(10);

        ConfigProperty.getInstance().setDb(db);
        ConfigProperty.getInstance().setServer(server);

        CertGenerator.generateIfNotExists();
        BeanContext.getInstance().init();
    }

    @BeforeEach
    void cleanUsers() {
        BeanContext.getUserMapper().delete(null);
        ServerProperty server = ConfigProperty.getInstance().getServer();
        server.setJwtSecret(TEST_JWT_SECRET);
        server.setJwtExpireMinutes(60);
        server.setCaptchaImageEnable(false);
        server.setLoginFailureThreshold(5);
        server.setLoginFailureWindowMinutes(15);
        server.setLoginLockMinutes(15);
        server.setLoginIpFailureThreshold(20);
        server.setLoginIpWindowMinutes(10);
        server.setLoginIpBlockMinutes(10);
    }

    @Test
    void createUserShouldStoreHashedPassword() throws Exception {
        UserService userService = new UserService();

        CreateUserReq req = new CreateUserReq();
        req.setUsername("security-admin");
        req.setEmail("security-admin@example.com");
        req.setRole("admin");
        req.setEnableFlag(true);
        req.setEncryptedPassword(encryptPassword("Secret@123"));

        userService.createUser(req);

        SysUser stored = BeanContext.getUserMapper().selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, "security-admin"));
        assertNotNull(stored);
        assertTrue(PasswordSecurityUtil.isHashed(stored.getPassword()));
        assertTrue(PasswordSecurityUtil.matches("Secret@123", stored.getPassword()));
    }

    @Test
    void loginShouldLockAccountAfterThresholdFailures() throws Exception {
        ServerProperty server = ConfigProperty.getInstance().getServer();
        server.setLoginFailureThreshold(2);
        server.setLoginLockMinutes(1);
        server.setLoginIpFailureThreshold(99);
        server.setLoginIpBlockMinutes(10);

        UserService userService = new UserService();
        CreateUserReq req = new CreateUserReq();
        req.setUsername("locked-user");
        req.setEmail("locked-user@example.com");
        req.setRole("user");
        req.setEnableFlag(true);
        req.setEncryptedPassword(encryptPassword("Correct@123"));
        userService.createUser(req);

        RuntimeException firstFailure = assertThrows(RuntimeException.class,
                () -> userService.login(loginReq("locked-user", "wrong-1"), "127.0.0.1"));
        assertTrue(firstFailure.getMessage().contains("用户名或密码错误"));

        RuntimeException secondFailure = assertThrows(RuntimeException.class,
                () -> userService.login(loginReq("locked-user", "wrong-2"), "127.0.0.1"));
        assertTrue(secondFailure.getMessage().contains("账号已锁定"));

        RuntimeException lockedFailure = assertThrows(RuntimeException.class,
                () -> userService.login(loginReq("locked-user", "Correct@123"), "127.0.0.1"));
        assertTrue(lockedFailure.getMessage().contains("账号已锁定"));
    }

    @Test
    void loginShouldBlockIpAfterThresholdFailures() throws Exception {
        ServerProperty server = ConfigProperty.getInstance().getServer();
        server.setLoginFailureThreshold(99);
        server.setLoginIpFailureThreshold(2);
        server.setLoginIpBlockMinutes(1);

        UserService userService = new UserService();
        CreateUserReq req = new CreateUserReq();
        req.setUsername("ip-user");
        req.setEmail("ip-user@example.com");
        req.setRole("user");
        req.setEnableFlag(true);
        req.setEncryptedPassword(encryptPassword("Correct@123"));
        userService.createUser(req);

        RuntimeException firstFailure = assertThrows(RuntimeException.class,
                () -> userService.login(loginReq("ip-user", "wrong-1"), "10.0.0.8"));
        assertTrue(firstFailure.getMessage().contains("用户名或密码错误"));

        RuntimeException secondFailure = assertThrows(RuntimeException.class,
                () -> userService.login(loginReq("ip-user", "wrong-2"), "10.0.0.8"));
        assertTrue(secondFailure.getMessage().contains("当前IP尝试过于频繁"));
    }

    @Test
    void loginShouldIssueAuthenticatableToken() throws Exception {
        UserService userService = new UserService();
        SysUser user = createUser(userService, "jwt-valid-user", "jwt-valid@example.com");

        UserInfo info = userService.login(loginReq(user.getUsername(), "Correct@123"), "127.0.0.2");

        assertNotNull(info.getToken());
        assertEquals(user.getId(), userService.authenticateToken(info.getToken()));
    }

    @Test
    void tokenSignedWithOldHardcodedSecretShouldBeRejected() throws Exception {
        UserService userService = new UserService();
        SysUser user = createUser(userService, "jwt-old-secret-user", "jwt-old@example.com");
        Date now = new Date();
        String forgedToken = createToken(
                OLD_HARDCODED_SECRET.getBytes(StandardCharsets.UTF_8),
                user,
                JwtTokenUtil.ISSUER,
                now,
                new Date(now.getTime() + 60_000L));

        assertNull(userService.authenticateToken(forgedToken));
    }

    @Test
    void tamperedTokenShouldBeRejected() throws Exception {
        UserService userService = new UserService();
        SysUser user = createUser(userService, "jwt-tamper-user", "jwt-tamper@example.com");
        String token = userService.login(loginReq(user.getUsername(), "Correct@123"), "127.0.0.3").getToken();
        char replacement = token.endsWith("A") ? 'B' : 'A';
        String tamperedToken = token.substring(0, token.length() - 1) + replacement;

        assertNotEquals(token, tamperedToken);
        assertNull(userService.authenticateToken(tamperedToken));
    }

    @Test
    void expiredTokenShouldBeRejected() throws Exception {
        UserService userService = new UserService();
        SysUser user = createUser(userService, "jwt-expired-user", "jwt-expired@example.com");
        Date issuedAt = new Date(System.currentTimeMillis() - 120_000L);
        String expiredToken = createToken(
                TEST_JWT_SECRET.getBytes(StandardCharsets.UTF_8),
                user,
                JwtTokenUtil.ISSUER,
                issuedAt,
                new Date(System.currentTimeMillis() - 60_000L));

        assertNull(userService.authenticateToken(expiredToken));
    }

    @Test
    void tokenWithWrongIssuerShouldBeRejected() throws Exception {
        UserService userService = new UserService();
        SysUser user = createUser(userService, "jwt-issuer-user", "jwt-issuer@example.com");
        Date now = new Date();
        String wrongIssuerToken = createToken(
                TEST_JWT_SECRET.getBytes(StandardCharsets.UTF_8),
                user,
                "another-issuer",
                now,
                new Date(now.getTime() + 60_000L));

        assertNull(userService.authenticateToken(wrongIssuerToken));
    }

    @Test
    void existingTokenShouldBeRejectedAfterUserIsDisabled() throws Exception {
        UserService userService = new UserService();
        SysUser user = createUser(userService, "jwt-disabled-user", "jwt-disabled@example.com");
        String token = userService.login(loginReq(user.getUsername(), "Correct@123"), "127.0.0.4").getToken();

        userService.updateEnableFlag(user.getId(), false);

        assertNull(userService.authenticateToken(token));
    }

    @Test
    void existingTokenShouldBeRejectedAfterUserIsDeleted() throws Exception {
        UserService userService = new UserService();
        SysUser user = createUser(userService, "jwt-deleted-user", "jwt-deleted@example.com");
        String token = userService.login(loginReq(user.getUsername(), "Correct@123"), "127.0.0.5").getToken();

        userService.deleteUser(user.getId());

        assertNull(userService.authenticateToken(token));
    }

    @Test
    void shortSecretShouldRejectTokenGenerationAndVerification() throws Exception {
        UserService userService = new UserService();
        SysUser user = createUser(userService, "jwt-short-secret-user", "jwt-short@example.com");
        String validToken = userService.login(loginReq(user.getUsername(), "Correct@123"), "127.0.0.6").getToken();

        ConfigProperty.getInstance().getServer().setJwtSecret("too-short");

        assertThrows(IllegalStateException.class, () -> JwtTokenUtil.createToken(user));
        assertNull(userService.authenticateToken(validToken));
    }

    private SysUser createUser(UserService userService, String username, String email) throws Exception {
        CreateUserReq req = new CreateUserReq();
        req.setUsername(username);
        req.setEmail(email);
        req.setRole("user");
        req.setEnableFlag(true);
        req.setEncryptedPassword(encryptPassword("Correct@123"));
        userService.createUser(req);
        return BeanContext.getUserMapper().selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
    }

    private static String createToken(byte[] secret, SysUser user, String issuer, Date issuedAt, Date expiresAt) {
        return JWT.create()
                .setIssuer(issuer)
                .setSubject(String.valueOf(user.getId()))
                .setIssuedAt(issuedAt)
                .setExpiresAt(expiresAt)
                .setPayload("userId", user.getId())
                .setPayload("username", user.getUsername())
                .setPayload("role", user.getRole())
                .sign(JWTSignerUtil.hs256(secret));
    }

    private LoginReq loginReq(String username, String password) throws Exception {
        LoginReq req = new LoginReq();
        req.setUsername(username);
        req.setEncryptedPassword(encryptPassword(password));
        req.setCaptchaId("");
        req.setCaptchaCode("");
        return req;
    }

    private static String encryptPassword(String password) throws Exception {
        PublicKey publicKey = parsePublicKey(PasswordTransportCryptoUtil.getPublicKeyPem());
        OAEPParameterSpec oaepParams = new OAEPParameterSpec(
                "SHA-256",
                "MGF1",
                new MGF1ParameterSpec("SHA-256"),
                PSource.PSpecified.DEFAULT);
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey, oaepParams);
        byte[] encrypted = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    private static PublicKey parsePublicKey(String pem) throws Exception {
        String cleaned = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] encoded = Base64.getDecoder().decode(cleaned);
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(encoded));
    }
}
