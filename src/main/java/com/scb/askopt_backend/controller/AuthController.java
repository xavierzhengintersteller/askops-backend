package com.scb.askopt_backend.controller;

import com.scb.askopt_backend.dto.LoginRequest;
import com.scb.askopt_backend.entity.SysUser;
import com.scb.askopt_backend.mapper.PermissionMapper;
import com.scb.askopt_backend.mapper.UserMapper;
import com.scb.askopt_backend.security.AuthUser;
import com.scb.askopt_backend.security.JwtUtil;
import com.scb.askopt_backend.service.AuthService;
import com.scb.askopt_backend.vo.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private PermissionMapper permissionMapper;
    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ApiResponse<AuthUser> login(@RequestBody LoginRequest request) {

        AuthUser user = authService.login(
                request.getUsername(),
                request.getPassword());

        return ApiResponse.success(user);
    }


    @PostMapping("/token/refresh")
    public ApiResponse<String> refreshToken(@RequestParam String refreshToken) {
        try {
            String newAccessToken = authService.refreshAccessToken(refreshToken);
            return ApiResponse.success(newAccessToken);
        } catch (RuntimeException e) {
            return ApiResponse.error(401, e.getMessage());
        }
    }
}
