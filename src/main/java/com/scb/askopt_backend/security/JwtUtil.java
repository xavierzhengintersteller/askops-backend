package com.scb.askopt_backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    // ✅ 从配置文件读取
    @Value("${jwt.secret}")
    private String secret;

    private Key key;

    // ✅ 统一初始化 Key（只执行一次）
    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT（只包含 uid + version）
     */
    public String generateToken(AuthUser authUser, long expireMs) {

        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", authUser.getUserId());
        claims.put("ver", authUser.getPermissionVersion());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(String.valueOf(authUser.getUserId()))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expireMs))
                .setIssuer("askopt-backend") // 推荐加 issuer
                .signWith(key, SignatureAlgorithm.HS256) // ✅ 统一使用 key
                .compact();
    }

    /**
     * 解析 JWT
     */
    public Claims parse(String token) throws JwtException {

        return Jwts.parserBuilder()
                .setSigningKey(key) // ✅ 统一使用同一个 key
                .requireIssuer("askopt-backend") // 可选增强安全
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 简化校验方法
     */
    public boolean validateToken(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}