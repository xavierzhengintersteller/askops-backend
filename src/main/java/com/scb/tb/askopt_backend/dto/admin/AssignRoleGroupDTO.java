package com.scb.tb.askopt_backend.dto.admin;


import lombok.Data;
import java.util.List;

@Data
public class AssignRoleGroupDTO {
    private Long roleId;
    private List<Long> groupIds;
}