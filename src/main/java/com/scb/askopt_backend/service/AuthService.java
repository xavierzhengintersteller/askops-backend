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
        AuthUser authUser = buildAuthUser(user, roleIds, permissionIds, groupIds, superAdmin);

        // 5️⃣ 生成 Token 并缓存
        generateAndCacheTokens(authUser);

        log.info("用户{}登录成功，超级管理员标识：{}", username, superAdmin);
        return authUser;
    }

    // ========== 优化后的刷新 Token 方法 ==========
    public String refreshAccessToken(String refreshToken) {
        // 1️⃣ 校验 RefreshToken 合法性（空值+Redis存在性）
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new GlobalExceptionHandler.LoginException("refresh token 不能为空");
        }
        String refreshKey = "refresh:" + refreshToken;
        Object usernameObj = redisUtil.get(refreshKey);
        if (usernameObj == null) {
            log.error("刷新AccessToken失败：refreshToken{}无效或已过期", refreshToken);
            throw new GlobalExceptionHandler.LoginException("refresh token 无效或已过期");
        }
        String username = usernameObj.toString();

        // 2️⃣ 获取/重建 AuthUser（适配新结构）
        AuthUser authUser = (AuthUser) redisUtil.get("auth:login:" + username);
        if (authUser == null) {
            log.warn("用户{}的AuthUser缓存失效，重新从数据库加载", username);
            // 缓存不存在，重新查询数据并构建 AuthUser
            SysUser user = userMapper.findByUsername(username);
            if (user == null) {
                throw new GlobalExceptionHandler.LoginException("用户不存在");
            }
            // 重新查询权限数据
            List<PermissionMapper.RolePermissionId> rolePermIds =
                    permissionMapper.findRoleIdsAndPermissionIdsByUserId(user.getId());
            Set<Long> groupIds = permissionMapper.findGroupIdsByUserId(user.getId());
            Long superAdminRoleId = permissionMapper.getSuperAdminRoleId();

            // 聚合数据
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
            // 重建 AuthUser
            authUser = buildAuthUser(user, roleIds, permissionIds, groupIds, superAdmin);
            // 重新缓存 AuthUser
            redisUtil.set("auth:login:" + username, authUser, ACCESS_TOKEN_CACHE_SEC);
        }

        // 3️⃣ 生成新的 AccessToken
        String newAccessToken = jwtUtil.generateToken(authUser, ACCESS_TOKEN_EXPIRE_MS);
        authUser.setAccessToken(newAccessToken);

        // 4️⃣ 滑动刷新 RefreshToken 有效期（优化体验，避免7天到期后重新登录）
        redisUtil.expire(refreshKey, REFRESH_TOKEN_EXPIRE_SEC + REFRESH_TOKEN_EXTEND_SEC);
        // 更新缓存中的 AuthUser（仅更新 AccessToken）
        redisUtil.set("auth:login:" + username, authUser, ACCESS_TOKEN_CACHE_SEC);

        log.info("用户{}刷新AccessToken成功", username);
        return newAccessToken;
    }

    // ========== 私有工具方法：构建 AuthUser（复用逻辑） ==========
    private AuthUser buildAuthUser(SysUser user, Set<Long> roleIds, Set<Long> permissionIds,
                                   Set<Long> groupIds, boolean superAdmin) {
        AuthUser authUser = new AuthUser();
        authUser.setUserId(user.getId());
        authUser.setUsername(user.getUsername());
        authUser.setSuperAdmin(superAdmin);
        authUser.setRoleIds(roleIds);
        authUser.setPermissionIds(permissionIds);
        authUser.setGroupIds(groupIds);
        return authUser;
    }

    // ========== 私有工具方法：生成并缓存 Token（复用逻辑） ==========
    private void generateAndCacheTokens(AuthUser authUser) {
        // 生成 AccessToken
        String accessToken = jwtUtil.generateToken(authUser, ACCESS_TOKEN_EXPIRE_MS);
        authUser.setAccessToken(accessToken);

        // 生成 RefreshToken
        String refreshToken = UUID.randomUUID().toString();
        authUser.setRefreshToken(refreshToken);

        // 缓存 RefreshToken（7天）
        redisUtil.set("refresh:" + refreshToken, authUser.getUsername(), REFRESH_TOKEN_EXPIRE_SEC);

        // 缓存 AuthUser（15分钟）
        redisUtil.set("auth:login:" + authUser.getUsername(), authUser, ACCESS_TOKEN_CACHE_SEC);
    }
}