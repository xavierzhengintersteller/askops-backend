package com.scb.askopt_backend.dto;

import lombok.Data;

@Data
public class RestartContainerRequest {
    private String containerId;
    private String nodeIp;
    private boolean success;
    private String message; // 成功/失败原因
}