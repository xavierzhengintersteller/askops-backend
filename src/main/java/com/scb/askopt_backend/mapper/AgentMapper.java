package com.scb.askopt_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scb.askopt_backend.dto.AgentIpPortDTO;
import com.scb.askopt_backend.entity.Agent;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Agent 数据访问层（适配PostgreSQL）
 */
@Mapper
public interface AgentMapper extends BaseMapper<Agent> {
    /**
     * find ip,port by userId
     * @param userId
     * @return
     */
    List<AgentIpPortDTO> findAgentsByUserId(Long userId);

    // 更新心跳时间+重置失败次数
    @Update("UPDATE t_agent SET last_heartbeat_time=#{now}, failcount=0, status='ONLINE', update_time=#{now} WHERE name=#{name}")
    int updateHeartbeat(@Param("name") String name, @Param("now") LocalDateTime now);

    // 扫描超时Agent标记离线，失败次数+1
    // PostgreSQL 替换MySQL TIMESTAMPDIFF，使用EXTRACT(EPOCH FROM 时间间隔) 获取总秒数
    @Update("UPDATE t_agent SET status='OFFLINE', failcount=failcount+1, update_time=#{now} " +
            "WHERE EXTRACT(EPOCH FROM (#{now} - last_heartbeat_time)) > heartbeat_timeout_sec AND status='ONLINE'")
    int markOfflineTimeoutAgent(@Param("now") LocalDateTime now);

    List<Agent> selectOnlineAgent();
}