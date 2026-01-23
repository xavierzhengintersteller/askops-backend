package com.scb.askopt_backend.mapper;

import com.scb.askopt_backend.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    SysUser findByUsername(@Param("username") String username);

    int insert(SysUser user);
}
