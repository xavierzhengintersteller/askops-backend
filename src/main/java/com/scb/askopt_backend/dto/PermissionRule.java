package com.scb.askopt_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PermissionRule {

    private String pattern;
    private String method;
    private String permission;

}
