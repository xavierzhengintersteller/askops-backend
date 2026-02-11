package com.scb.askopt_backend.service;

import com.scb.askopt_backend.entity.SysPermission;
import com.scb.askopt_backend.mapper.PermissionMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.ArrayList;
import java.util.List;

@Service
public class PermissionService {

    private static final Logger log = LoggerFactory.getLogger(PermissionService.class);

    @Autowired
    private PermissionMapper permissionMapper;

    private final List<SysPermission> permissions = new ArrayList<>();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @PostConstruct
    public void loadPermissions() {
        permissions.clear();
        List<SysPermission> list = permissionMapper.findAllPermissions();
        permissions.addAll(list);

        // 输出加载的权限列表
        log.info("Loaded {} permissions:", permissions.size());
        for (SysPermission p : permissions) {
            log.info("  [{}] {} -> {}", p.getHttpMethod(), p.getUrlPattern(), p.getPermissionCode());
        }
    }

    public String match(String path, String method) {
        // 先按 url_pattern 长度降序排序，精确匹配优先
        permissions.sort((a, b) -> b.getUrlPattern().length() - a.getUrlPattern().length());

        for (SysPermission p : permissions) {
            String httpMethod = p.getHttpMethod();
            if (httpMethod != null && !"*".equals(httpMethod) && !httpMethod.equalsIgnoreCase(method)) {
                continue;
            }
            if (pathMatcher.match(p.getUrlPattern(), path)) {
                return p.getPermissionCode();
            }
        }
        return null;
    }

}
