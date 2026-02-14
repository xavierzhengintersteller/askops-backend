package com.scb.askopt_backend.security;

import com.scb.askopt_backend.dto.PermissionRule;
import com.scb.askopt_backend.mapper.PermissionMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
public class PermissionMatcher {

    private final PermissionMapper permissionMapper;

    private final AntPathMatcher matcher = new AntPathMatcher();

    // 使用 AtomicReference 支持无锁热更新
    private final AtomicReference<List<PermissionRule>> ruleRef =
            new AtomicReference<>();

    @PostConstruct
    public void init() {
        refreshRules();
    }

    /**
     * 启动或刷新权限规则
     */
    public void refreshRules() {
        List<PermissionRule> rules = permissionMapper.selectAll();

        // 最长路径优先
        rules.sort(Comparator.comparingInt(
                (PermissionRule r) -> r.getPattern().length()
        ).reversed());

        ruleRef.set(rules);
    }

    /**
     * 根据 path + method 匹配所需权限
     */
    public String match(String path, String method) {

        for (PermissionRule rule : ruleRef.get()) {

            if (matcher.match(rule.getPattern(), path)
                    && rule.getMethod().equalsIgnoreCase(method)) {

                return rule.getPermission();
            }
        }

        return null;
    }
}
