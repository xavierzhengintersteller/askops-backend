package com.scb.askopt_backend.mapper;

import com.scb.askopt_backend.dto.PermissionRule;
import com.scb.askopt_backend.entity.SysPermission;
import lombok.Data;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Set;

@Mapper
public interface PermissionMapper {

    /** 启动时加载所有 URL 权限 */
    @Select("""
        SELECT id, permission_code, url_pattern, http_method, description
        FROM sys_permission
    """)
    List<SysPermission> findAllPermissions();


    // resolve incoming request path+method to a permission_code (or null)
    String findPermissionCodeByUrlAndMethod(@Param("path") String path, @Param("method") String method);

    // count matches where any of roleCodes grants permissionCode
    int countRolePermissionByRoleCodesAndPermissionCode(@Param("roleCodes") List<String> roleCodes,
                                                        @Param("permissionCode") String permissionCode);

    Set<String> findCodesByRoleCodes(List<String> roles);

    /**
     * 根据用户 ID 查询角色 + 权限
     * 返回 Map<role_code, Set<permission_code>>
     */
    @Select("""
        SELECT r.role_code, p.permission_code
        FROM sys_user_role ur
        JOIN sys_role r ON ur.role_code = r.role_code
        JOIN sys_role_permission rp ON r.role_code = rp.role_code
        JOIN sys_permission p ON rp.permission_code = p.permission_code
        WHERE ur.user_id = #{userId}
    """)
    @Results({
            @Result(property = "roleCode", column = "role_code"),
            @Result(property = "permissionCode", column = "permission_code")
    })
    List<RolePermission> findRolesAndPermissionsByUserId(@Param("userId") Long userId);
    @Data
    class RolePermission {
        private String roleCode;
        private String permissionCode;
        // getters/setters
    }

    /**List all permission in to memory when server start, and sort by url_pattern length desc for longest match first
     *
     * @return
     */
    @Select("""
        SELECT url_pattern AS pattern,
               http_method AS method,
               permission_code AS permission
        FROM sys_permission
    """)
    List<PermissionRule> selectAll();
}