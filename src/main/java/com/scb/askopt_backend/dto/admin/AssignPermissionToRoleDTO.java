package com.scb.askopt_backend.dto.admin;

import lombok.Data;

import java.util.List;

@Data
public class AssignPermissionToRoleDTO {
    private Long roleId;
    private List<Long> permissionIds;
}
