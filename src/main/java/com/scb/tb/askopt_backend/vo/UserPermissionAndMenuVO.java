package com.scb.tb.askopt_backend.vo;

import lombok.Data;
import java.util.List;
import java.util.Set;

@Data
public class UserPermissionAndMenuVO {
    private Long permissionVersion;
    // 左侧菜单树
    private List<UserMenuVO> leftMenuTree;
    // 权限ID集合
    private Set<Long> permissionIds;
    // 权限码集合（按钮控制）
    private Set<String> permissionCodes;
}