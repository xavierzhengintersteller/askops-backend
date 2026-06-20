package com.scb.askopt_backend.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResultCodeEnum {
    SUCCESS(0, "success"),
    LOGIN_ERROR(401, "用户名或密码错误"),
    USER_DISABLED(100002, "当前用户已被禁用，请联系管理员"),
    NO_AGENT(100001, "当前用户未分配代理节点"),
    SYSTEM_ERROR(500, "系统异常"),
    BAD_REQUEST(400, "参数错误"),
    TOKEN_EMPTY(401, "refreshToken 不能为空"),
    TOKEN_INVALID(401, "refreshToken 无效或已过期"),
    USER_NOT_EXIST(401, "用户不存在"),
    SESSION_EXPIRED(401, "会话已失效"),
    NOT_ALLOW_CHANGE_ADMIN_STATUS(100003, "不允许修改管理员状态"),
    NOT_ALLOW_DELETE_ADMIN(100004, "不允许删除管理员"),
    NOT_ALLOW_CHANGE_ADMIN_PWD(100005, "不允许修改管理员密码"),
    Role_NOTEXIST(100006, "角色不存在"),
    PERMISSION_NOTEXIST(100007, "权限不存在"),
    USER_ALREADY_EXIST(100008, "用户已存在"),
    VALUE_ALREADY_EXIST(100009, "值已存在"),
    // ===================== GoAgent & HMAC 相关 =====================
    GO_AGENT_SERIALIZE_ERROR(100010, "请求参数序列化失败"),
    GO_AGENT_SIGN_ERROR(100011, "接口签名生成失败"),
    GO_AGENT_CONNECT_ERROR(100012, "连接代理节点失败"),
    GO_AGENT_TIMEOUT(100013, "请求代理节点超时"),
    GO_AGENT_RESPONSE_ERROR(100014, "代理节点返回异常数据"),
    GO_AGENT_CLIENT_ID_INVALID(100015, "客户端身份标识无效");

    private final int code;
    private final String message;
}