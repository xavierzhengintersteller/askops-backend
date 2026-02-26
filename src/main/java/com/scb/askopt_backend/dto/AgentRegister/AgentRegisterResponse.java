package com.scb.askopt_backend.dto.AgentRegister;

import lombok.Data;

@Data
public class AgentRegisterResponse {
    private Long agentId;       // 分配的Agent ID
    private String message;     // 提示信息
    private Boolean success;    // 是否成功
}



