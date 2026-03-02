package com.scb.askopt_backend.security;

import com.scb.askopt_backend.entity.SysPermission;
import com.scb.askopt_backend.mapper.PermissionMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionMatcher {

    private final PermissionMapper permissionMapper;

    /**
     * 使用 volatile + 不可变集合
     * 保证线程安全读取
     */
    private volatile List<SysPermission> permissions = List.of();

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @PostConstruct
    public void loadPermissions() {

        List<SysPermission> list = permissionMapper.findAllPermissions();

        // 长度降序排序（精确优先）
        list.sort((a, b) -> b.getUrlPattern().length() - a.getUrlPattern().length());

        // ✅ 关键：整体替换引用，而不是修改原集合
        permissions = List.copyOf(list);

        log.info("Loaded {} permissions:", permissions.size());
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

            if (pathMatcher.match(p.getUrlPattern(), path)) {
                return String.valueOf(p.getId());
            }
        }

        return null;
    }
}