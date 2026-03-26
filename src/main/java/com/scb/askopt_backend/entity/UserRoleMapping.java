package com.scb.askopt_backend.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("user_role_mapping")
public class UserRoleMapping {

    private Long userId;

    private Long roleId;
}
