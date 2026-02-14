package com.scb.askopt_backend.service;

import com.scb.askopt_backend.config.RedisUtil;
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

        // 4️⃣ 构建 AuthUser 对象（不含 token）
        AuthUser authUser = new AuthUser();
        authUser.setUserId(user.getId());
        authUser.setUsername(username);
        authUser.setRoles(new ArrayList<>(roles));
        authUser.setPermissions(permissions);

        // 生成 AccessToken（短期）
        String accessToken = jwtUtil.generateToken(authUser, 15 * 60_000L); // 15 分钟

        authUser.setAccessToken(accessToken);

        // 6️⃣ 生成 RefreshToken（长期有效，如 7 天）
        String refreshToken = UUID.randomUUID().toString();
        authUser.setRefreshToken(refreshToken);

        // 7️⃣ Redis 缓存 RefreshToken -> username
        redisUtil.set("refresh:" + refreshToken, username, 7 * 24 * 60 * 60); // 7天

        // 8️⃣ 可选：缓存 AccessToken -> AuthUser（短期）以支持快速验证
        redisUtil.set("auth:login:" + username, authUser, 15 * 60); // 15分钟

        return authUser;
    }
    // AccessToken 和 RefreshToken 有效期可以从配置中读取
    private static final long ACCESS_TOKEN_EXPIRE_MS = 15 * 60_000; // 15分钟
    private static final long ACCESS_TOKEN_CACHE_SEC = 15 * 60;    // 15分钟
    private static final long REFRESH_TOKEN_EXPIRE_SEC = 7 * 24 * 60 * 60; // 7天

    public String refreshAccessToken(String refreshToken) {
        // 1️⃣ 校验 refresh token
        String username = redisUtil.get("refresh:" + refreshToken).toString();
        if (username == null) {
            throw new RuntimeException("refresh token invalid or expired");
        }
        // 2️⃣ 查询最新权限信息
        AuthUser authUser = (AuthUser) redisUtil.get("auth:login:" + username);
        if (authUser == null) {
            // 缓存不存在，重新拉取
            SysUser user = userMapper.findByUsername(username);
            List<PermissionMapper.RolePermission> rolePerms =
                    permissionMapper.findRolesAndPermissionsByUserId(user.getId());

            Set<String> roles = new HashSet<>();
            Set<String> permissions = new HashSet<>();
            for (PermissionMapper.RolePermission rp : rolePerms) {
                roles.add(rp.getRoleCode());
                permissions.add(rp.getPermissionCode());
            }

            authUser = new AuthUser();
            authUser.setUserId(user.getId());
            authUser.setUsername(username);
            authUser.setRoles(new ArrayList<>(roles));
            authUser.setPermissions(permissions);
        }

        // 3️⃣ 生成新的 AccessToken
        String newAccessToken = jwtUtil.generateToken(authUser, ACCESS_TOKEN_EXPIRE_MS);

        // 4️⃣ 更新缓存
        authUser.setAccessToken(newAccessToken);
        redisUtil.set("auth:login:" + username, authUser, ACCESS_TOKEN_CACHE_SEC);

        return newAccessToken;
    }
}
