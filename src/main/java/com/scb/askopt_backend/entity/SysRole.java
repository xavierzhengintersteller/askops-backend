package com.scb.askopt_backend.entity;

public class SysRole {
    private Long id;
    private String roleCode; // e.g. ADMIN, DEV, QA
    private String roleName;

    // getters / setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
}

