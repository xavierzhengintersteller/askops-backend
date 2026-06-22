package com.scb.askopt_backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scb.askopt_backend.config.Hmac.HmacProperties;
import com.scb.askopt_backend.config.Hmac.HmacSigner;
import com.scb.askopt_backend.security.BodyRepeatHttpServletRequest;
import com.scb.askopt_backend.vo.ApiResponse;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class GoAgentAuthFilter implements Filter {
    private static final String AGENT_API_PREFIX = "/api/agent/";
    private static final String HEADER_CLIENT_ID = "X-Client-Id";
    private static final String HEADER_SIGN = "X-Signature";
    private static final String HEADER_TIMESTAMP = "X-Timestamp";
    private static final long SIGN_EXPIRE_WINDOW = 60 * 1000;

    private final HmacProperties hmacProperties;
    private final ObjectMapper objectMapper;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String path = req.getServletPath();
        String traceId = (String) req.getAttribute(com.scb.askopt_backend.security.TraceFilter.MDC_TRACE_KEY);

        // 非Agent接口直接放行，交给JwtAuthFilter
        if (!path.startsWith(AGENT_API_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        // 1. 校验鉴权头部完整性
        String clientId = req.getHeader(HEADER_CLIENT_ID);
        String sign = req.getHeader(HEADER_SIGN);
        String tsStr = req.getHeader(HEADER_TIMESTAMP);
        if (clientId == null || sign == null || tsStr == null) {
            log.warn("[traceId={}] Agent请求缺少鉴权头,path={}", traceId, path);
            writeErr(resp, 401, "missing agent auth header");
            return;
        }

        // 2. 时间戳防重放校验
        long ts;
        try {
            ts = Long.parseLong(tsStr);
        } catch (NumberFormatException e) {
            log.warn("[traceId={}] Agent时间戳格式错误,clientId={}", traceId, clientId);
            writeErr(resp, 401, "invalid timestamp");
            return;
        }
        long now = System.currentTimeMillis();
        if (Math.abs(now - ts) > SIGN_EXPIRE_WINDOW) {
            log.warn("[traceId={}] Agent签名超时,clientId={},ts={}", traceId, clientId, tsStr);
            writeErr(resp, 401, "signature expired");
            return;
        }

        // 3. 校验clientId配置存在
        Map<String, String> clientSecretMap = hmacProperties.getClients();
        String secret = clientSecretMap.get(clientId);
        if (secret == null) {
            log.warn("[traceId={}] 未知Agent clientId={}", traceId, clientId);
            writeErr(resp, 401, "unknown agent clientId");
            return;
        }

        // 4. 读取请求体，兼容空body
        String method = req.getMethod();
        String body = StreamUtils.copyToString(req.getInputStream(), StandardCharsets.UTF_8);
        HttpServletRequest wrapReq = new BodyRepeatHttpServletRequest(req, body);
        String payload = String.format("%s%s%s%s", method, path, tsStr, body);
        String calcSign = HmacSigner.sign(payload, secret);

        if (!calcSign.equals(sign)) {
            log.warn("[traceId={}] Agent签名不匹配 clientId={},path={}", traceId, clientId, path);
            writeErr(resp, 401, "signature verify fail");
            return;
        }

        // 上下文存入当前Agent身份
        wrapReq.setAttribute("agentClientId", clientId);
        chain.doFilter(wrapReq, resp);
    }

    private void writeErr(HttpServletResponse resp, int code, String msg) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.setStatus(code);
        resp.setHeader(com.scb.askopt_backend.security.TraceFilter.TRACE_ID_HEADER,
                MDC.get(com.scb.askopt_backend.security.TraceFilter.MDC_TRACE_KEY));
        resp.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(code, msg)));
    }
}