package com.scb.askopt_backend.dto.agent;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AgentDTO {

    private Long id;

    private String name;

    private String ip;

    private Integer port;

    private String version;

    private String status;

    private LocalDateTime lastHeartbeatTime;

}