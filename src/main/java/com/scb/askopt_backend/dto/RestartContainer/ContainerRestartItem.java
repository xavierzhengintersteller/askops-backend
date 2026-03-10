package com.scb.askopt_backend.dto.RestartContainer;

import lombok.Data;

// 单个容器重启项
@Data
public class ContainerRestartItem {
    private String containerName; // 容器名
    private String nodeIp;        // 容器所属节点IP
}