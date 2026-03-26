package com.scb.askopt_backend.vo;

import lombok.Data;

import java.util.List;

@Data
public class UserWithRolesVO {
    private Long userId;
    private String username;
    private Boolean enabled;
    private List<RoleVO> roles;

    @Data
    public static class RoleVO {
        private Long roleId;
        private String roleName;
    }
}
