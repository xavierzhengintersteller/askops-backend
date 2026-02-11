package com.scb.askopt_backend.mapper;

import com.scb.askopt_backend.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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

    /** 根据用户 ID 查询拥有的权限编码 */
    @Select("""
        SELECT p.permission_code
        FROM sys_permission p
        JOIN sys_user_permission up
          ON p.permission_code = up.permission_code
        WHERE up.user_id = #{userId}
    """)
    Set<String> findCodesByUserId(Long userId);

    // resolve incoming request path+method to a permission_code (or null)
    String findPermissionCodeByUrlAndMethod(@Param("path") String path, @Param("method") String method);

    // count matches where any of roleCodes grants permissionCode
    int countRolePermissionByRoleCodesAndPermissionCode(@Param("roleCodes") List<String> roleCodes,
                                                        @Param("permissionCode") String permissionCode);
}