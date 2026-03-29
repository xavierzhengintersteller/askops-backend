package com.scb.askopt_backend.controller;

import com.scb.askopt_backend.security.AuthContext;
import com.scb.askopt_backend.service.UserService;
import com.scb.askopt_backend.vo.ApiResponse;
import com.scb.askopt_backend.vo.UserPermissionAndMenuVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 【合并接口】一次获取：左侧菜单树 + 权限ID + 权限码
     */
    @GetMapping("/permission-menu")
    public ApiResponse<UserPermissionAndMenuVO> getPermissionAndMenu() {
        Long userId = AuthContext.getUserId();
        UserPermissionAndMenuVO result = userService.getPermissionAndMenu(userId);
        return ApiResponse.success(result);
    }
}