package com.cky.proxy.server.util;

import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.Base64;

import javax.crypto.Cipher;

import com.cky.proxy.server.config.ConfigProperty;

public final class PasswordTransportCryptoUtil {
    private static final String KEY_ALIAS = "easyproxy";

    private static volatile PrivateKey privateKey;
    private static volatile PublicKey publicKey;

    private PasswordTransportCryptoUtil() {
    }

    public static String getPublicKeyPem() {
        PublicKey key = getPublicKey();
        String base64 = Base64.getMimeEncoder(64, new byte[] { '\n' }).encodeToString(key.getEncoded());
        return "-----BEGIN PUBLIC KEY-----\n" + base64 + "\n-----END PUBLIC KEY-----";
    }

    public static String decryptPassword(String encryptedPassword) {
        if (encryptedPassword == null || encryptedPassword.isBlank()) {
            throw new RuntimeException("密码密文不能为空");
        }
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            cipher.init(Cipher.DECRYPT_MODE, getPrivateKey());
            byte[] decoded = Base64.getDecoder().decode(encryptedPassword);
            byte[] plainBytes = cipher.doFinal(decoded);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("密码解密失败");
        }
    }

    private static PrivateKey getPrivateKey() {
        if (privateKey == null) {
            synchronized (PasswordTransportCryptoUtil.class) {
                if (privateKey == null) {
                    loadKeys();
                }
            }
        }
        return privateKey;
    }

    private static PublicKey getPublicKey() {
        if (publicKey == null) {
            synchronized (PasswordTransportCryptoUtil.class) {
                if (publicKey == null) {
                    loadKeys();
                }
            }
        }
        return publicKey;
    }

    private static void loadKeys() {
        try (FileInputStream fis = new FileInputStream(CertGenerator.getJksCertPath())) {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            char[] password = ConfigProperty.getInstance().getServer().getCertPassword().toCharArray();
            keyStore.load(fis, password);

            Key key = keyStore.getKey(KEY_ALIAS, password);
            if (!(key instanceof PrivateKey pk)) {
                throw new RuntimeException("未找到可用的私钥");
            }
            Certificate certificate = keyStore.getCertificate(KEY_ALIAS);
            if (certificate == null) {
                throw new RuntimeException("未找到可用的公钥证书");
            }

            privateKey = pk;
            publicKey = certificate.getPublicKey();
        } catch (Exception e) {
            throw new RuntimeException("加载密码传输密钥失败", e);
        }
    }
}
