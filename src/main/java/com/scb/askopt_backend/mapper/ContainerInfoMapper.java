package com.scb.askopt_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scb.askopt_backend.entity.ContainerInfoPO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import java.util.List;

public interface ContainerInfoMapper extends BaseMapper<ContainerInfoPO> {

    /** 批量标记指定节点所有容器为已删除（同步前清理旧数据） */
    @Update("UPDATE container_info SET is_deleted = true WHERE node_ip = #{nodeIp}")
    int markNodeContainersDeleted(@Param("nodeIp") String nodeIp);

    /** 批量插入容器快照（PG兼容） */
    int batchInsert(@Param("list") List<ContainerInfoPO> list);

    /** 分页查询用户有权限的未删除容器 */
    default IPage<ContainerInfoPO> selectContainerPage(Page<ContainerInfoPO> page,
                                                       @Param("ipList") List<String> allowNodeIps) {
        return selectPage(page, com.baomidou.mybatisplus.core.toolkit.Wrappers.lambdaQuery(ContainerInfoPO.class)
                .in(ContainerInfoPO::getNodeIp, allowNodeIps)
                .eq(ContainerInfoPO::getIsDeleted, false)
                .orderByAsc(ContainerInfoPO::getNodeIp, ContainerInfoPO::getContainerName));
    }
}