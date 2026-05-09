package com.scb.askopt_backend.service;

import com.scb.askopt_backend.config.RedisUtil;
import com.scb.askopt_backend.constant.RedisConstants;
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
    private static final long ACCESS_TOKEN_EXPIRE_MS = RedisConstants.ACCESS_TOKEN_EXPIRE_MS;
    private static final long REFRESH_TOKEN_EXPIRE_SEC = RedisConstants.REFRESH_TOKEN_EXPIRE_SEC;

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

    // ====================== ✅ 最终修复版：刷新 Token ======================
    public String refreshAccessToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new GlobalExceptionHandler.LoginException(ResultCodeEnum.TOKEN_EMPTY);
        }

        String refreshKey = "refresh:" + refreshToken;
        Long userId = redisUtil.getLong(refreshKey);

        if (userId == null) {
            throw new GlobalExceptionHandler.LoginException(ResultCodeEnum.TOKEN_INVALID);
        }

        // ====================== 自动续期所有 Redis Key ======================
        redisUtil.expire(refreshKey, REFRESH_TOKEN_EXPIRE_SEC);
        redisUtil.expire("auth:ver:" + userId, REFRESH_TOKEN_EXPIRE_SEC);
        redisUtil.expire("auth:perm:" + userId, REFRESH_TOKEN_EXPIRE_SEC);

        // ====================== 从 Redis 获取最新权限版本 ======================
        String versionKey = "auth:ver:" + userId;
        Long permissionVersion = redisUtil.getLong(versionKey);

        // 兜底：Redis 没有就查库并重建
        if (permissionVersion == null) {
            SysUser user = userMapper.selectById(userId);
            permissionVersion = user.getPermissionVersion();
            redisUtil.set(versionKey, permissionVersion, REFRESH_TOKEN_EXPIRE_SEC);
        }

        // 生成最新 token
        boolean isSuperAdmin = userMapper.isSuperAdmin(userId);
        AuthUser authUser = new AuthUser();
        authUser.setUserId(userId);
        authUser.setSuperAdmin(isSuperAdmin);
        authUser.setPermissionVersion(permissionVersion);

        return jwtUtil.generateToken(authUser, ACCESS_TOKEN_EXPIRE_MS);
    }

    // ====================== 登出：删除该用户所有 refreshToken ======================
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) return;

        String refreshKey = "refresh:" + refreshToken;
        Long userId = redisUtil.getLong(refreshKey);

        // 删除当前 token
        redisUtil.del(refreshKey);

        // 可选：删除该用户所有 token（多设备下线）
        if (userId != null) {
            redisUtil.del("auth:ver:" + userId);
            redisUtil.del("auth:perm:" + userId);
        }
    }

    // 类型转换工具
    private Long toLong(Object obj) {
        if (obj instanceof Number n) return n.longValue();
        return Long.parseLong(obj.toString());
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


}