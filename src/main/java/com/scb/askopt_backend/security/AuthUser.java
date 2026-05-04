package com.scb.askopt_backend.security;

import lombok.Data;

import java.util.List;
import java.util.Set;
@Data
public class AuthUser {
    private Long userId;
    private boolean superAdmin;
    private Long permissionVersion;
}

