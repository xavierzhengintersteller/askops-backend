package com.scb.askopt_backend.entity;

import lombok.Data;

@Data
public class SysUser {
    private Long id;
    private String username;
    private String password;
    private Boolean enabled;
    private Long permissionVersion;
}
