package com.scb.askopt_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scb.askopt_backend.dto.AgentIpPortDTO;
import com.scb.askopt_backend.entity.Agent;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Agent 数据访问层（纯 MyBatis 实现）
 */

public interface AgentMapper extends BaseMapper<Agent> {
    /**
     * find ip,port by userId
     * @param userId
     * @return
     */
    List<AgentIpPortDTO> findAgentsByUserId(Long userId);

    /**
     * 查询所有 agent
     */
    List<Agent> findAllAgents();


    /**
     * 更新 agent 状态
     */
    void updateStatus(@Param("id") Long id,
                      @Param("status") String status,
                      @Param("failCount") Integer failCount);


    /**
     * 插入Agent数据（自动生成主键ID）
     * @param agent 待插入的Agent对象
     */
    void insertAgent(Agent agent);

    // 原子递增失败次数
    void incrementFailCount(@Param("agentId") Long agentId);

    // 关键修复：查询单条Agent的失败次数
    Integer getFailCountById(@Param("agentId") Long agentId);

    // 批量更新状态、失败次数、心跳时间（合并操作，减少数据库交互）
    void updateAgentStatusAndFailCount(
            @Param("agentId") Long agentId,
            @Param("status") String status,
            @Param("failCount") Integer failCount,
            @Param("heartbeatTime") LocalDateTime heartbeatTime);

    // 仅更新心跳时间
    void updateHeartbeatTime(
            @Param("agentId") Long agentId,
            @Param("heartbeatTime") LocalDateTime heartbeatTime);
}