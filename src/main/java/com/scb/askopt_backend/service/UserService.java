package com.scb.askopt_backend.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scb.askopt_backend.entity.SysUser;
import com.scb.askopt_backend.mapper.UserMapper;
import com.scb.askopt_backend.security.AuthContext;
import com.scb.askopt_backend.vo.UserMenuVO;
import com.scb.askopt_backend.vo.UserPermissionAndMenuVO;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService extends ServiceImpl<UserMapper, SysUser> {

    /**
     * 合并接口：菜单树 + 权限
     */
    public UserPermissionAndMenuVO getPermissionAndMenu(Long userId) {
        UserPermissionAndMenuVO vo = new UserPermissionAndMenuVO();
        // ==============================================
        // 🔥 直接从 ThreadLocal 获取是否超级管理员
        // ==============================================
        boolean isSuperAdmin = AuthContext.isSuperAdmin();

        if (isSuperAdmin) {
            // ✅ 超管 → 直接返回全量菜单 + 全权限
            vo.setLeftMenuTree(buildMenuTree(baseMapper.selectAllMenuList(), null));
            vo.setPermissionIds(baseMapper.selectAllPermissionIds());
            vo.setPermissionCodes(baseMapper.selectAllPermissionCodes());
        } else {
            // ✅ 普通用户 → 查自己的权限
            List<UserMenuVO> menuList = baseMapper.selectUserMenuList(userId);
            vo.setLeftMenuTree(buildMenuTree(menuList, null));
            vo.setPermissionIds(baseMapper.selectUserPermissionIds(userId));
            vo.setPermissionCodes(baseMapper.selectUserPermissionCodes(userId));
        }

        return vo;
    }

    // ====================== 树形构建 ======================
    private List<UserMenuVO> buildMenuTree(List<UserMenuVO> list, Long parentId) {
        return list.stream()
                .filter(menu -> (parentId == null && menu.getParentId() == null)
                        || (parentId != null && parentId.equals(menu.getParentId())))
                .peek(menu -> menu.setChildren(buildMenuTree(list, menu.getId())))
                .collect(Collectors.toList());
    }

    // ====================== 权限版本号 ======================
    public void incrementPermissionVersion(Long userId) {
        baseMapper.incrementPermissionVersion(userId);
    }

    public Long getPermissionVersion(Long userId) {
        return baseMapper.getPermissionVersion(userId);
    }
}