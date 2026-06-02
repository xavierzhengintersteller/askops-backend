package com.scb.askopt_backend.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {
    /** 功能模块 */
    String module();
    /** 操作类型 */
    String operation();
}