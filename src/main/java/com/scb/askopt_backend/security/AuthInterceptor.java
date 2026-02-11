package com.scb.askopt_backend.security;

import com.scb.askopt_backend.service.PermissionService;
import com.scb.askopt_backend.vo.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final PermissionService permissionService;

    public AuthInterceptor(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String path = request.getServletPath();
        String method = request.getMethod();

        // 只拦 /containers/**
        if (!path.startsWith("/api/containers/")) {
            return true;
        }

        String requiredPermission =
                permissionService.match(path, method);

        if (requiredPermission == null) {
            deny(response, 403, "permission not configured");
            return false;
        }

        AuthUser user = AuthContext.get();
        if (user == null || !user.getPermissions().contains(requiredPermission)) {
            deny(response, 403, "permission denied");
            return false;
        }

        return true;
    }

    // 原生 JSON 返回
    private void deny(HttpServletResponse response, int status, String msg) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");

        // 手动构造 JSON 字符串
        String json = "{\"code\":" + status + ",\"message\":\"" + msg + "\"}";
        response.getWriter().write(json);
    }
}
