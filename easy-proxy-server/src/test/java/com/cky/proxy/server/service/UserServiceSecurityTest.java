package com.cky.proxy.server.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cky.proxy.server.config.ConfigProperty;
import com.cky.proxy.server.config.DatabaseProperty;
import com.cky.proxy.server.config.ServerProperty;
import com.cky.proxy.server.domain.dto.CreateUserReq;
import com.cky.proxy.server.domain.dto.LoginReq;
import com.cky.proxy.server.domain.entity.SysUser;
import com.cky.proxy.server.util.BeanContext;
import com.cky.proxy.server.util.CertGenerator;
import com.cky.proxy.server.util.PasswordSecurityUtil;
import com.cky.proxy.server.util.PasswordTransportCryptoUtil;

class UserServiceSecurityTest {

    @BeforeAll
    static void initEnvironment() throws Exception {
        DatabaseProperty db = new DatabaseProperty();
        db.setUrl("jdbc:h2:./data/database");
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
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
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
