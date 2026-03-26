package com.scb.askopt_backend.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_group")
public class TGroup {

    private Long id;

    private String groupName;
}
