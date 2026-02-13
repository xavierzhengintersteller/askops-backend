package com.scb.askopt_backend.mapper;

import com.scb.askopt_backend.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper {

    SysUser findByUsername(@Param("username") String username);

    int insert(SysUser user);

    // new: load role codes for a given user (e.g. ADMIN, DEV ...)
    List<String> findRoleCodesByUserId(@Param("userId") Long userId);

    // new: assign a role to a user by role_code
    int assignRoleToUserByCode(@Param("userId") Long userId, @Param("roleCode") String roleCode);
}
