package com.scb.askopt_backend.dto;

import lombok.Data;

import java.util.List;

/**
 * 容器详情DTO（根据实际返回字段定义）
 */
@Data
public class ContainerDTO {
    private String Id;
    private List<String> Names;
    private String Image;
    private String ImageID;
    private String Command;
    private Long Created;
    private List<PortDTO> Ports;
    private String State;
    private String Status;
    // 其他字段按需添加（Labels/NetworkSettings等）
}