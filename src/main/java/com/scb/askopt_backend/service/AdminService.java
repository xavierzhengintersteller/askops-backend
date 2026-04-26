package com.scb.askopt_backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scb.askopt_backend.dto.AddRoleDTO;
import com.scb.askopt_backend.dto.admin.*;
import com.scb.askopt_backend.entity.*;
import com.scb.askopt_backend.exception.ApiException;
import com.scb.askopt_backend.exception.GlobalExceptionHandler;
import com.scb.askopt_backend.exception.ResultCodeEnum;
import com.scb.askopt_backend.mapper.*;
import com.scb.askopt_backend.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
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
    private TGroupMapper tgroupMapper;
    @Autowired
    private PermissionMapper permissionMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserRoleMappingMapper userRoleMappingMapper;
    @Autowired
    private RolePermissionMappingMapper rolePermissionMappingMapper;
    @Autowired
    private RoleGroupMappingMapper roleGroupMappingMapper;


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
     * 根据角色ID查询组列表
     * 如果 roleId = null，返回所有组
     */
    public List<GroupVO> getGroupsByRoleId(Long roleId) {
        if (roleId == null) {
            // 返回【所有组】
            return tgroupMapper.selectList(null).stream().map(g -> {
                GroupVO vo = new GroupVO();
                vo.setGroupId(g.getId());
                vo.setGroupName(g.getGroupName());
                return vo;
            }).collect(Collectors.toList());
        }

        // 原有逻辑：根据角色ID查已分配组
        return roleMapper.selectGroupsByRoleId(roleId);
    }

    /**
     * 获取角色已分配的组ID
     */
    public List<Long> getGroupIdsByRoleId(Long roleId) {
        return roleMapper.selectGroupIdsByRoleId(roleId).stream()
                .map(o -> Long.valueOf(o.toString()))
                .collect(Collectors.toList());
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

    /**
     * 根据角色ID查询该角色已分配的所有权限ID列表
     * MyBatis-Plus 纯写法，无XML，用于前端权限树回显勾选
     *
     * @param roleId 角色ID
     * @return 权限ID集合 List<Long>
     */
    public List<Long> getPermissionIdsByRoleId(Long roleId) {
        // 1. 构建查询条件
        LambdaQueryWrapper<RolePermissionMapping> wrapper = new LambdaQueryWrapper<>();

        // 2. 条件：只查询当前角色的权限关联记录
        wrapper.eq(RolePermissionMapping::getRoleId, roleId);

        // 3. 只查询 permission_id 这一个字段，提高查询效率
        wrapper.select(RolePermissionMapping::getPermissionId);

        // 4. 调用MP的selectObjs，只返回查询字段的对象列表
        // 5. 流式处理：将Object强转为Long，并收集为List返回
        return rolePermissionMappingMapper.selectObjs(wrapper).stream()
                .map(o -> Long.valueOf(o.toString()))
                .collect(Collectors.toList());
    }

    /**
     * 删除角色（业务层）
     * @param roleId 角色主键ID
     */
    public void deleteRoleById(Long roleId) {
        // 1. 查询角色信息，禁止删除超级管理员角色
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new ApiException(ResultCodeEnum.Role_NOTEXIST.getCode(),
                    ResultCodeEnum.Role_NOTEXIST.getMessage());
        }
        if ("admin".equals(role.getRoleCode())) {
            throw new ApiException(ResultCodeEnum.NOT_ALLOW_CHANGE_ADMIN_STATUS.getCode(),
                    ResultCodeEnum.NOT_ALLOW_CHANGE_ADMIN_STATUS.getMessage());
        }

        // ========== 2. 级联删除关联中间表数据 ==========
        // 2. 删除【用户-角色】关联（你之前漏了这个！）
        LambdaQueryWrapper<UserRoleMapping> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(UserRoleMapping::getRoleId, roleId);
        userRoleMappingMapper.delete(userWrapper);
        // 2.1 删除 角色-权限 关联
        LambdaQueryWrapper<RolePermissionMapping> permWrapper = new LambdaQueryWrapper<>();
        permWrapper.eq(RolePermissionMapping::getRoleId, roleId);
        rolePermissionMappingMapper.delete(permWrapper);

        // 2.2 删除 角色-组 关联
        LambdaQueryWrapper<RoleGroupMapping> groupWrapper = new LambdaQueryWrapper<>();
        groupWrapper.eq(RoleGroupMapping::getRoleId, roleId);
        roleGroupMappingMapper.delete(groupWrapper);

        // ========== 3. 删除角色主表数据 ==========
        roleMapper.deleteById(roleId);
    }

    /**
     * 获取所有角色的完整详情（角色信息 + 权限名称 + 组名称）
     * 用于前端角色管理列表页面展示
     *
     * @return 角色详情列表
     */
     //
    public List<RoleDetailVO> getAllRoleDetailList() {
        // 1. 查询所有角色
        List<SysRole> roleList = roleMapper.selectList(null);

        // 2. 逐个封装 VO
        return roleList.stream().map(role -> {
            RoleDetailVO vo = new RoleDetailVO();
            vo.setId(role.getId());
            vo.setRoleName(role.getRoleName());
            vo.setRoleCode(role.getRoleCode());

            // ========== 封装 权限名称列表（修复空集合问题） ==========
            List<Long> permissionIds = getPermissionIdsByRoleId(role.getId());
            List<String> permissionNames;

            if (permissionIds == null || permissionIds.isEmpty()) {
                permissionNames = Collections.emptyList();
            } else {
                permissionNames = permissionMapper.selectObjs(
                        new LambdaQueryWrapper<SysPermission>()
                                .in(SysPermission::getId, permissionIds)
                                .ne(SysPermission::getType, "API")
                                .select(SysPermission::getPermissionName)
                ).stream().map(Object::toString).toList();
            }
            vo.setPermissionNames(permissionNames);

            // ========== 封装 组名称列表（修复空集合问题） ==========
            List<Long> groupIds = getGroupIdsByRoleId(role.getId());
            List<String> groupNames;

            if (groupIds == null || groupIds.isEmpty()) {
                groupNames = Collections.emptyList();
            } else {
                groupNames = tgroupMapper.selectObjs(
                        new LambdaQueryWrapper<TGroup>()
                                .in(TGroup::getId, groupIds)
                                .select(TGroup::getGroupName)
                ).stream().map(Object::toString).toList();
            }
            vo.setGroupNames(groupNames);

            return vo;
        }).toList();
    }
    /**
     * 新增角色
     * 校验：角色编码唯一、角色名称唯一
     */
    @Transactional(rollbackFor = Exception.class)
    public void addRole(AddRoleDTO dto) {
        // 1. 校验角色编码是否重复
        Long countCode = roleMapper.selectCount(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getRoleCode, dto.getRoleCode())
        );
        if (countCode > 0) {
            throw new ApiException(ResultCodeEnum.VALUE_ALREADY_EXIST.getCode(), ResultCodeEnum.VALUE_ALREADY_EXIST.getMessage());
        }

        // 2. 校验角色名称是否重复
        Long countName = roleMapper.selectCount(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getRoleName, dto.getRoleName())
        );
        if (countName > 0) {
            throw new ApiException(ResultCodeEnum.VALUE_ALREADY_EXIST.getCode(), ResultCodeEnum.VALUE_ALREADY_EXIST.getMessage());
        }

        // 3. 插入新角色
        SysRole role = new SysRole();
        role.setRoleName(dto.getRoleName());
        role.setRoleCode(dto.getRoleCode());
        roleMapper.insert(role);
    }
}
