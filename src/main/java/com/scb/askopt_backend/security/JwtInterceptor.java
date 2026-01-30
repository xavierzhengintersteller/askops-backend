package com.scb.askopt_backend.security;


import com.scb.askopt_backend.vo.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import tools.jackson.databind.ObjectMapper;

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

        return true;
    }
}
