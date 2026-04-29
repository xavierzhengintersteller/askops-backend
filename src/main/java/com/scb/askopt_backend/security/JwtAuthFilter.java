package com.scb.askopt_backend.security;

import com.scb.askopt_backend.config.RedisUtil;
import com.scb.askopt_backend.vo.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements Filter {

    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;
    private final PermissionMatcher permissionMatcher;
    private final ObjectMapper objectMapper;

    private static final String[] WHITELIST = {
            "/api/auth/",
            "/swagger-ui/",
            "/v3/api-docs"
    };

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String path = req.getServletPath();
        String method = req.getMethod();

        // 1. 白名单 / OPTIONS 放行
        if (isWhitelisted(path) || "OPTIONS".equalsIgnoreCase(method)) {
            chain.doFilter(request, response);
            return;
        }

        // 2. 获取 Token
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            unauthorized(resp, "missing token");
            return;
        }
        String token = header.substring(7);

        // 3. 解析 JWT
        Claims claims;
        try {
            claims = jwtUtil.parse(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT 解析失败: {}", e.getMessage());
            unauthorized(resp, "invalid token");
            return;
        }

        // 4. 从 Token 读取用户信息
        Long userId = getLongClaim(claims, "uid");
        Long tokenVersion = getLongClaim(claims, "ver");
        boolean superAdmin = Boolean.TRUE.equals(claims.get("superAdmin", Boolean.class));

        if (userId == null || tokenVersion == null) {
            unauthorized(resp, "invalid token payload");
            return;
        }

        // ==============================================
        // 超级管理员 → 直接放行
        // ==============================================
        if (superAdmin) {
            AuthContext.setUserId(userId);
            AuthContext.setPermissionVersion(tokenVersion);
            AuthContext.setSuperAdmin(true);
            try {
                chain.doFilter(request, response);
            } finally {
                AuthContext.clear();
            }
            return;
        }

        // 5. 普通用户：校验权限版本
        Object redisVerObj = redisUtil.get("auth:ver:" + userId);
        if (redisVerObj == null) {
            unauthorized(resp, "session expired");
            return;
        }

        Long currentVersion = toLong(redisVerObj);
        if (!tokenVersion.equals(currentVersion)) {
            unauthorized(resp, "permission changed");
            return;
        }

        // 6. 权限校验
        Object permObj = redisUtil.get("auth:perm:" + userId);
        if (permObj == null) {
            forbidden(resp, "no permissions");
            return;
        }

        Set<Long> permissionIds = safeConvertToLongSet(permObj);
        String requiredPermIdStr = permissionMatcher.match(path, method);

        if (requiredPermIdStr == null) {
            log.warn("⚠️ 未配置权限的接口被访问：{} {}", method, path);
            forbidden(resp, "no permission config");
            return;
        }

        try {
            Long required = Long.parseLong(requiredPermIdStr);
            if (!permissionIds.contains(required)) {
                forbidden(resp, "no permission");
                return;
            }
        } catch (NumberFormatException e) {
            forbidden(resp, "permission config error");
            return;
        }

        // 7. 放行前存入 ThreadLocal
        AuthContext.setUserId(userId);
        AuthContext.setPermissionVersion(tokenVersion);
        AuthContext.setSuperAdmin(false);
        try {
            chain.doFilter(request, response);
        } finally {
            AuthContext.clear();
        }
    }

    // ===================== 工具方法 =====================
    private boolean isWhitelisted(String path) {
        for (String p : WHITELIST) {
            if (path.startsWith(p)) return true;
        }
        return false;
    }

    private Long getLongClaim(Claims claims, String key) {
        Object val = claims.get(key);
        return val == null ? null : toLong(val);
    }

    private Long toLong(Object obj) {
        if (obj instanceof Number n) return n.longValue();
        return Long.parseLong(obj.toString());
    }

    private Set<Long> safeConvertToLongSet(Object obj) {
        Set<Long> set = new HashSet<>();
        if (obj instanceof Collection<?> c) {
            for (Object o : c) {
                if (o instanceof Number n) set.add(n.longValue());
            }
        }
        return set;
    }

    private void unauthorized(HttpServletResponse resp, String msg) throws IOException {
        resp.setStatus(401);
        writeJson(resp, ApiResponse.error(401, msg));
    }

    private void forbidden(HttpServletResponse resp, String msg) throws IOException {
        resp.setStatus(403);
        writeJson(resp, ApiResponse.error(403, msg));
    }

    private void writeJson(HttpServletResponse resp, ApiResponse<?> res) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(objectMapper.writeValueAsString(res));
    }
}