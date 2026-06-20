package com.scb.askopt_backend.dto.podman;

import lombok.Data;

@Data
public class AgentSingleRestartResult {
    private String containerName;
    private boolean success;
    private String msg;
}