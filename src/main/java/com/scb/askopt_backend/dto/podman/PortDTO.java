package com.scb.askopt_backend.dto.podman;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Podman容器端口映射对象，对应Go返回Ports数组内结构
 */
@Data
public class PortDTO {
    @JsonProperty("PrivatePort")
    private Integer PrivatePort;
    @JsonProperty("PublicPort")
    private Integer PublicPort;
    @JsonProperty("Type")
    private String Type;
//    private String HostIP;
}