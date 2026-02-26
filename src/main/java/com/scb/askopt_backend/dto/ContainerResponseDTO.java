package com.scb.askopt_backend.dto;

import lombok.Data;

import java.util.List;

@Data
public class ContainerResponseDTO {
    private String agentIp;        // Agent服务器IP
    private String agentName;      // Agent服务器名称（可选）
    private String agentGroup;     // 所属分组（可选）
    private List<ContainerDTO> containers; // 该Agent下的容器列表（解析后的对象，非字符串）
}