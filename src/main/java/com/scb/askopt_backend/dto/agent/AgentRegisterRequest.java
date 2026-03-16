package com.scb.askopt_backend.dto.agent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AgentRegisterRequest {
    @NotBlank(message = "Agent IP不能为空")  // 非空且非空白字符串
    private String ip;          // Agent IP（必填）

    @NotNull(message = "Agent 端口不能为空") // 非null（整数类型不能用NotBlank）
    private Integer port;       // Agent 端口（必填）

    @NotNull(message = "所属分组ID不能为空")
    private Long groupId;       // 所属分组（必填）唯一
    private String name;        // Agent 名称（可选）
}
