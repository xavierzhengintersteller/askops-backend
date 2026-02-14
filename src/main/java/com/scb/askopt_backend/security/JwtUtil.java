package com.scb.askopt_backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Component
public class JwtUtil {

    private static final String SECRET = "demo-secret-key-demo-secret-key-demo-secret-key-demo-secret-key"; // 256 bit+
    private static final long EXPIRE_MS = 24 * 60 * 60 * 1000; // 1天
    private final Key key = Keys.hmacShaKeyFor(SECRET.getBytes());
    /**
     * 根据 username + roles + permissions 生成 token
     */
    public String generateToken(AuthUser user, long expireMillis) {
        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("roles", user.getRoles())
                .claim("permissions", user.getPermissions())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expireMillis))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes()))
                .compact();
    }

    /**
     * 解析 token，返回 Claims
     */
    public Claims parse(String token) throws JwtException {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public List<String> getRoles(String token) {
        Object rolesObj = parseClaims(token).get("roles");
        if (rolesObj instanceof List) return (List<String>) rolesObj;
        return null;
    }

    public Set<String> getPermissions(String token) {
        Object permsObj = parseClaims(token).get("permissions");
        if (permsObj instanceof List) return Set.copyOf((List<String>) permsObj);
        if (permsObj instanceof Set) return (Set<String>) permsObj;
        return null;
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
