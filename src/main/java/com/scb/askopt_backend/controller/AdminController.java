package com.scb.askopt_backend.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.scb.askopt_backend.dto.admin.AssignPermissionToRoleDTO;
import com.scb.askopt_backend.dto.admin.AssignRoleDTO;
import com.scb.askopt_backend.dto.admin.AssignRoleGroupDTO;
import com.scb.askopt_backend.dto.admin.UserPageDTO;
import com.scb.askopt_backend.entity.SysRole;
import com.scb.askopt_backend.security.AuthContext;
import com.scb.askopt_backend.service.AdminService;
import com.scb.askopt_backend.service.PermissionService;
import com.scb.askopt_backend.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RBAC 管理API控制器
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;
    /**
     * 查询用户列表（带角色）
     * GET /api/users
     */
    @GetMapping("/users")
    public ApiResponse<List<UserWithRolesVO>> getUsersWithRoles() {
        List<UserWithRolesVO> users = adminService.getUsersWithRoles();
        return ApiResponse.success(users);
    }
    /**
     * 分页查询用户列表
     */
    @GetMapping("/user/page")
    public ApiResponse<IPage<UserPageVO>> page(UserPageDTO dto) {
        if (!AuthContext.isSuperAdmin()) {
            return ApiResponse.error(403, "仅超级管理员可访问");
        }
        return ApiResponse.success(adminService.pageUser(dto));
    }

    /**
     * 查询单个用户详情+角色IDS
     */
    @GetMapping("/user/{id}")
    public ApiResponse<UserPageVO> detail(@PathVariable Long id) {
        if (!AuthContext.isSuperAdmin()) {
            return ApiResponse.error(403, "仅超级管理员可访问");
        }
        return ApiResponse.success(adminService.getUserDetail(id));
    }

    /**
     * 角色下拉列表
     */
    @GetMapping("/role/list")
    public ApiResponse<List<SysRole>> roleList() {
        if (!AuthContext.isSuperAdmin()) {
            return ApiResponse.error(403, "仅超级管理员可访问");
        }
        return ApiResponse.success(adminService.listAllRoles());
    }

    /**
     * 给用户分配角色
     */
    @PutMapping("/user/assign-role")
    public ApiResponse<Void> assignRole(@RequestBody AssignRoleDTO dto) {
        if (!AuthContext.isSuperAdmin()) {
            return ApiResponse.error(403, "仅超级管理员可操作");
        }
        adminService.assignRolesToUser(dto);
        return ApiResponse.success();
    }
    /**
     * 给角色分配权限      * POST /api/admin/role/assign-permissions
     */
    @PostMapping("/role/assign-permissions")
    public ApiResponse<Void> assignPermissionsToRole(@RequestBody AssignPermissionToRoleDTO dto) {
        if (!AuthContext.isSuperAdmin()) {
            return ApiResponse.error(403, "仅超级管理员可操作");
        }
        adminService.assignPermissionsToRole(dto);
        return ApiResponse.success();
    }
    // ==================== 角色 ↔ 组 关联管理 ====================

    /**
     * 根据角色ID查询已分配的组列表
     */
    @GetMapping("/role-group/list")
    public ApiResponse<List<GroupVO>> getGroupsByRoleId(@RequestParam Long roleId) {
        if (!AuthContext.isSuperAdmin()) {
            return ApiResponse.error(403, "仅超级管理员可操作");
        }
        return ApiResponse.success(adminService.getGroupsByRoleId(roleId));
    }

    /**
     * 给角色分配组（先删后插）
     */
    @PostMapping("/role-group/assign")
    public ApiResponse<Void> assignGroupsToRole(@RequestBody AssignRoleGroupDTO dto) {
        if (!AuthContext.isSuperAdmin()) {
            return ApiResponse.error(403, "仅超级管理员可操作");
        }
        adminService.assignGroupsToRole(dto);
        return ApiResponse.success();
    }

    /**
     * 获取角色已分配的组ID集合（用于前端回显）
     */
    @GetMapping("/role-group/by-role")
    public ApiResponse<List<Long>> getGroupIdsByRoleId(@RequestParam Long roleId) {
        if (!AuthContext.isSuperAdmin()) {
            return ApiResponse.error(403, "仅超级管理员可操作");
        }
        return ApiResponse.success(adminService.getGroupIdsByRoleId(roleId));
    }

    /**
     * 清空角色的所有组
     */
    @DeleteMapping("/role-group/clear")
    public ApiResponse<Void> clearRoleGroups(@RequestParam Long roleId) {
        if (!AuthContext.isSuperAdmin()) {
            return ApiResponse.error(403, "仅超级管理员可操作");
        }
        adminService.clearRoleGroups(roleId);
        return ApiResponse.success();
    }
    /**
     * 获取权限树（给前端权限管理页面使用）
     */
    @GetMapping("/permission/list")
    public ApiResponse<List<PermissionTreeVO>> permissionTree() {
        if (!AuthContext.isSuperAdmin()) {
            return ApiResponse.error(403, "仅超级管理员可访问");
        }
        return ApiResponse.success(adminService.getPermissionTree());
    }
}
