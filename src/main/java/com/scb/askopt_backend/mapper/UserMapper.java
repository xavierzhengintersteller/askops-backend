package com.scb.askopt_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scb.askopt_backend.entity.SysUser;
import com.scb.askopt_backend.vo.AgentVO;
import com.scb.askopt_backend.vo.GroupVO;
import com.scb.askopt_backend.vo.UserMenuVO;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Set;

public interface UserMapper extends BaseMapper<SysUser> {

    @Select("SELECT * FROM askops_schema.sys_user WHERE username = #{username}")
    SysUser findByUsername(@Param("username") String username);

    @Select("SELECT * FROM askops_schema.sys_user WHERE id = #{userId}")
    SysUser findByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM askops_schema.sys_user WHERE id = #{userId}")
    SysUser selectUserById(@Param("userId") Long userId);

    @Select("SELECT * FROM askops_schema.sys_user")
    List<SysUser> selectAllUsers();

    @Update("UPDATE askops_schema.sys_user " +
            "SET permission_version = permission_version + 1 " +
            "WHERE id = #{userId}")
    void incrementPermissionVersion(@Param("userId") Long userId);

    @Select("SELECT permission_version FROM askops_schema.sys_user WHERE id = #{userId}")
    Long getPermissionVersion(@Param("userId") Long userId);
    @Select("""
            SELECT EXISTS (
                SELECT 1
                FROM askops_schema.user_role_mapping sur
                JOIN askops_schema.sys_role r ON sur.role_id = r.id
                WHERE sur.user_id = #{userId}
                  AND r.is_super_admin = true
                  AND r.role_code = 'admin'
            )
        """)
    boolean isSuperAdmin(@Param("userId") Long userId);

    // ===================== 菜单 + 权限 核心接口 =====================
    @Select("""
            SELECT DISTINCT
                sp.id,
                sp.parent_id,
                sp.permission_name,
                sp.path,
                sp.component,
                sp.icon,
                sp.sort
            FROM askops_schema.user_role_mapping sur
            JOIN askops_schema.role_permission_mapping srp ON sur.role_id = srp.role_id
            JOIN askops_schema.sys_permission sp ON srp.permission_id = sp.id
            WHERE sur.user_id = #{userId}
              AND sp.type = 'menu'
              AND sp.visible = true
            ORDER BY sp.sort
            """)
    List<UserMenuVO> selectUserMenuList(@Param("userId") Long userId);

    @Select("""
            SELECT DISTINCT sp.id
            FROM askops_schema.user_role_mapping sur
            JOIN askops_schema.role_permission_mapping srp ON sur.role_id = srp.role_id
            JOIN askops_schema.sys_permission sp ON srp.permission_id = sp.id
            WHERE sur.user_id = #{userId}
            """)
    Set<Long> selectUserPermissionIds(@Param("userId") Long userId);

    @Select("""
            SELECT DISTINCT sp.permission_code
            FROM askops_schema.user_role_mapping sur
            JOIN askops_schema.role_permission_mapping srp ON sur.role_id = srp.role_id
            JOIN askops_schema.sys_permission sp ON srp.permission_id = sp.id
            WHERE sur.user_id = #{userId}
            """)
    Set<String> selectUserPermissionCodes(@Param("userId") Long userId);
    // 超管 → 查询所有菜单
    @Select("""
        SELECT id, parent_id, permission_name, path, component, icon, sort
        FROM askops_schema.sys_permission
        WHERE type = 'menu' AND visible = true
        ORDER BY sort
        """)
    List<UserMenuVO> selectAllMenuList();

    // 超管 → 所有权限ID
    @Select("SELECT id FROM askops_schema.sys_permission")
    Set<Long> selectAllPermissionIds();

    // 超管 → 所有权限码
    @Select("SELECT permission_code FROM askops_schema.sys_permission")
    Set<String> selectAllPermissionCodes();
    // 查询用户拥有的角色IDS
    @Select("SELECT role_id FROM askops_schema.user_role_mapping WHERE user_id = #{userId}")
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    // 删除用户旧角色
    @Delete("DELETE FROM askops_schema.user_role_mapping WHERE user_id = #{userId}")
    void deleteUserRoles(@Param("userId") Long userId);

    // 批量插入用户角色
    @Insert("<script>"
            + "INSERT INTO askops_schema.user_role_mapping(user_id, role_id) VALUES "
            + "<foreach collection='roleIds' item='roleId' separator=','>"
            + "(#{userId}, #{roleId})"
            + "</foreach>"
            + "</script>")
    void insertUserRoles(
            @Param("userId") Long userId,
            @Param("roleIds") List<Long> roleIds
    );
    // ========================= 正确版本 =========================
// 1. 查询用户【通过角色】拥有的 组列表
    @Select("""
    SELECT DISTINCT
        g.id AS groupId,
        g.group_name AS groupName
    FROM askops_schema.user_role_mapping urm
    JOIN askops_schema.role_group_mapping rgm 
        ON urm.role_id = rgm.role_id
    JOIN askops_schema.t_group g 
        ON rgm.group_id = g.id
    WHERE urm.user_id = #{userId}
""")
    List<GroupVO> selectGroupsByUserId(@Param("userId") Long userId);

    // 2. 查询用户【通过角色→组】拥有的 Agent（带IP）
    @Select("""
    SELECT DISTINCT
        a.id AS agentId,
        a.ip,
        a.name,
        a.port,
        a.group_id AS groupId
    FROM askops_schema.user_role_mapping urm
    JOIN askops_schema.role_group_mapping rgm 
        ON urm.role_id = rgm.role_id
    JOIN askops_schema.t_agent a 
        ON rgm.group_id = a.group_id
    WHERE urm.user_id = #{userId}
""")
    List<AgentVO> selectAgentsByUserId(@Param("userId") Long userId);
}