package com.scb.askopt_backend.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.scb.askopt_backend.dto.AddRoleDTO;
import com.scb.askopt_backend.dto.admin.*;
import com.scb.askopt_backend.entity.SysRole;
import com.scb.askopt_backend.security.AuthContext;
import com.scb.askopt_backend.service.AdminService;
import com.scb.askopt_backend.service.PermissionService;
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
     * GET /api/users
     */
    @GetMapping("/users")
    public ApiResponse<List<UserWithRolesVO>> getUsersWithRoles() {
        List<UserWithRolesVO> users = adminService.getUsersWithRoles();
        return ApiResponse.success(users);
    }
    @GetMapping("/user/list")
    public ApiResponse<List<UserPageVO>> list() {
        return ApiResponse.success(adminService.userList());
    }

    /**
     * 查询单个用户详情+角色IDS
     *
     */
    @GetMapping("/user/{id}")
    public ApiResponse<UserPageVO> detail(@PathVariable Long id) {
        return ApiResponse.success(adminService.getUserDetail(id));
    }

    /**
     * 角色下拉列表
     */
    @GetMapping("/role/list")
    public ApiResponse<List<SysRole>> roleList() {
        return ApiResponse.success(adminService.listAllRoles());
    }

    /**
     * 给用户分配角色
     */
    @PostMapping("/user/assign-role")
    public ApiResponse<Void> assignRole(@RequestBody AssignRoleDTO dto) {

        adminService.assignRolesToUser(dto);
        return ApiResponse.success();
    }
    // =====Role管理=====
    /**
     * 给角色分配权限      * POST /api/admin/role-permission/assign
     */
    @PostMapping("/role-permission/assign")
    public ApiResponse<Void> assignPermissionsToRole(@RequestBody AssignPermissionToRoleDTO dto) {

        adminService.assignPermissionsToRole(dto);
        return ApiResponse.success();
    }
    /**
     * 获取角色已分配的权限ID集合（用于权限树回显）
     */
    @GetMapping("/role/permission/ids")
    public ApiResponse<List<Long>> getPermissionIdsByRoleId(@RequestParam Long roleId) {
        return ApiResponse.success(adminService.getPermissionIdsByRoleId(roleId));
    }
    /**
     * 获取权限树（给前端权限管理页面使用）
     */
    @GetMapping("/permission/list")
    public ApiResponse<List<PermissionTreeVO>> permissionTree() {
        return ApiResponse.success(adminService.getPermissionTree());
    }
    /**
     * 删除角色
     * 1. 禁止删除超级管理员角色
     * 2. 级联清空：角色-权限、角色-组 关联关系
     * 3. 事务保证数据一致性
     *
     * @param roleId 角色ID
     * @return 统一响应
     */
    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/role/delete/{roleId}")
    public ApiResponse<Void> deleteRole(@PathVariable Long roleId) {
        adminService.deleteRoleById(roleId);
        return ApiResponse.success();
    }
    /**
     * 获取所有角色完整详情（用于角色管理列表）
     * 包含：角色信息 + 权限名称列表 + 组名称列表
     */
    @GetMapping("/role/detail/list")
    public ApiResponse<List<RoleDetailVO>> getAllRoleDetail() {
        return ApiResponse.success(adminService.getAllRoleDetailList());
    }
    /**
     * 新增角色
     */
    @PostMapping("/role/add")
    public ApiResponse<Void> addRole(@RequestBody AddRoleDTO dto) {
        adminService.addRole(dto);
        return ApiResponse.success();
    }
    // =====Role管理=====
    // ==================== 角色 ↔ 组 关联管理 ====================

    /**
     * 获取【所有组】列表（给角色分配组使用，穿梭框数据源）
     */
    @GetMapping("/role-group/list")
    public ApiResponse<List<GroupVO>> getAllGroupList() {
        // 注意：这里传 null 就是查所有组
        return ApiResponse.success(adminService.getGroupsByRoleId(null));
    }
    /**
     * 给角色分配组（先删后插）
     */
    @PostMapping("/role-group/assign")
    public ApiResponse<Void> assignGroupsToRole(@RequestBody AssignRoleGroupDTO dto) {

        adminService.assignGroupsToRole(dto);
        return ApiResponse.success();
    }

    /**
     * 获取角色已分配的组ID集合（用于前端回显）
     */
    @GetMapping("/role-group/by-role")
    public ApiResponse<List<Long>> getGroupIdsByRoleId(@RequestParam Long roleId) {

        return ApiResponse.success(adminService.getGroupIdsByRoleId(roleId));
    }

    /**
     * 清空角色的所有组
     */
    @DeleteMapping("/role-group/clear")
    public ApiResponse<Void> clearRoleGroups(@RequestParam Long roleId) {
        adminService.clearRoleGroups(roleId);
        return ApiResponse.success();
    }

    @PostMapping("/user/add")
    public ApiResponse<Void> addUser(@RequestBody AddUserDTO dto) {
        if (!AuthContext.isSuperAdmin()) {
            return ApiResponse.error(403, "仅超管可操作");
        }
        adminService.addUser(dto);
        return ApiResponse.success();
    }
    @PostMapping("/user/blacklist")
    public ApiResponse<Void> blacklistUser(@RequestBody BlacklistUserDTO dto) {
        adminService.updateUserStatus(dto);
        return ApiResponse.success();
    }
    @PostMapping("/user/delete/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return ApiResponse.success();
    }
    @PostMapping("/user/update-password")
    public ApiResponse<Void> updatePassword(@RequestBody UpdateUserPwdDTO dto) {
        adminService.updatePassword(dto);
        return ApiResponse.success();
    }
}
