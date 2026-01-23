package com.scb.askopt_backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtil {

    private static final String SECRET = "demo-secret-key-demo-secret-key-demo-secret-key-demo-secret-key"; // 256 bit+

    public String generateToken(String username){
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis()+24*60*60*1000)) // 1天有效
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes()))
                .compact();
    }

    public String getUsername(String token){
        return Jwts.parserBuilder()
                .setSigningKey(SECRET.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String token){
        try {
            Jwts.parserBuilder().setSigningKey(SECRET.getBytes()).build().parseClaimsJws(token);
            return true;
        } catch (Exception e){
            return false;
        }
    }
}
