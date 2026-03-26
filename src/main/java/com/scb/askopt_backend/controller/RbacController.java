package com.scb.askopt_backend.controller;

import com.scb.askopt_backend.entity.SysPermission;
import com.scb.askopt_backend.service.PermissionService;
import com.scb.askopt_backend.service.UserService;
import com.scb.askopt_backend.vo.ApiResponse;
import com.scb.askopt_backend.vo.UserWithRolesVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RBAC 管理API控制器
 */
@RestController
@RequestMapping("/api/rbac")
public class RbacController {

    @Autowired
    private UserService userService;

    @Autowired
    private PermissionService permissionService;

    /**
     * 查询用户列表（带角色）
     * GET /api/users
     */
    @GetMapping("/users")
    public ApiResponse<List<UserWithRolesVO>> getUsersWithRoles() {
        List<UserWithRolesVO> users = userService.getUsersWithRoles();
        return ApiResponse.success(users);
    }

//    /**
//     * 给用户分配角色
//     * POST /api/users/{id}/roles
//     */
//    @PostMapping("/users/{id}/roles")
//    public ApiResponse<String> assignRolesToUser(@PathVariable Long id, @RequestBody List<Long> roleIds) {
//        userService.assignRolesToUser(id, roleIds);
//        return ApiResponse.success("角色分配成功");
//    }

//    /**
//     * 查询角色（带权限）
//     * GET /api/roles
//     */
//    @GetMapping("/roles")
//    public ApiResponse<List<SysRole>> getRolesWithPermissions() {
//        List<SysRole> roles = roleService.getRolesWithPermissions();
//        return ApiResponse.success(roles);
//    }
//
//    /**
//     * 给角色分配权限
//     * POST /api/roles/{id}/permissions
//     */
//    @PostMapping("/roles/{id}/permissions")
//    public ApiResponse<String> assignPermissionsToRole(@PathVariable Long id, @RequestBody List<Long> permissionIds) {
//        roleService.assignPermissionsToRole(id, permissionIds);
//        return ApiResponse.success("权限分配成功");
//    }

    /**
     * 查询权限列表
     * GET /api/permissions
     */
    @GetMapping("/permissions")
    public ApiResponse<List<SysPermission>> getPermissions() {
        List<SysPermission> permissions = permissionService.list();
        return ApiResponse.success(permissions);
    }
}
