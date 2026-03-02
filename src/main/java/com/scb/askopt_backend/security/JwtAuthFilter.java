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
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

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
            "/v3/api-docs",
            "/api/agent/"
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

        // 1️⃣ 白名单 + OPTIONS 放行
        if (isWhitelisted(path) || "OPTIONS".equalsIgnoreCase(method)) {
            chain.doFilter(request, response);
            return;
        }

        try {

            // 2️⃣ 获取 Token
            String header = req.getHeader("Authorization");
            if (header == null || !header.startsWith("Bearer ")) {
                unauthorized(resp, "missing token");
                return;
            }

            String token = header.substring(7);

            // 3️⃣ 解析 JWT（捕获所有异常）
            Claims claims;
            try {
                claims = jwtUtil.parse(token);
            } catch (JwtException | IllegalArgumentException e) {
                log.warn("JWT parse failed: {}", e.getMessage());
                unauthorized(resp, "invalid token");
                return;
            }

            Long userId = getLongClaim(claims, "uid");
            Long tokenVersion = getLongClaim(claims, "ver");

            if (userId == null || tokenVersion == null) {
                unauthorized(resp, "invalid token payload");
                return;
            }

            // 4️⃣ 校验版本号
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

            // 5️⃣ 读取权限集合
            Object permObj = redisUtil.get("auth:perm:" + userId);
            if (permObj == null) {
                forbidden(resp, "no permissions");
                return;
            }

            Set<Long> permissionIds = safeConvertToLongSet(permObj);

            // 6️⃣ 匹配当前接口所需权限
            String requiredPermIdStr =
                    permissionMatcher.match(path, method);

            if (requiredPermIdStr != null) {
                Long requiredPermId;
                try {
                    requiredPermId = Long.parseLong(requiredPermIdStr);
                } catch (NumberFormatException e) {
                    log.error("Permission format error: {}", requiredPermIdStr);
                    forbidden(resp, "permission format error");
                    return;
                }

                if (!permissionIds.contains(requiredPermId)) {
                    forbidden(resp,
                            "access denied: lack permission " + requiredPermId);
                    return;
                }
            }

            // 7️⃣ 写入上下文
            AuthContext.setUserId(userId);

            try {
                chain.doFilter(request, response);
            } finally {
                AuthContext.clear();
            }

        } catch (Exception e) {
            log.error("Unexpected auth filter error", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Internal Server Error");
        }
    }

    /* ======================== 工具方法 ======================== */

    private boolean isWhitelisted(String path) {
        for (String prefix : WHITELIST) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private Long getLongClaim(Claims claims, String key) {
        Object val = claims.get(key);
        return val == null ? null : toLong(val);
    }

    private Long toLong(Object obj) {
        if (obj instanceof Number n) {
            return n.longValue();
        }
        return Long.valueOf(obj.toString());
    }

    private Set<Long> safeConvertToLongSet(Object obj) {
        Set<Long> result = new HashSet<>();
        if (obj instanceof Collection<?> collection) {
            for (Object item : collection) {
                if (item instanceof Number n) {
                    result.add(n.longValue());
                }
            }
        }
        return result;
    }

    private void unauthorized(HttpServletResponse resp, String msg)
            throws IOException {
        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        writeJson(resp, ApiResponse.error(401, msg));
    }

    private void forbidden(HttpServletResponse resp, String msg)
            throws IOException {
        resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        writeJson(resp, ApiResponse.error(403, msg));
    }

    private void writeJson(HttpServletResponse response,
                           ApiResponse<?> apiResponse)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter()
                .write(objectMapper.writeValueAsString(apiResponse));
    }
}