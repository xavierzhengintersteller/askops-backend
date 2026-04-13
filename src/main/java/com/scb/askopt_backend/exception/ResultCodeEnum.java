package com.scb.askopt_backend.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public enum ResultCodeEnum {

    // 成功
    SUCCESS(0, "success"),

    // 业务异常
    NO_AGENT(100001, "当前用户未分配任何代理节点，请联系管理员"),

    // 系统异常
    UNAUTHORIZED(401, "用户未登录"),
    ERROR(500, "系统异常");

    private final int code;
    private final String message;
}