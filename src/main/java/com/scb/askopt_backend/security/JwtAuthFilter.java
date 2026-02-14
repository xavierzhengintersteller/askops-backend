package com.scb.askopt_backend.security;

import com.scb.askopt_backend.config.RedisUtil;
import com.scb.askopt_backend.mapper.PermissionMapper;
import com.scb.askopt_backend.vo.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements Filter {

    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;
    private final PermissionMapper permissionMapper;

    private static final String[] SWAGGER_WHITELIST = {
            "/swagger-ui.html",
            "/swagger-ui/",
            "/v3/api-docs",
            "/swagger-resources",
            "/webjars/",
            "/doc.html"
    };

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String path = req.getServletPath();

        // 放行登录接口、Swagger、OPTIONS
        if (path.startsWith("/api/auth/") ||
                Arrays.stream(SWAGGER_WHITELIST).anyMatch(path::startsWith) ||
                "OPTIONS".equalsIgnoreCase(req.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            writeJson(resp, ApiResponse.error(401, "invalid token"));
            return;
        }

        String token = header.substring(7);
        String username = jwtUtil.getUsername(token);

        AuthUser authUser = (AuthUser) redisUtil.get("auth:login:" + username);

        if (authUser == null || !token.equals(authUser.getToken())) {
            writeJson(resp, ApiResponse.error(401, "token expired or invalid"));
            return;
        }

        // 权限校验
        String permissionCode = permissionMapper.findPermissionCodeByUrlAndMethod(path, req.getMethod());
        if (permissionCode != null && !authUser.getPermissions().contains(permissionCode)) {
            writeJson(resp, ApiResponse.error(403, "access denied"));
            return;
        }

        // ✅ 设置线程上下文
        try {
            AuthContext.set(authUser);
            chain.doFilter(request, response);
        } finally {
            // ✅ 请求结束，清理线程上下文
            AuthContext.clear();
        }
    }

    private void writeJson(HttpServletResponse response, ApiResponse<?> apiResponse) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
