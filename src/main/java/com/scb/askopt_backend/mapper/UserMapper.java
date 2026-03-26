package com.scb.askopt_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scb.askopt_backend.entity.SysUser;
import com.scb.askopt_backend.vo.UserWithRolesVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserMapper extends BaseMapper<SysUser> {

    SysUser findByUsername(@Param("username") String username);

    SysUser findByUserId(@Param("userId") Long userId);

    int insert(SysUser user);
    /**
     * 根据用户ID查询用户信息
     * @param userId 用户ID
     * @return 系统用户信息
     */
    SysUser selectUserById(@Param("userId") Long userId);

    /**
     * 查询所有用户信息
     * @return 用户列表
     */
    List<SysUser> selectAllUsers();

    /**
     * 递增用户权限版本号（你现有代码中的方法）
     * 说明：需确保sys_user表有permission_version字段，若无需先新增该字段
     * @param userId 用户ID
     */
    void incrementPermissionVersion(@Param("userId") Long userId);

    /**
     * 获取用户当前的权限版本号（你现有代码中的方法）
     * @param userId 用户ID
     * @return 权限版本号
     */
    Long getPermissionVersion(@Param("userId") Long userId);
    /**
     * 查询所有用户及其对应的角色列表
     * @return 用户+角色VO集合
     */
    List<UserWithRolesVO> selectUsersWithRoles();
}
