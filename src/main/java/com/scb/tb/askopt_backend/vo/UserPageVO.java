package com.scb.tb.askopt_backend.vo;

import lombok.Data;

import java.util.List;

@Data
public class UserPageVO {
    private Long userId;
    private String username;
    private Boolean enabled;
    private List<String> roleNames;       // 角色ID
    private List<Long> roleIds;
    // 可访问组
    private List<GroupVO> groups;
    // 可访问Agent（含IP）
    private List<AgentVO> agents;
}