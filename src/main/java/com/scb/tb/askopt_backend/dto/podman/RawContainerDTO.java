package com.scb.tb.askopt_backend.dto.podman;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class RawContainerDTO {
    @JsonProperty("nodeIp")
    private String nodeIp;

    // Go json key: id
    @JsonProperty("id")
    private String id;

    // Go json key: name
    @JsonProperty("name")
    private String name;

    // Go json key: image
    @JsonProperty("image")
    private String image;

    // Go json key: command
    @JsonProperty("command")
    private String command;

    // Go json key: created
    @JsonProperty("created")
    private Long created;

    // Go json key: state
    @JsonProperty("state")
    private String state;

    // Go json key: status
    @JsonProperty("status")
    private String status;

    // Go json key: ports
    @JsonProperty("ports")
    private List<PortDTO> ports;
}