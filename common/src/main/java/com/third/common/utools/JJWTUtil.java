package com.third.common.utools;

import com.third.common.constants.JWTConstant;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Map;

@Component
public class JJWTUtil {

    private static Key key;

    @Value("${jwt.secret}")
    public void setSecretKey(String secretKey) {
        key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public static String createJWT(Map<String, Object> claims) {
        return Jwts.builder()
                .claims(claims)
                .signWith(key)
                .expiration(new Date(System.currentTimeMillis() + JWTConstant.EXPIRATION_TIME))
                .compact();
    }

    public static Claims parseJWT(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
