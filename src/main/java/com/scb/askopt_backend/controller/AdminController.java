package com.scb.askopt_backend.controller;

import com.scb.askopt_backend.annotation.AuditLog;
import com.scb.askopt_backend.constant.AuditConstant;
import com.scb.askopt_backend.dto.AddRoleDTO;
import com.scb.askopt_backend.dto.admin.*;
import com.scb.askopt_backend.entity.SysRole;
import com.scb.askopt_backend.context.AuthContext;
import com.scb.askopt_backend.service.AdminService;
import com.scb.askopt_backend.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
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
     * 【查询：不加审计】
     */
    @GetMapping("/users")
    public ApiResponse<List<UserWithRolesVO>> getUsersWithRoles() {
        List<UserWithRolesVO> users = adminService.getUsersWithRoles();
        return ApiResponse.success(users);
    }

    /**
     * 用户列表
     * 【查询：不加审计】
     */
    @GetMapping("/user/list")
    public ApiResponse<List<UserPageVO>> list() {
        return ApiResponse.success(adminService.userList());
    }

    /**
     * 查询单个用户详情
     * 【查询：不加审计】
     */
    @GetMapping("/user/{id}")
    public ApiResponse<UserPageVO> detail(@PathVariable Long id) {
        return ApiResponse.success(adminService.getUserDetail(id));
    }

    /**
     * 角色下拉列表
     * 【查询：不加审计】
     */
    @GetMapping("/role/list")
    public ApiResponse<List<SysRole>> roleList() {
        return ApiResponse.success(adminService.listAllRoles());
    }

    /**
     * 给用户分配角色
     */
    @AuditLog(module = "USER_ROLE", operation = AuditConstant.UPDATE)
    @PostMapping("/user/assign-role")
    public ApiResponse<Void> assignRole(@RequestBody AssignRoleDTO dto) {
        adminService.assignRolesToUser(dto);
        return ApiResponse.success();
    }

    // =====Role管理=====

    /**
     * 给角色分配权限
     */
    @AuditLog(module = "ROLE_PERMISSION", operation = AuditConstant.UPDATE)
    @PostMapping("/role-permission/assign")
    public ApiResponse<Void> assignPermissionsToRole(@RequestBody AssignPermissionToRoleDTO dto) {
        adminService.assignPermissionsToRole(dto);
        return ApiResponse.success();
    }

    /**
     * 获取角色已分配的权限ID
     * 【查询：不加审计】
     */
    @GetMapping("/role/permission/ids")
    public ApiResponse<List<Long>> getPermissionIdsByRoleId(@RequestParam Long roleId) {
        return ApiResponse.success(adminService.getPermissionIdsByRoleId(roleId));
    }

    /**
     * 权限树
     * 【查询：不加审计】
     */
    @GetMapping("/permission/list")
    public ApiResponse<List<PermissionTreeVO>> permissionTree() {
        return ApiResponse.success(adminService.getPermissionTree());
    }

    /**
     * 删除角色
     */
    @Transactional(rollbackFor = Exception.class)
    @AuditLog(module = "ROLE", operation = AuditConstant.DELETE)
    @PostMapping("/role/delete/{roleId}")
    public ApiResponse<Void> deleteRole(@PathVariable Long roleId) {
        adminService.deleteRoleById(roleId);
        return ApiResponse.success();
    }

    /**
     * 角色详情列表
     * 【查询：不加审计】
     */
    @GetMapping("/role/detail/list")
    public ApiResponse<List<RoleDetailVO>> getAllRoleDetail() {
        return ApiResponse.success(adminService.getAllRoleDetailList());
    }

    /**
     * 新增角色
     */
    @AuditLog(module = "ROLE", operation = AuditConstant.CREATE)
    @PostMapping("/role/add")
    public ApiResponse<Void> addRole(@RequestBody AddRoleDTO dto) {
        adminService.addRole(dto);
        return ApiResponse.success();
    }

    // ==================== 角色 ↔ 组 关联管理 ====================

    /**
     * 组列表
     * 【查询：不加审计】
     */
    @GetMapping("/role-group/list")
    public ApiResponse<List<GroupVO>> getAllGroupList() {
        return ApiResponse.success(adminService.getGroupsByRoleId(null));
    }

    /**
     * 给角色分配组
     */
    @AuditLog(module = "ROLE_GROUP", operation = AuditConstant.UPDATE)
    @PostMapping("/role-group/assign")
    public ApiResponse<Void> assignGroupsToRole(@RequestBody AssignRoleGroupDTO dto) {
        adminService.assignGroupsToRole(dto);
        return ApiResponse.success();
    }

    /**
     * 获取角色已分配的组ID
     * 【查询：不加审计】
     */
    @GetMapping("/role-group/by-role")
    public ApiResponse<List<Long>> getGroupIdsByRoleId(@RequestParam Long roleId) {
        return ApiResponse.success(adminService.getGroupIdsByRoleId(roleId));
    }

    /**
     * 清空角色的组
     */
    @AuditLog(module = "ROLE_GROUP", operation = AuditConstant.UPDATE)
    @DeleteMapping("/role-group/clear")
    public ApiResponse<Void> clearRoleGroups(@RequestParam Long roleId) {
        adminService.clearRoleGroups(roleId);
        return ApiResponse.success();
    }

    /**
     * 新增用户
     */
    @AuditLog(module = "USER", operation = AuditConstant.CREATE)
    @PostMapping("/user/add")
    public ApiResponse<Void> addUser(@RequestBody AddUserDTO dto) {
        if (!AuthContext.isSuperAdmin()) {
            return ApiResponse.error(403, "仅超管可操作");
        }
        adminService.addUser(dto);
        return ApiResponse.success();
    }

    /**
     * 用户拉黑/解禁
     */
    @AuditLog(module = "USER", operation = AuditConstant.UPDATE)
    @PostMapping("/user/blacklist")
    public ApiResponse<Void> blacklistUser(@RequestBody BlacklistUserDTO dto) {
        adminService.updateUserStatus(dto);
        return ApiResponse.success();
    }

    /**
     * 删除用户
     */
    @AuditLog(module = "USER", operation = AuditConstant.DELETE)
    @PostMapping("/user/delete/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return ApiResponse.success();
    }

    /**
     * 修改用户密码
     */
    @AuditLog(module = "USER", operation = AuditConstant.UPDATE)
    @PostMapping("/user/update-password")
    public ApiResponse<Void> updatePassword(@RequestBody UpdateUserPwdDTO dto) {
        adminService.updatePassword(dto);
        return ApiResponse.success();
    }
}