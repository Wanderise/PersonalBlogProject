package com.third.server;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.security.Key;

@SpringBootTest
class ServerApplicationTests {

    @Test
    void contextLoads() {

// 生成安全的随机密钥
        Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

// 将密钥编码成 Base64 字符串（用于配置文件等）
        String base64Key = Encoders.BASE64.encode(key.getEncoded());
        System.out.println("Base64 密钥：" + base64Key);
    }

}
