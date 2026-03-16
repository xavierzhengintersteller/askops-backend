package com.scb.askopt_backend.constant;

public enum AgentStatus {
    /** 已注册（刚完成注册，未完成首次心跳/可用性校验） */
    REGISTERED("REGISTERED", "已注册"),
    /** 在线（完成心跳，可用） */
    ONLINE("ONLINE", "在线"),
    /** 离线（心跳超时，不可用） */
    OFFLINE("OFFLINE", "离线"),
    /** 禁用（人工禁用，不可用） */
    DISABLED("DISABLED", "禁用");

    private final String code;
    private final String desc;

    AgentStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}