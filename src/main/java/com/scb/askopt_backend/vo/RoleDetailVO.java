package com.scb.askopt_backend.vo;

import lombok.Data;
import java.util.List;

/**
 * 角色完整详情VO
 * 用于角色管理列表页面：展示角色 + 权限名称 + 组名称
 */
@Data
public class RoleDetailVO {

    private Long id;

    // 角色名称
    private String roleName;

    // 角色编码
    private String roleCode;

    // 拥有的权限名称列表（菜单+按钮）
    private List<String> permissionNames;

    // 可访问的组名称列表
    private List<String> groupNames;
}