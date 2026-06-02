package com.scb.askopt_backend.controller;

import com.scb.askopt_backend.annotation.AuditLog;
import com.scb.askopt_backend.constant.AuditConstant;
import com.scb.askopt_backend.dto.LoginRequest;
import com.scb.askopt_backend.entity.SysUser;
import com.scb.askopt_backend.mapper.PermissionMapper;
import com.scb.askopt_backend.mapper.UserMapper;
import com.scb.askopt_backend.security.JwtUtil;
import com.scb.askopt_backend.service.AuthService;
import com.scb.askopt_backend.vo.ApiResponse;
import com.scb.askopt_backend.vo.LoginVO;
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
    public ApiResponse<LoginVO> login(@RequestBody LoginRequest request) {
        LoginVO loginVO = authService.login(request.getUsername(), request.getPassword());
        return ApiResponse.success(loginVO);
    }

    @AuditLog(module = "AUTH", operation = AuditConstant.CREATE)
    @PostMapping("/register")
    public ApiResponse<String> register(@RequestBody LoginRequest request) {
        SysUser existingUser = userMapper.findByUsername(request.getUsername());
        if (existingUser != null) {
            return ApiResponse.error(400, "用户名已存在");
        }
        try {
            SysUser user = new SysUser();
            user.setUsername(request.getUsername());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            userMapper.insert(user);
            return ApiResponse.success("注册成功");
        } catch (Exception e) {
            return ApiResponse.error(500, "注册失败: " + e.getMessage());
        }
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