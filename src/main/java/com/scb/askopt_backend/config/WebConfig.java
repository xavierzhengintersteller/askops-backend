package com.scb.askopt_backend.config;

import com.scb.askopt_backend.security.AuthInterceptor;
import com.scb.askopt_backend.security.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;
    @Autowired
    private AuthInterceptor authInterceptor;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .order(1)
                .addPathPatterns("/api/**")  // 拦截所有 /api/**
                .excludePathPatterns("/api/auth/**"); // 放行登录登出
        registry.addInterceptor(authInterceptor)
                .order(2)
                .addPathPatterns("/api/**")  // 拦截所有 /api/**
                .excludePathPatterns("/api/auth/**"); // 放行登录登出
    }
}
