package com.third.common.utools;

import com.third.common.constants.JWTConstant;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Component
public class JJWTUtil {

    private static volatile SecretKey key;

    @Value("${jwt.secret}")
    public void setSecretKey(String secretKey) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("JWT secret must be configured");
        }
        byte[] secretBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes for HS256");
        }
        key = Keys.hmacShaKeyFor(secretBytes);
    }

    private static SecretKey getKey() {
        if (key == null) {
            throw new IllegalStateException("JWT secret key not initialized");
        }
        return key;
    }

    public static String createJWT(Map<String, Object> claims) {
        return Jwts.builder()
                .claims(claims)
                .signWith(getKey())
                .expiration(new Date(System.currentTimeMillis() + JWTConstant.EXPIRATION_TIME))
                .compact();
    }

    public static Claims parseJWT(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
