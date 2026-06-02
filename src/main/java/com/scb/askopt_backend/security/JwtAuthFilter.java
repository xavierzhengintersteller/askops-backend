package com.scb.askopt_backend.security;

import com.scb.askopt_backend.config.RedisUtil;
import com.scb.askopt_backend.constant.RedisConstants;
import com.scb.askopt_backend.context.AuthContext;
import com.scb.askopt_backend.vo.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

import static com.scb.askopt_backend.constant.RedisConstants.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements Filter {

    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;
    private final PermissionMatcher permissionMatcher;
    private final ObjectMapper objectMapper;

    private static final String[] WHITELIST = {
            "/api/auth/login",
            "/api/auth/logout",
            "/api/auth/token/refresh",
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

        // ==============================================
        // 【全局 TraceID 生成】所有请求都生成，包括白名单
        // ==============================================
        String traceId = UUID.randomUUID().toString().replace("-", "");
        req.setAttribute("traceId", traceId); // 放入 request，给 AOP 使用
        MDC.put("traceId", traceId);           // 放入日志，方便排查
        resp.setHeader("X-Trace-Id",traceId);   //  响应带回前端
        try {
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
            boolean superAdmin = Boolean.TRUE.equals(claims.getOrDefault("superAdmin", false));
            // ========== 黑名单校验 ==========
            String blackKey = RedisConstants.REDIS_BLACKLIST_USER + userId;
            if (redisUtil.hasKey(blackKey)) {
                unauthorized(resp, "user disabled or forced offline");
                return;
            }
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
            Object redisVerObj = redisUtil.get(REDIS_PERMISSION_VERSION + userId);
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
            Object permObj = redisUtil.get(REDIS_PERMISSION_LIST + userId);
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
        } finally {
            // 最后清空 MDC
            MDC.clear();
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