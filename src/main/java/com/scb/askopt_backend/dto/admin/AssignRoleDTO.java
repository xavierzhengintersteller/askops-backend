package com.scb.askopt_backend.dto.admin;

import lombok.Data;
import java.util.List;

@Data
public class AssignRoleDTO {
    private Long userId;
    private List<Long> roleIds;
}