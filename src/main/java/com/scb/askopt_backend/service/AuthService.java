package com.scb.askopt_backend.service;

import com.scb.askopt_backend.config.RedisUtil;
import com.scb.askopt_backend.dto.RequestAuthUser;
import com.scb.askopt_backend.entity.SysUser;
import com.scb.askopt_backend.mapper.PermissionMapper;
import com.scb.askopt_backend.mapper.UserMapper;
import com.scb.askopt_backend.security.AuthUser;
import com.scb.askopt_backend.security.JwtUtil;
import com.scb.askopt_backend.vo.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.scb.askopt_backend.exception.GlobalExceptionHandler;
import org.springframework.stereotype.Service;


import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class AuthService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final long TOKEN_EXPIRE_SECONDS = 3600; // 1小时

    public AuthUser login(String username, String password) {
        // 1️⃣ 验证用户
        SysUser user = userMapper.findByUsername(username);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new GlobalExceptionHandler.LoginException("用户名或密码错误");
        }

        // 2️⃣ 查询角色 + 权限（一次查询）
        List<PermissionMapper.RolePermission> rolePerms =
                permissionMapper.findRolesAndPermissionsByUserId(user.getId());

        // 3️⃣ 聚合角色和权限
        Set<String> roles = new HashSet<>();
        Set<String> permissions = new HashSet<>();
        for (PermissionMapper.RolePermission rp : rolePerms) {
            roles.add(rp.getRoleCode());
            permissions.add(rp.getPermissionCode());
        }

        // 4️⃣ 构建 AuthUser
        AuthUser authUser = new AuthUser();
        authUser.setUserId(user.getId());
        authUser.setUsername(username);
        authUser.setRoles(new ArrayList<>(roles));
        authUser.setPermissions(permissions);

        // 5️⃣ 生成 token
        String token = jwtUtil.generateToken(authUser);
        authUser.setToken(token);

        // 6️⃣ Redis 缓存 token -> AuthUser
        String redisKey = "auth:login:" + username;
        redisUtil.set(redisKey, authUser, TOKEN_EXPIRE_SECONDS);
        return authUser;
    }
}
