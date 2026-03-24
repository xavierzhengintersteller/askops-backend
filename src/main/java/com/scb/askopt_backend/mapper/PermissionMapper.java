package com.scb.askopt_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scb.askopt_backend.entity.SysPermission;
import lombok.Data;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Set;

public interface PermissionMapper extends BaseMapper<SysPermission> {

    // resolve incoming request path+method to a permission_code (or null)
    String findPermissionCodeByUrlAndMethod(@Param("path") String path, @Param("method") String method);

    // count matches where any of roleCodes grants permissionCode
    int countRolePermissionByRoleCodesAndPermissionCode(@Param("roleCodes") List<String> roleCodes,
                                                        @Param("permissionCode") String permissionCode);

    Set<String> findCodesByRoleCodes(List<String> roles);
    // 1. 查询用户关联的角色ID、权限ID（核心保留）
    List<RolePermissionId> findRoleIdsAndPermissionIdsByUserId(@Param("userId") Long userId);

    // 2. 新增：通过用户ID查询关联的分组ID（用户→角色→分组）
    Set<Long> findGroupIdsByUserId(@Param("userId") Long userId);

    // 3. 查询超级管理员角色ID（如 opsadmin 对应角色）
    Long getSuperAdminRoleId();
    @Data
    class RolePermissionId {
        private Long roleId;
        private Long permissionId;
    }
    /**
     * 根据用户ID查询该用户拥有的所有权限ID
     * @param userId 用户ID
     * @return 权限ID集合（Set<Long>）
     */
    Set<Long> findPermissionIdsByUserId(@Param("userId") Long userId);

}