package com.scb.askopt_backend.security;

import lombok.Data;

import java.util.List;
import java.util.Set;
@Data
public class AuthUser {
    private Long userId;
    private String username;
    private List<String> roles;
    private Set<String> permissions;
    private String accessToken;   // 短期 JWT
    private String refreshToken;  // 长期刷新 token
}

