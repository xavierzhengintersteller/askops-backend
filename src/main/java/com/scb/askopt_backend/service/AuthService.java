package com.scb.askopt_backend.service;

import com.scb.askopt_backend.config.RedisUtil;
import com.scb.askopt_backend.entity.SysUser;
import com.scb.askopt_backend.exception.GlobalExceptionHandler;
import com.scb.askopt_backend.mapper.PermissionMapper;
import com.scb.askopt_backend.mapper.UserMapper;
import com.scb.askopt_backend.security.AuthUser;
import com.scb.askopt_backend.security.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j // 新增日志注解
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

    // ========== 常量统一管理（从配置读取更佳，此处先固化） ==========
    // AccessToken 有效期（15分钟，毫秒）
    private static final long ACCESS_TOKEN_EXPIRE_MS = 15 * 60_000L;
    // AccessToken 缓存有效期（15分钟，秒）
    private static final long ACCESS_TOKEN_CACHE_SEC = 15 * 60L;
    // RefreshToken 有效期（7天，秒）
    private static final long REFRESH_TOKEN_EXPIRE_SEC = 7 * 24 * 60 * 60L;
    // RefreshToken 滑动过期延长时间（1天，秒），避免频繁登录
    private static final long REFRESH_TOKEN_EXTEND_SEC = 24 * 60 * 60L;

    // ========== 登录方法（保留优化后的逻辑） ==========
    public AuthUser login(String username, String password) {
        // 1️⃣ 验证用户
        SysUser user = userMapper.findByUsername(username);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            log.error("登录失败：用户名{}不存在或密码错误", username);
            throw new GlobalExceptionHandler.LoginException("用户名或密码错误");
        }

        // 2️⃣ 批量查询核心数据
        List<PermissionMapper.RolePermissionId> rolePermIds =
                permissionMapper.findRoleIdsAndPermissionIdsByUserId(user.getId());
        Set<Long> groupIds = permissionMapper.findGroupIdsByUserId(user.getId());
        Long superAdminRoleId = permissionMapper.getSuperAdminRoleId();
        Long PermissionVersion = user.getPermissionVersion(); // 从用户表获取权限版本

        // 3️⃣ 聚合角色ID、权限ID，判断超级管理员
        Set<Long> roleIds = new HashSet<>();
        Set<Long> permissionIds = new HashSet<>();
        boolean superAdmin = false;

        for (PermissionMapper.RolePermissionId rp : rolePermIds) {
            if (rp.getRoleId() != null) {
                roleIds.add(rp.getRoleId());
                if (superAdminRoleId != null && superAdminRoleId.equals(rp.getRoleId())) {
                    superAdmin = true;
                }
            }
            if (rp.getPermissionId() != null) {
                permissionIds.add(rp.getPermissionId());
            }
        }

        // 4️⃣ 构建 AuthUser
        AuthUser authUser = buildAuthUser(user, roleIds, permissionIds, groupIds, superAdmin, PermissionVersion);

        // 5️⃣ 生成 Token 并缓存
        generateAndCacheTokens(authUser);

        log.info("用户{}登录成功，超级管理员标识：{}", username, superAdmin);
        return authUser;
    }

    public String refreshAccessToken(String refreshToken) {

        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new GlobalExceptionHandler.LoginException("refresh token 不能为空");
        }

        String refreshKey = "refresh:" + refreshToken;
        Object userIdObj = redisUtil.get(refreshKey);
        if (userIdObj == null) {
            throw new GlobalExceptionHandler.LoginException("refresh token 无效或已过期");
        }
        Long userId = Long.valueOf(userIdObj.toString());

        // ✅ 删除旧 refreshToken（轮换）
        redisUtil.del(refreshKey);

        // ✅ 每次 refresh 都重新查数据库
        SysUser user = userMapper.findByUserId(userId);
        if (user == null) {
            throw new GlobalExceptionHandler.LoginException("用户不存在");
        }

        // 🔥 读取当前 Redis version
        Object redisVerObj = redisUtil.get("auth:ver:" + userId);
        if (redisVerObj == null) {
            throw new GlobalExceptionHandler.LoginException("会话已失效");
        }
        Long currentVersion = Long.valueOf(redisVerObj.toString());

        // 🔄 查询最新权限、角色、分组
        List<PermissionMapper.RolePermissionId> rolePermIds =
                permissionMapper.findRoleIdsAndPermissionIdsByUserId(user.getId());
        Set<Long> groupIds = permissionMapper.findGroupIdsByUserId(user.getId());
        Long superAdminRoleId = permissionMapper.getSuperAdminRoleId();
        Set<Long> roleIds = new HashSet<>();
        Set<Long> permissionIds = new HashSet<>();
        boolean superAdmin = false;

        for (PermissionMapper.RolePermissionId rp : rolePermIds) {
            if (rp.getRoleId() != null) {
                roleIds.add(rp.getRoleId());
                if (superAdminRoleId != null && superAdminRoleId.equals(rp.getRoleId())) {
                    superAdmin = true;
                }
            }
            if (rp.getPermissionId() != null) {
                permissionIds.add(rp.getPermissionId());
            }
        }

        AuthUser authUser = buildAuthUser(user, roleIds, permissionIds, groupIds, superAdmin, currentVersion);

        // ✅ 生成新的 AccessToken
        String newAccessToken = jwtUtil.generateToken(authUser, ACCESS_TOKEN_EXPIRE_MS);
        authUser.setAccessToken(newAccessToken);

        // 🔥 更新 Redis 权限缓存 auth:perm:{userId}
        redisUtil.set("auth:perm:" + userId, permissionIds, REFRESH_TOKEN_EXPIRE_SEC);

        // 🔄 可选：生成新 refreshToken 并缓存（安全轮换）
        String newRefreshToken = UUID.randomUUID().toString();
        authUser.setRefreshToken(newRefreshToken);
        redisUtil.set("refresh:" + newRefreshToken, userId, REFRESH_TOKEN_EXPIRE_SEC);

        return newAccessToken;
    }
    // ========== 私有工具方法：构建 AuthUser（复用逻辑） ==========
    private AuthUser buildAuthUser(SysUser user, Set<Long> roleIds, Set<Long> permissionIds,
                                   Set<Long> groupIds, boolean superAdmin, Long PermissionVersion) {
        AuthUser authUser = new AuthUser();
        authUser.setUserId(user.getId());
        authUser.setUsername(user.getUsername());
        authUser.setSuperAdmin(superAdmin);
        authUser.setRoleIds(roleIds);
        authUser.setPermissionIds(permissionIds);
        authUser.setGroupIds(groupIds);
        authUser.setPermissionVersion(user.getPermissionVersion());
        return authUser;
    }
    private void generateAndCacheTokens(AuthUser authUser) {

        String accessToken = jwtUtil.generateToken(authUser, ACCESS_TOKEN_EXPIRE_MS);
        authUser.setAccessToken(accessToken);

        String refreshToken = UUID.randomUUID().toString();
        authUser.setRefreshToken(refreshToken);

        Long userId = authUser.getUserId();

        // 🔥 Redis 存 version
        redisUtil.set("auth:ver:" + userId,
                authUser.getPermissionVersion(),
                REFRESH_TOKEN_EXPIRE_SEC);
        // 🔥 Redis 存 permissionIds
        redisUtil.set("auth:perm:" + userId,
                authUser.getPermissionIds(),
                REFRESH_TOKEN_EXPIRE_SEC);

        redisUtil.set("refresh:" + refreshToken,
                userId,
                REFRESH_TOKEN_EXPIRE_SEC);
    }

}