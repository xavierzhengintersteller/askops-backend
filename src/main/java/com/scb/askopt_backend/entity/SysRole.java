package com.scb.askopt_backend.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_role")
public class SysRole {
    private Long id;
    @TableField("role_code")
    private String roleCode; // e.g. ADMIN, DEV, QA
    @TableField("role_name")
    private String roleName;

}
