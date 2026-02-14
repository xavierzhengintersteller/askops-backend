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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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



    @PostMapping("/register")
    public ApiResponse<Void> register(@RequestBody LoginRequest request) {

        // 1. 参数基础校验
        if (request.getUsername() == null || request.getPassword() == null) {
            return ApiResponse.error(400, "用户名或密码不能为空");
        }

        // 2. 用户是否已存在
        SysUser existUser = userMapper.findByUsername(request.getUsername());
        if (existUser != null) {
            return ApiResponse.error(409, "用户名已存在");
        }

        // 3. 构建用户对象
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true);

        // 4. 保存
        userMapper.insert(user);

        // 5. assign default role "OTHER" (if exists)
        userMapper.assignRoleToUserByCode(user.getId(), "OTHER");

        return ApiResponse.success();
    }


}
