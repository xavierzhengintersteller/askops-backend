package com.scb.tb.askopt_backend.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("role_permission_mapping")
public class RolePermissionMapping {

    private Long roleId;

    private Long permissionId;
}
