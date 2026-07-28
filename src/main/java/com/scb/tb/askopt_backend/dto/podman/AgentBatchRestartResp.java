package com.scb.tb.askopt_backend.dto.podman;

import lombok.Data;
import java.util.List;

@Data
public class AgentBatchRestartResp {
    private Integer total;
    private Integer success;
    private Integer fail;
    private List<AgentSingleRestartResult> results;
}