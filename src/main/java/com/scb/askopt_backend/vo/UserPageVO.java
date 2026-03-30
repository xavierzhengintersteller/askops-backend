package com.scb.askopt_backend.vo;

import lombok.Data;

import java.util.List;

@Data
public class UserPageVO {
    private Long userId;
    private String username;
    private List<Long> roleIds;       // 角色ID
    // 可访问组
    private List<GroupVO> groups;
    // 可访问Agent（含IP）
    private List<AgentVO> agents;
}