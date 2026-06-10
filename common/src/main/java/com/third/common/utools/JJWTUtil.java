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

// JWT工具类，key通过Spring @Value注入，getKey()防御性检查防止未初始化
@Component
public class JJWTUtil {

    private static volatile SecretKey key;

    @Value("${jwt.secret}")
    public void setSecretKey(String secretKey) {
        key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    // 防御性检查，防止Spring注入时序问题导致使用未初始化key
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
