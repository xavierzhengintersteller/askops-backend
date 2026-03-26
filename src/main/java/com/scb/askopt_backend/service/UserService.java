package com.scb.askopt_backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scb.askopt_backend.entity.SysUser;
import com.scb.askopt_backend.mapper.UserMapper;
import com.scb.askopt_backend.vo.UserWithRolesVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService extends ServiceImpl<UserMapper, SysUser> implements IService<SysUser> {

    @Autowired
    private UserMapper userMapper;

    /**
     * 查询用户列表（带角色）
     */
    public List<UserWithRolesVO> getUsersWithRoles() {
        return userMapper.selectUsersWithRoles();
    }

}
