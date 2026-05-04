package com.scb.askopt_backend.service;

import com.scb.askopt_backend.config.RedisUtil;
import com.scb.askopt_backend.entity.SysUser;
import com.scb.askopt_backend.exception.GlobalExceptionHandler;
import com.scb.askopt_backend.exception.ResultCodeEnum;
import com.scb.askopt_backend.mapper.PermissionMapper;
import com.scb.askopt_backend.mapper.UserMapper;
import com.scb.askopt_backend.security.AuthContext;
import com.scb.askopt_backend.security.AuthUser;
import com.scb.askopt_backend.security.JwtUtil;
import com.scb.askopt_backend.vo.LoginVO;
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

    // ====================== 登录：返回 LoginVO ======================
    public LoginVO login(String username, String password) {
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
        authUser.setSuperAdmin(isSuperAdmin);
        authUser.setPermissionVersion(user.getPermissionVersion());

        // 4. 缓存权限
        Set<Long> permissionIds = getUserPermissionIds(userId);
        redisUtil.set("auth:perm:" + userId, permissionIds, REFRESH_TOKEN_EXPIRE_SEC);

        // 5. 生成 token 并返回 LoginVO
        return generateAndCacheTokens(authUser);
    }

    // ====================== ✅ 最终版：刷新 Token ======================
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
        // 🔥 1. 从 Redis 读取最新 permissionVersion
        // ==============================================
        String versionKey = "auth:ver:" + userId;
        Long permissionVersion = redisUtil.getLong(versionKey);

        // 兜底：Redis 无值 → 查数据库最新版本
        if (permissionVersion == null) {
            SysUser user = userMapper.selectById(userId);
            permissionVersion = user.getPermissionVersion();
            redisUtil.set(versionKey, permissionVersion, REFRESH_TOKEN_EXPIRE_SEC);
        }

        // ==============================================
        // 🔥 2. 构建带最新版本号的 AuthUser
        // ==============================================
        boolean isSuperAdmin = userMapper.isSuperAdmin(userId);
        AuthUser authUser = new AuthUser();
        authUser.setUserId(userId);
        authUser.setSuperAdmin(isSuperAdmin);
        authUser.setPermissionVersion(permissionVersion); // ✅ 最新版本必须放进去

        // ==============================================
        // 🔥 3. 生成新 accessToken（带最新版本）并返回
        // ==============================================
        return jwtUtil.generateToken(authUser, ACCESS_TOKEN_EXPIRE_MS);
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

    // ====================== 生成并缓存Token，返回LoginVO ======================
    private LoginVO generateAndCacheTokens(AuthUser authUser) {
        Long userId = authUser.getUserId();

        // 生成 Token
        String accessToken = jwtUtil.generateToken(authUser, ACCESS_TOKEN_EXPIRE_MS);
        String refreshToken = UUID.randomUUID().toString();

        // 封装返回VO
        LoginVO loginVO = new LoginVO();
        loginVO.setAccessToken(accessToken);
        loginVO.setRefreshToken(refreshToken);

        // 缓存到 Redis
        redisUtil.set("auth:ver:" + userId, authUser.getPermissionVersion(), REFRESH_TOKEN_EXPIRE_SEC);
        redisUtil.set("refresh:" + refreshToken, userId, REFRESH_TOKEN_EXPIRE_SEC);

        return loginVO;
    }

    // ====================== 登出 ======================
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) return;
        String key = "refresh:" + refreshToken;
        redisUtil.del(key);
    }
}