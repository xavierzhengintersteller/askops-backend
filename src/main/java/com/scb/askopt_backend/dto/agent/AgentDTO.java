package com.scb.askopt_backend.dto.agent;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AgentDTO {

    private Long id;

    private String name;

    private String ip;

    private Integer port;
    @Pattern(regexp = "REGISTERED|ONLINE|OFFLINE|DISABLED", message = "状态值只能是REGISTERED/ONLINE/OFFLINE/DISABLED")
    private String status;

    private LocalDateTime lastHeartbeatTime;

}