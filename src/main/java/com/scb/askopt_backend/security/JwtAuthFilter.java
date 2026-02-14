package com.scb.askopt_backend.security;

import com.scb.askopt_backend.vo.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements Filter {

    private final JwtUtil jwtUtil;
    private final PermissionMatcher permissionMatcher;
    private final ObjectMapper objectMapper;

    private static final String[] WHITELIST = {
            "/api/auth/",
            "/swagger-ui",
            "/v3/api-docs"
    };

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String path = req.getServletPath();
        String method = req.getMethod();

        // 白名单
        if (Arrays.stream(WHITELIST).anyMatch(path::startsWith)
                || "OPTIONS".equalsIgnoreCase(method)) {
            chain.doFilter(request, response);
            return;
        }

        // 1️⃣ 校验 JWT
        String header = req.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            writeJson(resp, ApiResponse.error(401, "invalid token"));
            return;
        }

        String token = header.substring(7);

        Claims claims;
        try {
            claims = jwtUtil.parse(token);
        } catch (Exception e) {
            writeJson(resp, ApiResponse.error(401, "token invalid"));
            return;
        }

        String username = claims.getSubject();
        List<String> permissions =
                claims.get("permissions", List.class);

        // 2️⃣ 匹配所需权限
        String requiredPermission =
                permissionMatcher.match(path, method);

        // 如果接口有权限要求
        if (requiredPermission != null
                && !permissions.contains(requiredPermission)) {

            writeJson(resp,
                    ApiResponse.error(403, "access denied"));
            return;
        }

        // 3️⃣ 放行
        AuthContext.set(username);

        try {
            chain.doFilter(request, response);
        } finally {
            AuthContext.clear();
        }
    }

    private void writeJson(HttpServletResponse response,
                           ApiResponse<?> apiResponse)
            throws IOException {

        response.setContentType("application/json;charset=UTF-8");
        response.getWriter()
                .write(objectMapper.writeValueAsString(apiResponse));
    }
}
