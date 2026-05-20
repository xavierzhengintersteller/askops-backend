package com.scb.askopt_backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scb.askopt_backend.config.RedisUtil;
import com.scb.askopt_backend.dto.AddRoleDTO;
import com.scb.askopt_backend.dto.admin.*;
import com.scb.askopt_backend.entity.*;
import com.scb.askopt_backend.exception.ApiException;
import com.scb.askopt_backend.exception.ResultCodeEnum;
import com.scb.askopt_backend.mapper.*;
import com.scb.askopt_backend.vo.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.scb.askopt_backend.constant.RedisConstants.*;

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
    @Autowired
    private AuthService authService;
    @Autowired
    private RedisUtil redisUtil;

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
        List<SysUser> userList = lambdaQuery().list();

        return userList.stream().map(user -> {
            Long userId = user.getId();

            UserPageVO vo = new UserPageVO();
            vo.setUserId(userId);
            vo.setUsername(user.getUsername());
            vo.setEnabled(user.getEnabled());
            vo.setRoleNames(userMapper.selectRoleNamesByUserId(userId));
            vo.setRoleIds(userMapper.selectRoleIdsByUserId(userId));
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
        vo.setRoleIds(userMapper.selectRoleIdsByUserId(userId));
        vo.setGroups(userMapper.selectGroupsByUserId(userId));
        vo.setAgents(userMapper.selectAgentsByUserId(userId));

        return vo;
    }

    // 角色列表
    public List<SysRole> listAllRoles() {
        return roleMapper.selectList(null);
    }

    // ==============================
    // 分配角色（已加权限刷新）
    // ==============================
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

        // 🔥 刷新权限
        refreshUserPermission(userId);
    }

    // ==================== 角色 ↔ 组 关联管理 ====================
    public List<GroupVO> getGroupsByRoleId(Long roleId) {
        if (roleId == null) {
            return tgroupMapper.selectList(null).stream().map(g -> {
                GroupVO vo = new GroupVO();
                vo.setGroupId(g.getId());
                vo.setGroupName(g.getGroupName());
                return vo;
            }).collect(Collectors.toList());
        }
        return roleMapper.selectGroupsByRoleId(roleId);
    }

    public List<Long> getGroupIdsByRoleId(Long roleId) {
        return roleMapper.selectGroupIdsByRoleId(roleId).stream()
                .map(o -> Long.valueOf(o.toString()))
                .collect(Collectors.toList());
    }

    // ==============================
    // 给角色分配组（已加权限刷新）
    // ==============================
    @Transactional(rollbackFor = Exception.class)
    public void assignGroupsToRole(AssignRoleGroupDTO dto) {
        Long roleId = dto.getRoleId();
        List<Long> groupIds = dto.getGroupIds();

        roleMapper.deleteRoleGroups(roleId);

        if (groupIds != null && !groupIds.isEmpty()) {
            roleMapper.batchInsertRoleGroups(roleId, groupIds);
        }

        // 🔥 批量刷新
        refreshRoleUsersPermission(roleId);
    }

    // ==============================
    // 给角色分配权限（已加权限刷新）
    // ==============================
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissionsToRole(AssignPermissionToRoleDTO dto) {
        Long roleId = dto.getRoleId();
        List<Long> permissionIds = dto.getPermissionIds();

        LambdaQueryWrapper<RolePermissionMapping> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RolePermissionMapping::getRoleId, roleId);
        rolePermissionMappingMapper.delete(wrapper);

        if (permissionIds == null || permissionIds.isEmpty()) {
            return;
        }

        Set<Long> uniqueSet = new HashSet<>(permissionIds);
        for (Long permId : uniqueSet) {
            RolePermissionMapping mapping = new RolePermissionMapping();
            mapping.setRoleId(roleId);
            mapping.setPermissionId(permId);
            rolePermissionMappingMapper.insert(mapping);
        }

        // 🔥 批量刷新
        refreshRoleUsersPermission(roleId);
    }

    // ==============================
    // 清空角色组（已加权限刷新）
    // ==============================
    @Transactional(rollbackFor = Exception.class)
    public void clearRoleGroups(Long roleId) {
        roleMapper.deleteRoleGroups(roleId);
        // 🔥 批量刷新
        refreshRoleUsersPermission(roleId);
    }

    // ==================== 权限树 ====================
    public List<PermissionTreeVO> getPermissionTree() {
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

    // 新增用户
    @Transactional(rollbackFor = Exception.class)
    public void addUser(AddUserDTO dto) {
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEnabled(true);
        save(user);

        AssignRoleDTO assignRoleDTO = new AssignRoleDTO();
        assignRoleDTO.setUserId(user.getId());
        assignRoleDTO.setRoleIds(dto.getRoleIds());
        assignRolesToUser(assignRoleDTO);
    }

    // ==============================
    // 用户拉黑/解禁
    // ==============================
    @Transactional
    public void updateUserStatus(BlacklistUserDTO dto) {
        SysUser user = getById(dto.getUserId());

        if (user != null && "admin".equals(user.getUsername())) {
            return;
        }

        lambdaUpdate()
                .eq(SysUser::getId, dto.getUserId())
                .set(SysUser::getEnabled, dto.getEnabled())
                .update();

    }

    // ==============================
    // 删除用户（已加权限刷新）
    // ==============================
    @Transactional
    public void deleteUser(Long userId) {
        SysUser user = getById(userId);

        if (user != null && "admin".equals(user.getUsername())) {
            return;
        }

        userRoleMappingMapper.delete(
                new LambdaQueryWrapper<UserRoleMapping>()
                        .eq(UserRoleMapping::getUserId, userId)
        );
        this.removeById(userId);

        String keyVer = REDIS_PERMISSION_VERSION + userId;
        String keyPerm = REDIS_PERMISSION_LIST + userId;
        redisUtil.del(keyVer);
        redisUtil.del(keyPerm);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(UpdateUserPwdDTO dto) {
        SysUser user = getById(dto.getUserId());

        if (user != null && "admin".equals(user.getUsername())) {
            return;
        }

        lambdaUpdate()
                .eq(SysUser::getId, dto.getUserId())
                .set(SysUser::getPassword, passwordEncoder.encode(dto.getNewPassword()))
                .update();
    }

    public List<Long> getPermissionIdsByRoleId(Long roleId) {
        LambdaQueryWrapper<RolePermissionMapping> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RolePermissionMapping::getRoleId, roleId);
        wrapper.select(RolePermissionMapping::getPermissionId);

        return rolePermissionMappingMapper.selectObjs(wrapper).stream()
                .map(o -> Long.valueOf(o.toString()))
                .collect(Collectors.toList());
    }

    // ==============================
    // 删除角色（已加权限刷新）
    // ==============================
    @Transactional(rollbackFor = Exception.class)
    public void deleteRoleById(Long roleId) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new ApiException(ResultCodeEnum.Role_NOTEXIST.getCode(), ResultCodeEnum.Role_NOTEXIST.getMessage());
        }
        if ("admin".equals(role.getRoleCode())) {
            throw new ApiException(ResultCodeEnum.NOT_ALLOW_CHANGE_ADMIN_STATUS.getCode(), ResultCodeEnum.NOT_ALLOW_CHANGE_ADMIN_STATUS.getMessage());
        }

        // 先刷新再删除
        refreshRoleUsersPermission(roleId);

        LambdaQueryWrapper<UserRoleMapping> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(UserRoleMapping::getRoleId, roleId);
        userRoleMappingMapper.delete(userWrapper);

        LambdaQueryWrapper<RolePermissionMapping> permWrapper = new LambdaQueryWrapper<>();
        permWrapper.eq(RolePermissionMapping::getRoleId, roleId);
        rolePermissionMappingMapper.delete(permWrapper);

        LambdaQueryWrapper<RoleGroupMapping> groupWrapper = new LambdaQueryWrapper<>();
        groupWrapper.eq(RoleGroupMapping::getRoleId, roleId);
        roleGroupMappingMapper.delete(groupWrapper);

        roleMapper.deleteById(roleId);
    }

    public List<RoleDetailVO> getAllRoleDetailList() {
        List<SysRole> roleList = roleMapper.selectList(null);

        return roleList.stream().map(role -> {
            RoleDetailVO vo = new RoleDetailVO();
            vo.setId(role.getId());
            vo.setRoleName(role.getRoleName());
            vo.setRoleCode(role.getRoleCode());

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

    @Transactional(rollbackFor = Exception.class)
    public void addRole(AddRoleDTO dto) {
        Long countCode = roleMapper.selectCount(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getRoleCode, dto.getRoleCode())
        );
        if (countCode > 0) {
            throw new ApiException(ResultCodeEnum.VALUE_ALREADY_EXIST.getCode(), ResultCodeEnum.VALUE_ALREADY_EXIST.getMessage());
        }

        Long countName = roleMapper.selectCount(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getRoleName, dto.getRoleName())
        );
        if (countName > 0) {
            throw new ApiException(ResultCodeEnum.VALUE_ALREADY_EXIST.getCode(), ResultCodeEnum.VALUE_ALREADY_EXIST.getMessage());
        }

        SysRole role = new SysRole();
        role.setRoleName(dto.getRoleName());
        role.setRoleCode(dto.getRoleCode());
        roleMapper.insert(role);
    }

    // ==================== 权限刷新工具方法 ====================
    /**
     * 权限变更后强制刷新（Long 版本号 · 终极版）
     */
    public void refreshUserPermission(Long userId) {
        if (userId == null) return;

        String keyVer = REDIS_PERMISSION_VERSION + userId;
        String keyPerm = REDIS_PERMISSION_LIST + userId;

        // 🔥 唯一 Long 版本号：时间戳 + 随机数（绝对不重复）
        long newVersion = System.currentTimeMillis() + new Random().nextInt(1000);

        // 获取最新权限
        Set<Long> permissionIds = authService.getUserPermissionIds(userId);

        // 写入 Redis
        redisUtil.set(keyVer, newVersion, REFRESH_TOKEN_EXPIRE_SEC);
        redisUtil.set(keyPerm, permissionIds, REFRESH_TOKEN_EXPIRE_SEC);
    }

    /**
     * 批量刷新角色下所有用户权限
     */
    public void refreshRoleUsersPermission(Long roleId) {
        if (roleId == null) return;

        Set<Long> userIds = userRoleMappingMapper.selectObjs(
                        new LambdaQueryWrapper<UserRoleMapping>()
                                .eq(UserRoleMapping::getRoleId, roleId)
                                .select(UserRoleMapping::getUserId)
                ).stream()
                .map(o -> Long.valueOf(o.toString()))
                .collect(Collectors.toSet());

        for (Long userId : userIds) {
            refreshUserPermission(userId);
        }
    }

}