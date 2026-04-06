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
import java.util.stream.Collectors;

@Slf4j
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

    private static final long ACCESS_TOKEN_EXPIRE_MS = 15 * 60_000L;
    private static final long ACCESS_TOKEN_CACHE_SEC = 15 * 60L;
    private static final long REFRESH_TOKEN_EXPIRE_SEC = 7 * 24 * 60 * 60L;
    private static final long REFRESH_TOKEN_EXTEND_SEC = 24 * 60 * 60L;

    // ====================== 登录：超级精简 ======================
    public AuthUser login(String username, String password) {
        // 1. 验证用户
        SysUser user = userMapper.findByUsername(username);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new GlobalExceptionHandler.LoginException("用户名或密码错误");
        }

        Long userId = user.getId();
        // 2. 判断是否超管
        boolean isSuperAdmin = userMapper.isSuperAdmin(userId);

        // 3. 构建极简 AuthUser
        AuthUser authUser = new AuthUser();
        authUser.setUserId(user.getId());
        authUser.setUsername(user.getUsername());
        authUser.setSuperAdmin(isSuperAdmin);
        authUser.setPermissionVersion(user.getPermissionVersion());

        // ==============================================
        // 🔥 🔥 🔥 修复：登录时缓存权限ID到Redis
        // ==============================================
        Set<Long> permissionIds = getUserPermissionIds(userId);
        redisUtil.set("auth:perm:" + userId, permissionIds, REFRESH_TOKEN_EXPIRE_SEC);

        // 4. 生成 token
        generateAndCacheTokens(authUser);
        return authUser;
    }

    // ====================== 【新增】获取用户所有权限ID ======================
    private Set<Long> getUserPermissionIds(Long userId) {
        // 1. 查询用户所有角色ID
        List<Long> roleIds = userMapper.listRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return new HashSet<>();
        }

        // 2. 查询角色对应的所有权限ID
        Set<Long> permissionIds = new HashSet<>();
        for (Long roleId : roleIds) {
            List<Long> pids = userMapper.listPermissionIdsByRoleId(roleId);
            permissionIds.addAll(pids);
        }
        return permissionIds;
    }

    // ====================== 刷新token ======================
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
        redisUtil.del(refreshKey);

        SysUser user = userMapper.findByUserId(userId);
        if (user == null) {
            throw new GlobalExceptionHandler.LoginException("用户不存在");
        }

        Object redisVerObj = redisUtil.get("auth:ver:" + userId);
        if (redisVerObj == null) {
            throw new GlobalExceptionHandler.LoginException("会话已失效");
        }

        // 重新判断超管
        boolean superAdmin = userMapper.isSuperAdmin(userId);

        AuthUser authUser = new AuthUser();
        authUser.setUserId(user.getId());
        authUser.setUsername(user.getUsername());
        authUser.setSuperAdmin(superAdmin);
        authUser.setPermissionVersion((Long) redisVerObj);

        String newAccessToken = jwtUtil.generateToken(authUser, ACCESS_TOKEN_EXPIRE_MS);
        String newRefreshToken = UUID.randomUUID().toString();

        authUser.setAccessToken(newAccessToken);
        authUser.setRefreshToken(newRefreshToken);

        generateAndCacheTokens(authUser);

        // ==============================================
        // 🔥 🔥 🔥 刷新token时也刷新权限缓存
        // ==============================================
        Set<Long> permissionIds = getUserPermissionIds(userId);
        redisUtil.set("auth:perm:" + userId, permissionIds, REFRESH_TOKEN_EXPIRE_SEC);

        return newAccessToken;
    }

    // ====================== 生成并缓存token ======================
    private void generateAndCacheTokens(AuthUser authUser) {
        String accessToken = jwtUtil.generateToken(authUser, ACCESS_TOKEN_EXPIRE_MS);
        String refreshToken = UUID.randomUUID().toString();

        authUser.setAccessToken(accessToken);
        authUser.setRefreshToken(refreshToken);

        Long userId = authUser.getUserId();

        redisUtil.set("auth:ver:" + userId,
                authUser.getPermissionVersion(),
                REFRESH_TOKEN_EXPIRE_SEC);

        redisUtil.set("refresh:" + refreshToken,
                userId,
                REFRESH_TOKEN_EXPIRE_SEC);
    }
}