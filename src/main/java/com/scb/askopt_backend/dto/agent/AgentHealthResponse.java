package com.scb.askopt_backend.dto.agent;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AgentHealthResponse {

    private Long total;
    private Long success;
    private Long failed;
    private List<AgentDTO> agents;

}