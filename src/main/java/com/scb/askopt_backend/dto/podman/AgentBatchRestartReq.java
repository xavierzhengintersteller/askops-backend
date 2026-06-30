package com.scb.askopt_backend.dto.podman;

import lombok.Data;
import java.util.List;

/**
 * 发给Go Agent批量重启接口的请求体
 */
@Data
public class AgentBatchRestartReq {
    private List<String> ContainerIds;
}