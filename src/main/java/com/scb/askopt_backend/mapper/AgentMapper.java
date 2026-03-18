package com.scb.askopt_backend.mapper;

import com.scb.askopt_backend.dto.AgentIpPortDTO;
import com.scb.askopt_backend.entity.Agent;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Agent 数据访问层（纯 MyBatis 实现）
 */
@org.apache.ibatis.annotations.Mapper // 保留 MyBatis 的 @Mapper 注解
public interface AgentMapper {
    /**
     * find ip,port by userId
     * @param userId
     * @return
     */
    @Select("""
            select a.ip, a.port
            from t_agent a
            join t_group g on a.group_id = g.id
            join role_group_mapping rg on rg.group_id = g.id
            join user_role_mapping ur on ur.role_id = rg.role_id
            where ur.user_id = #{userId}
            """)
    List<AgentIpPortDTO> findAgentsByUserId(Long userId);

    /**
     * 查询所有 agent
     */
    @Select("""
        SELECT id,
               name,
               ip,
               port,
               status,
               last_heartbeat_time
        FROM askops_schema.t_agent
    """)
    List<Agent> findAllAgents();


    /**
     * 更新 agent 状态
     */
    @Update("""
        UPDATE askops_schema.t_agent
        SET status = #{status},
            last_heartbeat_time = now(),
            update_time = now(),
            failcount = #{failCount}
        WHERE id = #{id}
    """)
    void updateStatus(@Param("id") Long id,
                      @Param("status") String status,
                      @Param("failCount") Integer failCount);


    /**
     * 插入Agent数据（自动生成主键ID）
     * @param agent 待插入的Agent对象
     */
    @Insert("INSERT INTO t_agent (name, ip, port, group_id, status, last_heartbeat_time) " +
            "VALUES (#{name}, #{ip}, #{port}, #{groupId}, #{status}, #{lastHeartbeatTime})")
    // 关键：开启主键自增返回，将数据库生成的ID回填到agent对象的id字段
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void insertAgent(Agent agent);

    // 原子递增失败次数
    @Update("UPDATE t_agent SET failcount = failcount + 1 WHERE id = #{agentId}")
    void incrementFailCount(@Param("agentId") Long agentId);

    // 关键修复：查询单条Agent的失败次数
    @Select("SELECT failcount FROM t_agent WHERE id = #{agentId}")
    Integer getFailCountById(@Param("agentId") Long agentId);

    // 批量更新状态、失败次数、心跳时间（合并操作，减少数据库交互）
    @Update("UPDATE t_agent SET status = #{status}, failcount = #{failCount}, last_heartbeat_time = #{heartbeatTime} WHERE id = #{agentId}")
    void updateAgentStatusAndFailCount(
            @Param("agentId") Long agentId,
            @Param("status") String status,
            @Param("failCount") Integer failCount,
            @Param("heartbeatTime") LocalDateTime heartbeatTime);

    // 仅更新心跳时间
    @Update("UPDATE t_agent SET last_heartbeat_time = #{heartbeatTime} WHERE id = #{agentId}")
    void updateHeartbeatTime(
            @Param("agentId") Long agentId,
            @Param("heartbeatTime") LocalDateTime heartbeatTime);
}