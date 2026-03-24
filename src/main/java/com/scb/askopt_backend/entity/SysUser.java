package com.scb.askopt_backend.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_user")
public class SysUser {
    private Long id;
    private String username;
    private String password;
    private Boolean enabled;
    @TableField("permission_version")
    private Long permissionVersion;
}
