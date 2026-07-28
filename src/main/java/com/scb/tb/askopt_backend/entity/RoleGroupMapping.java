package com.scb.tb.askopt_backend.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("role_group_mapping")
public class RoleGroupMapping {

    private Long roleId;

    private Long groupId;
}
