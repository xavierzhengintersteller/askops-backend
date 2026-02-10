package com.scb.askopt_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContainerInfo {
    private String id;
    private String name;
    private String status;
}
