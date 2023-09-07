package com.ytrue.gateway.core;

import com.ytrue.gateway.core.authorization.IAuth;
import com.ytrue.gateway.core.authorization.JwtUtil;
import io.jsonwebtoken.Claims;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import com.ytrue.gateway.core.authorization.auth.AuthService;

/**
 * @author ytrue
 * @date 2023-09-07 14:30
 * @description ShiroTest
 */
public class ShiroTest {

    private final static Logger logger = LoggerFactory.getLogger(ShiroTest.class);

    @Test
    public void test_auth_service() {
        IAuth auth = new AuthService();
        boolean validate = auth.validate("ytrue", "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ5dHJ1ZSIsImV4cCI6MTY5NDY3MzEwMSwiaWF0IjoxNjk0MDY4MzAxLCJrZXkiOiJ5dHJ1ZSJ9.KxJ21TLxBWZGH14QQTk6jl4CXHobIJ9zboiq3hlMO2s");
        System.out.println(validate ? "验证成功" : "验证失败");
    }

    @Test
    public void test_awt() {
        String issuer = "ytrue";
        long ttlMillis = 7 * 24 * 60 * 60 * 1000L;
        Map<String, Object> claims = new HashMap<>();
        claims.put("key", "ytrue");

        // 编码
        String token = JwtUtil.encode(issuer, ttlMillis, claims);
        System.out.println(token);

        // 解码
        Claims parser = JwtUtil.decode(token);
        System.out.println(parser.getSubject());
    }

}
