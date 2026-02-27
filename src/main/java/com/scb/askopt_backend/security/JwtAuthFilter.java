package com.scb.askopt_backend.security;

import com.scb.askopt_backend.vo.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements Filter {

    private final JwtUtil jwtUtil;
    private final PermissionMatcher permissionMatcher;
    private final ObjectMapper objectMapper;

    private static final String[] WHITELIST = {
            "/api/auth/",
            "/api/agent/",
            "/swagger-ui/index.html",
            "/swagger-ui/",
            "/v3/api-docs"
    };

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String path = req.getServletPath();
        String method = req.getMethod();

        // 白名单放行
        if (Arrays.stream(WHITELIST).anyMatch(path::startsWith)
                || "OPTIONS".equalsIgnoreCase(method)) {
            chain.doFilter(request, response);
            return;
        }

        // 1️⃣ 校验 JWT Token
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            writeJson(resp, ApiResponse.error(401, "invalid token"));
            return;
        }

        String token = header.substring(7);
        Claims claims;
        try {
            claims = jwtUtil.parse(token);
        } catch (Exception e) {
            log.error("JWT parse error", e);
            writeJson(resp, ApiResponse.error(401, "token invalid"));
            return;
        }

        // 2️⃣ 解析 JWT 中的权限ID（统一转为 Long 类型，和 AuthUser 对齐）
        String username = claims.getSubject();
        // 改用 Set<Long> 存储，和 AuthUser 结构一致（也可用 List<Long>，contains 逻辑相同）
        Set<Long> permissionIds = parsePermissionIdsFromClaims(claims);

        // 防护：无权限直接拒绝
        if (permissionIds.isEmpty()) {
            writeJson(resp, ApiResponse.error(403, "no permissions"));
            return;
        }

        // 3️⃣ 匹配当前接口所需的权限ID
        String requiredPermIdStr = permissionMatcher.match(path, method);

        // 4️⃣ 权限校验逻辑（核心：统一用 Long 类型）
        if (requiredPermIdStr != null) { // 接口需要权限
            try {
                // 关键：转成 Long 类型，和 AuthUser/permissionIds 完全对齐
                Long requiredPermId = Long.parseLong(requiredPermIdStr);
                // 类型一致，能正确匹配
                if (!permissionIds.contains(requiredPermId)) {
                    writeJson(resp, ApiResponse.error(403,
                            "access denied: lack permission " + requiredPermId));
                    return;
                }
            } catch (NumberFormatException e) {
                log.error("Permission ID format error: {}", requiredPermIdStr, e);
                writeJson(resp, ApiResponse.error(403, "permission format error"));
                return;
            }
        }

        // 5️⃣ 放行，设置用户上下文（可选：可将 AuthUser 存入上下文）
        AuthContext.set(username);
        // 进阶：如果需要将完整 AuthUser 存入上下文，可从 claims 解析更多字段
        // AuthUser authUser = buildAuthUserFromClaims(claims);
        // AuthContext.setAuthUser(authUser);

        try {
            chain.doFilter(request, response);
        } finally {
            AuthContext.clear();
        }
    }

    /**
     * 从 JWT Claims 解析权限ID，统一转为 Set<Long>（消除泛型警告 + 类型安全）
     */
    private Set<Long> parsePermissionIdsFromClaims(Claims claims) {

        Object raw = claims.get("permissions");

        if (!(raw instanceof List<?> rawList)) {
            return Collections.emptySet();
        }

        return rawList.stream()
                .filter(Objects::nonNull)
                .map(obj -> {
                    if (obj instanceof Number n) {
                        return n.longValue();
                    }
                    throw new IllegalArgumentException("Invalid permission type: " + obj);
                })
                .collect(Collectors.toSet());
    }
//    private Set<Long> parsePermissionIdsFromClaims(Claims claims) {
//        List<Long> permissionIdList = new ArrayList<>();
//        // 获取原始 List，避免泛型警告
//        List<?> rawPerms = claims.get("permissionIds", List.class);
//        if (rawPerms != null) {
//            for (Object obj : rawPerms) {
//                if (obj instanceof Number) {
//                    // 统一转为 Long（兼容 Integer/Long/Short 等数字类型）
//                    permissionIdList.add(((Number) obj).longValue());
//                } else {
//                    log.warn("Invalid permission ID type in JWT: {} (value: {})",
//                            obj != null ? obj.getClass().getName() : "null", obj);
//                }
//            }
//        }
//        // 转为 Set，和 AuthUser 结构一致（查询效率更高）
//        return new HashSet<>(permissionIdList);
//    }

    private void writeJson(HttpServletResponse response,
                           ApiResponse<?> apiResponse)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}