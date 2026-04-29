package com.scb.askopt_backend.service;

import com.scb.askopt_backend.config.RedisUtil;
import com.scb.askopt_backend.entity.SysUser;
import com.scb.askopt_backend.exception.ApiException;
import com.scb.askopt_backend.exception.GlobalExceptionHandler;
import com.scb.askopt_backend.exception.ResultCodeEnum;
import com.scb.askopt_backend.mapper.PermissionMapper;
import com.scb.askopt_backend.mapper.UserMapper;
import com.scb.askopt_backend.security.AuthContext;
import com.scb.askopt_backend.security.AuthUser;
import com.scb.askopt_backend.security.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

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

    // 配置常量
    private static final long ACCESS_TOKEN_EXPIRE_MS = 15 * 60_000L;         // 15分钟
    private static final long REFRESH_TOKEN_EXPIRE_SEC = 7 * 24 * 60 * 60L;   // 7天

    // ====================== 登录 ======================
    public AuthUser login(String username, String password) {
        // 1. 验证用户
        SysUser user = userMapper.findByUsername(username);
        // 账号禁用 → 登录异常 401
        if (user != null && !Boolean.TRUE.equals(user.getEnabled())) {
            throw new GlobalExceptionHandler.LoginException(ResultCodeEnum.USER_DISABLED);
        }

        // 用户名/密码错误
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new GlobalExceptionHandler.LoginException(ResultCodeEnum.LOGIN_ERROR);
        }

        Long userId = user.getId();
        // 2. 判断是否超管
        boolean isSuperAdmin = userMapper.isSuperAdmin(userId);

        // 3. 构建 AuthUser
        AuthUser authUser = new AuthUser();
        authUser.setUserId(user.getId());
        authUser.setUsername(user.getUsername());
        authUser.setSuperAdmin(isSuperAdmin);
        authUser.setPermissionVersion(user.getPermissionVersion());

        // 4. 缓存权限
        Set<Long> permissionIds = getUserPermissionIds(userId);
        redisUtil.set("auth:perm:" + userId, permissionIds, REFRESH_TOKEN_EXPIRE_SEC);

        // 5. 生成 token
        generateAndCacheTokens(authUser);
        return authUser;
    }

    // ====================== 获取用户权限ID ======================
    private Set<Long> getUserPermissionIds(Long userId) {
        List<Long> roleIds = userMapper.listRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return new HashSet<>();
        }

        Set<Long> permissionIds = new HashSet<>();
        for (Long roleId : roleIds) {
            List<Long> pids = userMapper.listPermissionIdsByRoleId(roleId);
            permissionIds.addAll(pids);
        }
        return permissionIds;
    }

    // ====================== ✅ 刷新Token：只返回新的 accessToken ======================
// ====================== ✅ 刷新 Token（单一职责）======================
    public String refreshAccessToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new GlobalExceptionHandler.LoginException(ResultCodeEnum.TOKEN_EMPTY);
        }

        String refreshKey = "refresh:" + refreshToken;
        Long userId = redisUtil.getLong(refreshKey);

        if (userId == null) {
            throw new GlobalExceptionHandler.LoginException(ResultCodeEnum.TOKEN_INVALID);
        }

        // ==============================================
        // 🔥 🔥 🔥 全部来自 ThreadLocal（前端旧token带来的值）
        // ==============================================
        Long Version = AuthContext.getPermissionVersion();
        boolean isSuperAdmin = AuthContext.isSuperAdmin();

        AuthUser authUser = new AuthUser();
        authUser.setUserId(userId);
        authUser.setSuperAdmin(isSuperAdmin);
        authUser.setPermissionVersion(Version);

        return jwtUtil.generateToken(authUser, ACCESS_TOKEN_EXPIRE_MS);
    }
    // ====================== 生成并缓存Token（登录时使用） ======================
    private void generateAndCacheTokens(AuthUser authUser) {
        Long userId = authUser.getUserId();

        // 生成 Token
        String accessToken = jwtUtil.generateToken(authUser, ACCESS_TOKEN_EXPIRE_MS);
        String refreshToken = UUID.randomUUID().toString();

        authUser.setAccessToken(accessToken);
        authUser.setRefreshToken(refreshToken);

        // 缓存到 Redis
        redisUtil.set("auth:ver:" + userId, authUser.getPermissionVersion(), REFRESH_TOKEN_EXPIRE_SEC);
        redisUtil.set("refresh:" + refreshToken, userId, REFRESH_TOKEN_EXPIRE_SEC);
    }

    // ====================== 登出 ======================
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) return;
        String key = "refresh:" + refreshToken;
        redisUtil.del(key);
    }
}