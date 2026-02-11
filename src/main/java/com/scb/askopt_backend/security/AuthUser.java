package com.scb.askopt_backend.security;

import lombok.Data;

import java.util.Set;
@Data
public class AuthUser {
    private Long userId;
    private String username;
    private Set<String> permissions;
}

