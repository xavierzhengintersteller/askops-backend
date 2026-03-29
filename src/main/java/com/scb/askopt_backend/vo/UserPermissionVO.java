package com.scb.askopt_backend.vo;

import lombok.Data;
import java.util.Set;

@Data
public class UserPermissionVO {
    private Set<Long> permissionIds;
    private Set<String> permissionCodes;
}