package com.cky.proxy.server.util;

import cn.hutool.crypto.digest.BCrypt;

public final class PasswordSecurityUtil {
    private static final int BCRYPT_COST = 12;

    private PasswordSecurityUtil() {
    }

    public static String hashPassword(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            throw new RuntimeException("密码不能为空");
        }
        return BCrypt.hashpw(plainText, BCrypt.gensalt(BCRYPT_COST));
    }

    public static boolean matches(String plainText, String hashedPassword) {
        if (plainText == null || plainText.isBlank() || hashedPassword == null || hashedPassword.isBlank()) {
            return false;
        }
        if (!isHashed(hashedPassword)) {
            return plainText.equals(hashedPassword);
        }
        return BCrypt.checkpw(plainText, hashedPassword);
    }

    public static boolean isHashed(String passwordValue) {
        if (passwordValue == null) {
            return false;
        }
        return passwordValue.startsWith("$2a$") || passwordValue.startsWith("$2b$") || passwordValue.startsWith("$2y$");
    }
}
