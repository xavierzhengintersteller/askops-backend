package com.scb.askopt_backend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scb.askopt_backend.dto.admin.AssignPermissionToRoleDTO;
import com.scb.askopt_backend.dto.admin.AssignRoleDTO;
import com.scb.askopt_backend.dto.admin.AssignRoleGroupDTO;
import com.scb.askopt_backend.dto.admin.UserPageDTO;
import com.scb.askopt_backend.entity.SysRole;
import com.scb.askopt_backend.entity.SysUser;
import com.scb.askopt_backend.mapper.AdminMapper;
import com.scb.askopt_backend.mapper.RoleMapper;
import com.scb.askopt_backend.mapper.UserMapper;
import com.scb.askopt_backend.vo.GroupVO;
import com.scb.askopt_backend.vo.UserPageVO;
import com.scb.askopt_backend.vo.UserWithRolesVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService extends ServiceImpl<UserMapper, SysUser> {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final AdminMapper adminMapper;
    /**
     * 查询用户列表（带角色）
     */
    public List<UserWithRolesVO> getUsersWithRoles() {
        return adminMapper.selectUsersWithRoles();
    }
    // 分页查询用户
    public IPage<UserPageVO> pageUser(UserPageDTO dto) {
        Page<SysUser> page = new Page<>(dto.getPageNum(), dto.getPageSize());

        IPage<SysUser> userPage = lambdaQuery()
                .like(dto.getUsername() != null, SysUser::getUsername, dto.getUsername())
                .page(page);

        List<UserPageVO> records = userPage.getRecords().stream().map(user -> {
            Long userId = user.getId();

            UserPageVO vo = new UserPageVO();
            vo.setUserId(userId);
            vo.setUsername(user.getUsername());
            vo.setRoleIds(userMapper.selectRoleIdsByUserId(userId));
            vo.setGroups(userMapper.selectGroupsByUserId(userId));
            vo.setAgents(userMapper.selectAgentsByUserId(userId));

            return vo;
        }).toList();

        Page<UserPageVO> result = new Page<>();
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        result.setTotal(page.getTotal());
        result.setRecords(records);
        return result;
    }

    // 查询用户详情 + 角色IDS + 组 + AGENT（全部统一）
    public UserPageVO getUserDetail(Long userId) {
        SysUser user = getById(userId);

        UserPageVO vo = new UserPageVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRoleIds(userMapper.selectRoleIdsByUserId(userId));
        vo.setGroups(userMapper.selectGroupsByUserId(userId));
        vo.setAgents(userMapper.selectAgentsByUserId(userId));

        return vo;
    }
    // 角色列表
    public List<SysRole> listAllRoles() {
        return roleMapper.selectList(null);
    }

    // 分配角色（事务）
    @Transactional(rollbackFor = Exception.class)
    public void assignRolesToUser(AssignRoleDTO dto) {
        Long userId = dto.getUserId();
        List<Long> roleIds = dto.getRoleIds();

        // 先删旧角色
        userMapper.deleteUserRoles(userId);

        // 再插入新角色
        if (roleIds != null && !roleIds.isEmpty()) {
            userMapper.insertUserRoles(userId, roleIds);
        }
    }
    // ==================== 角色 ↔ 组 关联管理 ====================

    /**
     * 根据角色ID查组列表
     */
    public List<GroupVO> getGroupsByRoleId(Long roleId) {
        return roleMapper.selectGroupsByRoleId(roleId);
    }

    /**
     * 获取角色已分配的组ID
     */
    public List<Long> getGroupIdsByRoleId(Long roleId) {
        return roleMapper.selectGroupIdsByRoleId(roleId);
    }

    /**
     * 给角色分配组（事务 + 先删后插）
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignGroupsToRole(AssignRoleGroupDTO dto) {
        Long roleId = dto.getRoleId();
        List<Long> groupIds = dto.getGroupIds();

        // 1. 删除旧关系
        roleMapper.deleteRoleGroups(roleId);

        // 2. 批量插入新关系
        if (groupIds != null && !groupIds.isEmpty()) {
            roleMapper.batchInsertRoleGroups(roleId, groupIds);
        }
    }

    /**
     * 给角色分配权限（事务 + 先删后插）
     * @param dto
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissionsToRole(AssignPermissionToRoleDTO dto) {
        Long roleId = dto.getRoleId();
        List<Long> permissionIds = dto.getPermissionIds();

        // 1. 删除旧关系
        roleMapper.deleteRolePermissions(roleId);

        // 2. 批量插入新关系
        if (permissionIds != null && !permissionIds.isEmpty()) {
            roleMapper.batchInsertRolePermissions(roleId, permissionIds);
        }
    }
    /**
     * 清空角色组
     */
    @Transactional(rollbackFor = Exception.class)
    public void clearRoleGroups(Long roleId) {
        roleMapper.deleteRoleGroups(roleId);
    }


}
