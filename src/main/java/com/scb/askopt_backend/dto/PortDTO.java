package com.scb.askopt_backend.dto;

import lombok.Data;

/**
 * 容器端口DTO
 */
@Data
public class PortDTO {
    private Integer PrivatePort;
    private Integer PublicPort;
    private String Type;
}