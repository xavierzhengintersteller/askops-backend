package com.scb.tb.askopt_backend.dto.podman;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ContainerInfoDTO {
    private String nodeIp;
    private String containerId;
    private String shortId;
    private String containerName;
    private String image;
    private String imageId;
    private String state;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime lastSeenTime;
    private List<PortDTO> ports;
    // 详情弹窗拓展字段
    private String command;
}