package com.scb.tb.askopt_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scb.tb.askopt_backend.entity.SysRole;
import com.scb.tb.askopt_backend.vo.GroupVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface RoleMapper extends BaseMapper<SysRole> {
    // ==================== 角色 ↔ 组 关联 ====================

    // 根据角色ID查组列表
    @Select("""
        SELECT DISTINCT
            g.id AS groupId,
            g.group_name AS groupName
        FROM askops_schema.role_group_mapping rgm
        JOIN askops_schema.t_group g ON rgm.group_id = g.id
        WHERE rgm.role_id = #{roleId}
    """)
    List<GroupVO> selectGroupsByRoleId(@Param("roleId") Long roleId);

    // 根据角色ID查组ID
    @Select("""
        SELECT group_id
        FROM askops_schema.role_group_mapping
        WHERE role_id = #{roleId}
    """)
    List<Long> selectGroupIdsByRoleId(@Param("roleId") Long roleId);

    // 删除角色的所有组
    @Delete("""
        DELETE FROM askops_schema.role_group_mapping
        WHERE role_id = #{roleId}
    """)
    void deleteRoleGroups(@Param("roleId") Long roleId);

    // 批量插入角色-组关系
    @Insert("<script>"
            + "INSERT INTO askops_schema.role_group_mapping(role_id, group_id) VALUES "
            + "<foreach collection='groupIds' item='groupId' separator=','>"
            + "(#{roleId}, #{groupId})"
            + "</foreach>"
            + "</script>")
    void batchInsertRoleGroups(
            @Param("roleId") Long roleId,
            @Param("groupIds") List<Long> groupIds
    );
    // 删除角色的所有权限
    @Delete("""
        DELETE FROM askops_schema.role_permission_mapping
        WHERE role_id = #{roleId}
    """)
    void deleteRolePermissions(@Param("roleId") Long roleId);

    // 批量插入角色-权限关系
    @Insert("<script>"
            + "INSERT INTO askops_schema.role_permission_mapping(role_id, permission_id) VALUES "
            + "<foreach collection='permissionIds' item='permissionId' separator=','>"
            + "(#{roleId}, #{permissionId})"
            + "</foreach>"
            + "</script>")
    void batchInsertRolePermissions(
            @Param("roleId") Long roleId,
            @Param("permissionIds") List<Long> permissionIds
    );
}
