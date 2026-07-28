package com.scb.tb.askopt_backend.dto.RestartContainer;

import lombok.Data;

// 单个容器重启结果
@Data
public class RestartResult {
    private String containerName;
    private String nodeIp;
    private boolean success; // 是否成功
    private String message; // 失败原因（成功则为null）
}