package com.scb.askopt_backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scb.askopt_backend.dto.admin.*;
import com.scb.askopt_backend.entity.SysPermission;
import com.scb.askopt_backend.entity.SysRole;
import com.scb.askopt_backend.entity.SysUser;
import com.scb.askopt_backend.entity.UserRoleMapping;
import com.scb.askopt_backend.exception.ApiException;
import com.scb.askopt_backend.exception.GlobalExceptionHandler;
import com.scb.askopt_backend.exception.ResultCodeEnum;
import com.scb.askopt_backend.mapper.*;
import com.scb.askopt_backend.vo.GroupVO;
import com.scb.askopt_backend.vo.PermissionTreeVO;
import com.scb.askopt_backend.vo.UserPageVO;
import com.scb.askopt_backend.vo.UserWithRolesVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService extends ServiceImpl<UserMapper, SysUser> {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private AdminMapper adminMapper;
    @Autowired
    private PermissionMapper permissionMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserRoleMappingMapper userRoleMappingMapper;

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
            vo.setRoleNames(userMapper.selectRoleNamesByUserId(userId));
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
    // 全量用户列表（无分页、无筛选、无DTO）
    public List<UserPageVO> userList() {
        // 查所有用户
        List<SysUser> userList = lambdaQuery().list();

        // 组装VO
        return userList.stream().map(user -> {
            Long userId = user.getId();

            UserPageVO vo = new UserPageVO();
            vo.setUserId(userId);
            vo.setUsername(user.getUsername());
            vo.setEnabled(user.getEnabled());
            vo.setRoleNames(userMapper.selectRoleNamesByUserId(userId));
            vo.setGroups(userMapper.selectGroupsByUserId(userId));
            vo.setAgents(userMapper.selectAgentsByUserId(userId));

            return vo;
        }).toList();
    }

    // 查询用户详情 + 角色IDS + 组 + AGENT（全部统一）
    public UserPageVO getUserDetail(Long userId) {
        SysUser user = getById(userId);

        UserPageVO vo = new UserPageVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRoleNames(userMapper.selectRoleNamesByUserId(userId));
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
    // ==================== 权限树（核心修复） ====================
    public List<PermissionTreeVO> getPermissionTree() {
        // 从 PermissionMapper 查询所有权限
        List<SysPermission> allPermissions = permissionMapper.selectList(null);

        List<PermissionTreeVO> allVos = allPermissions.stream().map(p -> {
            PermissionTreeVO vo = new PermissionTreeVO();
            BeanUtils.copyProperties(p, vo);
            return vo;
        }).collect(Collectors.toList());

        return allVos.stream()
                .filter(vo -> vo.getParentId() == null || vo.getParentId() == 0)
                .peek(vo -> vo.setChildren(buildChildren(vo, allVos)))
                .collect(Collectors.toList());
    }

    private List<PermissionTreeVO> buildChildren(PermissionTreeVO parent, List<PermissionTreeVO> all) {
        return all.stream()
                .filter(vo -> parent.getId().equals(vo.getParentId()))
                .peek(vo -> vo.setChildren(buildChildren(vo, all)))
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public void addUser(AddUserDTO dto) {
        // 1. 新建用户
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword())); // 正式项目记得加密 BCrypt
        user.setEnabled(true);
        save(user); // 保存后 user.getId() 才有值

        // 2. 分配角色（你已有的方法复用）
        AssignRoleDTO assignRoleDTO = new AssignRoleDTO();
        assignRoleDTO.setUserId(user.getId());
        assignRoleDTO.setRoleIds(dto.getRoleIds());
        assignRolesToUser(assignRoleDTO); // 复用你原来的分配逻辑
    }

    @Transactional
    public void updateUserStatus(BlacklistUserDTO dto) {
        SysUser user = getById(dto.getUserId());

        // admin 直接不执行，不抛异常
        if (user != null && "admin".equals(user.getUsername())) {
            return;
        }

        lambdaUpdate()
                .eq(SysUser::getId, dto.getUserId())
                .set(SysUser::getEnabled, dto.getEnabled())
                .update();
    }

    @Transactional
    public void deleteUser(Long userId) {
        SysUser user = getById(userId);

        // admin 不允许删，直接 return
        if (user != null && "admin".equals(user.getUsername())) {
            return;
        }

        userRoleMappingMapper.delete(
                new LambdaQueryWrapper<UserRoleMapping>()
                        .eq(UserRoleMapping::getUserId, userId)
        );
        this.removeById(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(UpdateUserPwdDTO dto) {
        SysUser user = getById(dto.getUserId());

        // admin 不允许改密，直接 return
        if (user != null && "admin".equals(user.getUsername())) {
            return;
        }

        lambdaUpdate()
                .eq(SysUser::getId, dto.getUserId())
                .set(SysUser::getPassword, passwordEncoder.encode(dto.getNewPassword()))
                .update();
    }
}
