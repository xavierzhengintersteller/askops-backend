package com.scb.tb.askopt_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scb.tb.askopt_backend.entity.SysUser;
import com.scb.tb.askopt_backend.vo.UserWithRolesVO;

import java.util.List;

public interface AdminMapper extends BaseMapper<SysUser> {
    List<UserWithRolesVO> selectUsersWithRoles();

}
