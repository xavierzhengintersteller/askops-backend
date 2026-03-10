package com.scb.askopt_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ContainerInfoDTO {

    // 基本信息
    @JsonProperty("Id")
    private String id;

    @JsonProperty("Names")
    private List<String> names;

    @JsonProperty("Image")
    private String image;

    @JsonProperty("ImageID")
    private String imageID;

    @JsonProperty("Command")
    private String command;

    @JsonProperty("Created")
    private Long created;

    @JsonProperty("State")
    private String state;

    @JsonProperty("Status")
    private String status;

    // 网络和挂载信息
    @JsonProperty("NetworkSettings")
    private NetworkSettings networkSettings;

    @JsonProperty("Mounts")
    private List<Mount> mounts;

    // 端口信息
    @JsonProperty("Ports")
    private List<Port> ports;

    // Labels 信息
    @JsonProperty("Labels")
    private Map<String, String> labels;

    // 可选字段
    @JsonProperty("Name")
    private String name;

    @JsonProperty("Config")
    private Map<String, Object> config;

    @JsonProperty("NetworkingConfig")
    private Map<String, Object> networkingConfig;

    @JsonProperty("Platform")
    private String platform;

    @JsonProperty("AdjustCPUShares")
    private Boolean adjustCPUShares;

    // ⭐ 自定义字段：节点 IP
    private String nodeIp;

    // 内部类：端口
    @Data
    public static class Port {
        @JsonProperty("PrivatePort")
        private Integer privatePort;

        @JsonProperty("PublicPort")
        private Integer publicPort;

        @JsonProperty("Type")
        private String type;

        @JsonProperty("HostIP")
        private String hostIP; // 有些 podman 版本会返回 hostIP
    }

    // 内部类：挂载
    @Data
    public static class Mount {
        @JsonProperty("Type")
        private String type;

        @JsonProperty("Source")
        private String source;

        @JsonProperty("Destination")
        private String destination;

        @JsonProperty("Mode")
        private String mode;

        @JsonProperty("RW")
        private Boolean rw;

        @JsonProperty("Propagation")
        private String propagation;
    }

    // 内部类：网络信息
    @Data
    public static class NetworkSettings {
        @JsonProperty("Networks")
        private Map<String, Object> networks; // Networks 可以是 Map<String, Object>
    }
}