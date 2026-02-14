package com.scb.askopt_backend.entity;

import lombok.Data;

@Data
public class SysRole {
    private Long id;
    private String roleCode; // e.g. ADMIN, DEV, QA
    private String roleName;

}

