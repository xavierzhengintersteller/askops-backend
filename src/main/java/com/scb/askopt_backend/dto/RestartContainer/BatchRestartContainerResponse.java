package com.scb.askopt_backend.dto.RestartContainer;

import lombok.Data;

import java.util.List;

// 批量重启响应DTO
@Data
public class BatchRestartContainerResponse {
    private int total; // 总请求数
    private int success; // 成功数
    private int fail; // 失败数
    private List<RestartResult> results; // 每个容器的重启结果
}