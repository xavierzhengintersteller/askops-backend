package com.scb.tb.askopt_backend.security;

import com.scb.tb.askopt_backend.entity.SysPermission;
import com.scb.tb.askopt_backend.mapper.PermissionMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionMatcher {

    private final PermissionMapper permissionMapper;

    private volatile List<SysPermission> permissions = List.of();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @PostConstruct
    public void loadPermissions() {
        // 1. 查全部
        List<SysPermission> list = permissionMapper.selectList(null);

        // ==============================================
        // 🔥 只保留 type = api 的权限（核心改动）
        // ==============================================
        list = list.stream()
                .filter(p -> "api".equals(p.getType()))
                .collect(Collectors.toList());

        // 排序：长路径优先
        list.sort((a, b) -> {
            String urlA = a.getUrlPattern() == null ? "" : a.getUrlPattern();
            String urlB = b.getUrlPattern() == null ? "" : b.getUrlPattern();
            return Integer.compare(urlB.length(), urlA.length());
        });

        permissions = List.copyOf(list);

        log.info("✅ 开机加载 API 权限完成，共 {} 条", permissions.size());
        for (SysPermission p : permissions) {
            log.info("  [{}] {} -> permissionId: {}",
                    p.getHttpMethod(),
                    p.getUrlPattern(),
                    p.getId());
        }
    }

    public String match(String path, String method) {
        for (SysPermission p : permissions) {
            String httpMethod = p.getHttpMethod();

            if (httpMethod != null
                    && !"*".equals(httpMethod)
                    && !httpMethod.equalsIgnoreCase(method)) {
                continue;
            }

            String pattern = p.getUrlPattern();
            if (pattern == null) continue;

            if (pathMatcher.match(pattern, path)) {
                return String.valueOf(p.getId());
            }
        }
        return null;
    }
}