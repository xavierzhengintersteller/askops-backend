package com.scb.askopt_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_role")
public class SysRole {
    @TableId (type = IdType.AUTO)
    private Long id;
    @TableField("role_code")
    private String roleCode; // e.g. ADMIN, DEV, QA
    @TableField("role_name")
    private String roleName;

}
