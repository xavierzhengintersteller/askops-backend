package com.scb.tb.askopt_backend.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@Order(1)
public class TraceFilter implements Filter {
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String MDC_TRACE_KEY = "traceId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // 优先透传上游TraceId，无则新建
        String traceId = req.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        MDC.put(MDC_TRACE_KEY, traceId);
        req.setAttribute(MDC_TRACE_KEY, traceId);
        resp.setHeader(TRACE_ID_HEADER, traceId);

        // 统一处理OPTIONS跨域预检
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            resp.setHeader("Access-Control-Allow-Origin", "*");
            resp.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
            resp.setHeader("Access-Control-Allow-Headers", "Authorization,X-Client-Id,X-Signature,X-Timestamp,X-Trace-Id");
            resp.setStatus(HttpServletResponse.SC_OK);
            MDC.remove(MDC_TRACE_KEY);
            return;
        }

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_TRACE_KEY);
        }
    }
}