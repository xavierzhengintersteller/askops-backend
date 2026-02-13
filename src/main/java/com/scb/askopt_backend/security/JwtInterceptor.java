package com.scb.askopt_backend.security;


import com.scb.askopt_backend.entity.SysUser;
import com.scb.askopt_backend.mapper.PermissionMapper;
import com.scb.askopt_backend.mapper.UserMapper;
import com.scb.askopt_backend.vo.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Set;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;
    private static final String[] SWAGGER_WHITELIST = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/swagger-resources",
            "/swagger-resources/**",
            "/webjars/**",
            "/doc.html" // Knife4j
    };
    @Autowired
    private PermissionMapper permissionMapper;
    @Autowired
    private UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String path = request.getServletPath();
        // 放行登录
        if(path.startsWith("/api/auth/")) return true;
        // 2️⃣ 放行 Swagger / OpenAPI
        for (String whitePath : SWAGGER_WHITELIST) {
            if (path.startsWith(whitePath.replace("/**", ""))) {
                return true;
            }
        }
        // 3️⃣ 放行 OPTIONS（前端跨域必备）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String header = request.getHeader("Authorization");
        response.setContentType("application/json;charset=UTF-8"); // 设置响应类型

        if(header == null || !header.startsWith("Bearer ")){
            ApiResponse<Void> apiResponse = ApiResponse.error(401, "invalid token");
            String json = objectMapper.writeValueAsString(apiResponse);
            response.getWriter().write(json);
            return false;
        }

        String token = header.substring(7);
        if(!jwtUtil.validateToken(token)){
            ApiResponse<Void> apiResponse = ApiResponse.error(401, "token expired");
            String json = objectMapper.writeValueAsString(apiResponse);
            response.getWriter().write(json);
            return false;
        }

        // ⭐ 关键：解析用户名
        String username = jwtUtil.getUsername(token);

        // ⭐ 加载用户
        SysUser user = userMapper.findByUsername(username);
        if (user == null) {
            ApiResponse<Void> apiResponse = ApiResponse.error(401, "invalid user");
            String json = objectMapper.writeValueAsString(apiResponse);
            response.getWriter().write(json);
            return false;
        }

        // --- RBAC checks start ---
        // 1) resolve permission code for this request URL + method
        String permissionCode = permissionMapper.findPermissionCodeByUrlAndMethod(path, request.getMethod());
        if (permissionCode == null) {
            // no mapping found -> deny (explicit mapping required)
            ApiResponse<Void> apiResponse = ApiResponse.error(403, "no permission mapping for this API");
            String json = objectMapper.writeValueAsString(apiResponse);
            response.getWriter().write(json);
            return false;
        }

        // 2) load user's role codes
        List<String> roleCodes = userMapper.findRoleCodesByUserId(user.getId());
        if (roleCodes == null || roleCodes.isEmpty()) {
            ApiResponse<Void> apiResponse = ApiResponse.error(403, "no roles assigned");
            String json = objectMapper.writeValueAsString(apiResponse);
            response.getWriter().write(json);
            return false;
        }

        // 3) check whether any role grants the required permission
        int cnt = permissionMapper.countRolePermissionByRoleCodesAndPermissionCode(roleCodes, permissionCode);
        if (cnt <= 0) {
            ApiResponse<Void> apiResponse = ApiResponse.error(403, "access denied");
            String json = objectMapper.writeValueAsString(apiResponse);
            response.getWriter().write(json);
            return false;
        }
        // --- RBAC checks end ---

        // 4) OK: set AuthContext with permissions (and continue)
        Set<String> permissions = permissionMapper.findCodesByUserId(user.getId());

        AuthUser authUser = new AuthUser();
        authUser.setUserId(user.getId());
        authUser.setUsername(username);
        authUser.setPermissions(permissions);

        AuthContext.set(authUser);

        return true;
    }
}
