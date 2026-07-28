package com.scb.tb.askopt_backend.security;

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

    @Value("${jwt.secret}")
    private String secret;

    private Key key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(AuthUser authUser, long expireMs) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", authUser.getUserId());
        claims.put("ver", authUser.getPermissionVersion());
        claims.put("superAdmin", authUser.isSuperAdmin()); // 把超管写进JWT

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(String.valueOf(authUser.getUserId()))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expireMs))
                .setIssuer("askopt-backend")
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parse(String token) throws JwtException {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .requireIssuer("askopt-backend")
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}