package com.scb.tb.askopt_backend.security;

import com.scb.tb.askopt_backend.config.RedisUtil;
import com.scb.tb.askopt_backend.constant.RedisConstants;
import com.scb.tb.askopt_backend.context.AuthContext;
import com.scb.tb.askopt_backend.vo.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

import static com.scb.tb.askopt_backend.constant.RedisConstants.*;

@Slf4j
@Component
@Order(3)
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
            "/api/test/test",
            "/swagger-ui/",
            "/api/agent/",
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
        String traceId = (String) req.getAttribute(TraceFilter.MDC_TRACE_KEY);

        // 1. 白名单接口直接放行
        if (isWhitelisted(path)) {
            chain.doFilter(request, response);
            return;
        }

        // 2. 获取 Token
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            log.warn("[traceId={}] 接口缺少Token path={}", traceId, path);
            unauthorized(resp, "missing token");
            return;
        }
        String token = header.substring(7);

        // 3. 解析 JWT
        Claims claims;
        try {
            claims = jwtUtil.parse(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("[traceId={}] JWT解析失败 path={},err={}", traceId, path, e.getMessage());
            unauthorized(resp, "invalid token");
            return;
        }

        // 4. 从 Token 读取用户信息
        Long userId = getLongClaim(claims, "uid");
        Long tokenVersion = getLongClaim(claims, "ver");
        boolean superAdmin = Boolean.TRUE.equals(claims.getOrDefault("superAdmin", false));
        // 黑名单校验
        String blackKey = RedisConstants.REDIS_BLACKLIST_USER + userId;
        if (redisUtil.hasKey(blackKey)) {
            log.warn("[traceId={}] 用户已下线 userId={}", traceId, userId);
            unauthorized(resp, "user disabled or forced offline");
            return;
        }
        if (userId == null || tokenVersion == null) {
            unauthorized(resp, "invalid token payload");
            return;
        }

        // 超级管理员 → 直接放行
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

        // 普通用户：校验权限版本
        Object redisVerObj = redisUtil.get(REDIS_PERMISSION_VERSION + userId);
        if (redisVerObj == null) {
            unauthorized(resp, "session expired");
            return;
        }

        Long currentVersion = toLong(redisVerObj);
        if (!tokenVersion.equals(currentVersion)) {
            log.warn("[traceId={}] 用户权限变更 userId={}", traceId, userId);
            unauthorized(resp, "permission changed");
            return;
        }

        // 权限校验
        Object permObj = redisUtil.get(REDIS_PERMISSION_LIST + userId);
        if (permObj == null) {
            forbidden(resp, "no permissions");
            return;
        }

        Set<Long> permissionIds = safeConvertToLongSet(permObj);
        String requiredPermIdStr = permissionMatcher.match(path, req.getMethod());

        if (requiredPermIdStr == null) {
            log.warn("[traceId={}] 未配置权限接口 {} {}", traceId, req.getMethod(), path);
            forbidden(resp, "no permission config");
            return;
        }

        try {
            Long required = Long.parseLong(requiredPermIdStr);
            if (!permissionIds.contains(required)) {
                log.warn("[traceId={}] 用户无接口权限 userId={},perm={}", traceId, userId, required);
                forbidden(resp, "no permission");
                return;
            }
        } catch (NumberFormatException e) {
            forbidden(resp, "permission config error");
            return;
        }

        // 存入上下文放行
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
        resp.setHeader(TraceFilter.TRACE_ID_HEADER, MDC.get(TraceFilter.MDC_TRACE_KEY));
        resp.getWriter().write(objectMapper.writeValueAsString(res));
    }
}