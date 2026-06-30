package com.scb.askopt_backend.dto.RestartContainer;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ContainerRestartItem {
    @NotBlank(message = "容器ID不能为空")
    private String containerId; // 64位完整容器ID

    @NotBlank(message = "节点IP不能为空")
    private String nodeIp;      // 容器所属节点IP
}