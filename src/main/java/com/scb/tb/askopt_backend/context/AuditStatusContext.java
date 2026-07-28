package com.scb.tb.askopt_backend.context;

import lombok.experimental.UtilityClass;

/**
 * 审计状态上下文（用于批量接口/特殊接口设置审计结果）
 */
@UtilityClass
public class AuditStatusContext {

    private final ThreadLocal<String> STATUS = new ThreadLocal<>();

    // ====================== 审计状态
    public void setStatus(String status) {
        STATUS.set(status);
    }

    public String getStatus() {
        return STATUS.get();
    }

    // ====================== 清空
    public void clear() {
        STATUS.remove();
    }
}