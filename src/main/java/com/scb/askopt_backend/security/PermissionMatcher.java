package com.scb.askopt_backend.security;

import com.scb.askopt_backend.entity.SysPermission;
import com.scb.askopt_backend.mapper.PermissionMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.logging.Logger;
import org.mybatis.logging.LoggerFactory;
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

        log.info("Loaded {} permissions:", permissions.size());
        for (SysPermission p : permissions) {
            log.info("  [{}] {} -> {}", p.getHttpMethod(), p.getUrlPattern(), p.getPermissionCode());
        }
    }

    /**
     * 匹配 URL + 方法返回 permissionCode
     */
    public String match(String path, String method) {
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
