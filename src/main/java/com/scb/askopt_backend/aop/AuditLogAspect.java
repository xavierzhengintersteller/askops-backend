package com.scb.askopt_backend.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scb.askopt_backend.annotation.AuditLog;
import com.scb.askopt_backend.context.AuditStatusContext;
import com.scb.askopt_backend.context.AuthContext;
import com.scb.askopt_backend.entity.SysAuditLog;
import com.scb.askopt_backend.service.SysAuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final SysAuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @SneakyThrows
    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint point, AuditLog auditLog) {
        long start = System.currentTimeMillis();
        SysAuditLog logEntity = new SysAuditLog();
        Object result = null;

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        HttpServletResponse response = attributes.getResponse();
        String traceId = (String) request.getAttribute("traceId");

        try {
            fillBaseInfo(point, auditLog, logEntity, request);
            result = point.proceed();

            String status = AuditStatusContext.getStatus();
            logEntity.setStatus(status == null ? "SUCCESS" : status);
            logEntity.setHttpCode(response.getStatus());
            logEntity.setResponseResult(objectMapper.writeValueAsString(result));
        } catch (Throwable e) {
            logEntity.setStatus("FAIL");
            logEntity.setHttpCode(response.getStatus());
            logEntity.setErrorMsg(e.getMessage());
            throw e;
        } finally {
            logEntity.setTraceId(traceId);
            logEntity.setCostTime(System.currentTimeMillis() - start);
            logEntity.setCreateTime(LocalDateTime.now());
            auditLogService.asyncSave(logEntity);
            AuditStatusContext.clear();
        }
        return result;
    }

    private void fillBaseInfo(ProceedingJoinPoint point, AuditLog auditLog, SysAuditLog log, HttpServletRequest request) {
        Long userId = AuthContext.getUserId();
        Boolean superAdmin = AuthContext.isSuperAdmin();
        Method method = ((MethodSignature) point.getSignature()).getMethod();

        log.setUserId(userId);
        log.setSuperAdmin(superAdmin);
        log.setModule(auditLog.module());
        log.setOperation(auditLog.operation());
        log.setRequestMethod(request.getMethod());
        log.setRequestPath(request.getRequestURI());
        log.setRequestIp(getClientIp(request));
        log.setUserAgent(request.getHeader("User-Agent"));

        try {
            // ==== 关键：序列化前脱敏密码 ====
            Object[] args = point.getArgs();
            if (args != null && args.length > 0) {
                String json = objectMapper.writeValueAsString(args[0]);
                // 自动替换 password → ******
                json = json.replaceAll("\"password\":\"[^\"]*\"", "\"password\":\"******\"");
                log.setRequestParams(json);
            }
        } catch (Exception e) {
            log.setRequestParams("params error");
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}