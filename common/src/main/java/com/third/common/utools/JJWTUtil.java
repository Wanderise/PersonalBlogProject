package com.third.common.utools;

import com.third.common.constants.JWTConstant;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.JwtParserBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Map;

public class JJWTUtil {
    private static final String SECRET_KEY = JWTConstant.SECRET_KEY;
    private static final Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

    public static String createJWT(Map<String, Object> claims) {
        return Jwts.builder()
                .setClaims(claims)
                .signWith(key)
                .setExpiration(new Date(System.currentTimeMillis() + JWTConstant.EXPIRATION_TIME))
                .compact();
    }

    public static Claims parseJWT(String token) {
        return Jwts.parser()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
