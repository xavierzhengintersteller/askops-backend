package com.scb.askopt_backend.security;

import com.scb.askopt_backend.entity.SysPermission;
import com.scb.askopt_backend.mapper.PermissionMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class PermissionMatcher {

    @Autowired
    private PermissionMapper permissionMapper;

    private final List<SysPermission> permissions = new ArrayList<>();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @PostConstruct
    public void loadPermissions() {
        permissions.clear();
        List<SysPermission> list = permissionMapper.findAllPermissions();

        // 长度降序排序，精确匹配优先
        list.sort((a, b) -> b.getUrlPattern().length() - a.getUrlPattern().length());

        permissions.addAll(list);

        // 日志调整：打印 permissionId 而非 permissionCode
        log.info("Loaded {} permissions:", permissions.size());
        for (SysPermission p : permissions) {
            log.info("  [{}] {} -> permissionId: {}", p.getHttpMethod(), p.getUrlPattern(), p.getId());
        }
    }

    /**
     * 匹配 URL + 方法返回 permissionId（转为字符串，方便后续解析）
     * 原逻辑：返回 permissionCode（字符串）
     * 新逻辑：返回 permissionId（数字转字符串）
     */
    public String match(String path, String method) {
        for (SysPermission p : permissions) {
            String httpMethod = p.getHttpMethod();
            // 跳过方法不匹配的权限（* 代表匹配所有方法）
            if (httpMethod != null && !"*".equals(httpMethod) && !httpMethod.equalsIgnoreCase(method)) {
                continue;
            }
            // URL 模式匹配
            if (pathMatcher.match(p.getUrlPattern(), path)) {
                // 核心改动：返回 permissionId（转为字符串），而非 permissionCode
                return String.valueOf(p.getId()); // 假设 SysPermission 中权限ID字段是 id
                // 如果你的权限ID字段名是 permissionId，改为：return String.valueOf(p.getPermissionId());
            }
        }
        return null;
    }
}