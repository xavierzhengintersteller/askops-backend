package com.scb.askopt_backend.mapper;

import com.scb.askopt_backend.dto.AgentIpPortDTO;
import com.scb.askopt_backend.entity.Agent;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * Agent 数据访问层（纯 MyBatis 实现）
 */
@org.apache.ibatis.annotations.Mapper // 保留 MyBatis 的 @Mapper 注解
public interface AgentMapper {

    /**
     * 根据IP查询Agent（注册时校验是否已注册）
     */
    @Select("SELECT id, ip, port, group_id, name, status, last_heartbeat_time, create_time, update_time " +
            "FROM t_agent WHERE ip = #{ip}")
    Agent selectByIp(@Param("ip") String ip);
    /**
     * 根据AgentName查询Agent（注册时校验是否已注册）
     */
    @Select("SELECT id, ip, port, group_id, name, status, last_heartbeat_time, create_time, update_time " +
            "FROM t_agent WHERE name = #{name}")
    Agent selectByName(@Param("name") String name);
    /**
     * 新增Agent（注册核心方法）
     */
    @Insert("INSERT INTO t_agent (ip, port, group_id, name, status, last_heartbeat_time, create_time, update_time) " +
            "VALUES (#{ip}, #{port}, #{groupId}, #{name}, #{status}, #{lastHeartbeatTime}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertAgent(Agent agent);
    /**
     * 可选：更新Agent心跳时间（后续health接口用）
     */
    @Update("UPDATE t_agent SET status = #{status}, last_heartbeat_time = #{lastHeartbeatTime}, update_time = #{updateTime} " +
            "WHERE id = #{id}")
    int updateHeartbeat(Agent agent);

    /**
     * 可选：根据ID查询Agent（后续health/report接口用）
     */
    @Select("SELECT id, ip, port, group_id, name, status, last_heartbeat_time, create_time, update_time " +
            "FROM t_agent WHERE id = #{id}")
    Agent selectById(@Param("id") Long id);

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
    AgentIpPortDTO findAgentsByUserId(Long userId);
}