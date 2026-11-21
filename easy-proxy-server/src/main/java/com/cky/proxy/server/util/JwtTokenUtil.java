package com.cky.proxy.server.util;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import com.cky.proxy.server.config.ConfigProperty;
import com.cky.proxy.server.config.ServerProperty;
import com.cky.proxy.server.domain.entity.SysUser;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTValidator;
import cn.hutool.jwt.signers.JWTSigner;
import cn.hutool.jwt.signers.JWTSignerUtil;

/**
 * Creates and validates the server's signed access tokens.
 */
public final class JwtTokenUtil {
    public static final String ISSUER = "easy-proxy";
    public static final int MIN_SECRET_BYTES = 32;

    private JwtTokenUtil() {
    }

    public static String createToken(SysUser user) {
        if (user == null || user.getId() == null || user.getUsername() == null || user.getRole() == null) {
            throw new IllegalStateException("JWT user information is incomplete");
        }

        ServerProperty server = ConfigProperty.getInstance().getServer();
        if (server == null) {
            throw new IllegalStateException("Server configuration is unavailable");
        }

        Integer expireMinutes = server.getJwtExpireMinutes();
        if (expireMinutes == null || expireMinutes <= 0) {
            throw new IllegalStateException("jwtExpireMinutes must be positive");
        }

        Date issuedAt = new Date();
        Date expiresAt = new Date(issuedAt.getTime() + expireMinutes.longValue() * 60_000L);
        JWTSigner signer = createSigner(server.getJwtSecret());

        return JWT.create()
                .setIssuer(ISSUER)
                .setSubject(String.valueOf(user.getId()))
                .setIssuedAt(issuedAt)
                .setExpiresAt(expiresAt)
                .setPayload("userId", user.getId())
                .setPayload("username", user.getUsername())
                .setPayload("role", user.getRole())
                .sign(signer);
    }

    public static Integer verifyAndGetUserId(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("JWT token is required");
        }

        JWTSigner signer = createSigner(getConfiguredSecret());
        JWT jwt = JWT.of(token);
        JWTValidator.of(jwt)
                .validateAlgorithm(signer)
                .validateDate();

        Object issuer = jwt.getPayload("iss");
        if (!ISSUER.equals(issuer)) {
            throw new IllegalArgumentException("Invalid JWT issuer");
        }

        Object expiresAt = jwt.getPayload("exp");
        Object issuedAt = jwt.getPayload("iat");
        if (expiresAt == null || issuedAt == null) {
            throw new IllegalArgumentException("JWT timestamps are required");
        }

        Integer userId = parseUserId(jwt.getPayload("userId"));
        Object subject = jwt.getPayload("sub");
        if (userId == null || !String.valueOf(userId).equals(subject)) {
            throw new IllegalArgumentException("Invalid JWT subject");
        }

        return userId;
    }

    private static JWTSigner createSigner(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("jwtSecret must be configured");
        }

        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("jwtSecret must contain at least " + MIN_SECRET_BYTES + " UTF-8 bytes");
        }
        return JWTSignerUtil.hs256(secretBytes);
    }

    private static String getConfiguredSecret() {
        ServerProperty server = ConfigProperty.getInstance().getServer();
        if (server == null) {
            throw new IllegalStateException("Server configuration is unavailable");
        }
        return server.getJwtSecret();
    }

    private static Integer parseUserId(Object value) {
        long userId;
        if (value instanceof Number number) {
            userId = number.longValue();
        } else if (value instanceof String text && text.matches("\\d+")) {
            try {
                userId = Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        } else {
            return null;
        }

        if (userId <= 0 || userId > Integer.MAX_VALUE) {
            return null;
        }
        return (int) userId;
    }
}
