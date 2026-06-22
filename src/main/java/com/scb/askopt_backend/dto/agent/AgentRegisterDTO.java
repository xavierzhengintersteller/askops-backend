package com.scb.askopt_backend.dto.agent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AgentRegisterDTO {
    @NotBlank(message = "agent名称不能为空")
    private String name;
    @NotBlank(message = "ip不能为空")
    private String ip;
    @NotNull(message = "port不能为空")
    private Integer port;
    private Long groupId;
    private Integer heartbeatTimeoutSec;
    @NotBlank(message = "clientId不能为空")
    private String clientId;
}