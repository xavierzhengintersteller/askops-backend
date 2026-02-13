package com.scb.askopt_backend.controller;

import com.scb.askopt_backend.dto.LoginRequest;
import com.scb.askopt_backend.entity.SysUser;
import com.scb.askopt_backend.mapper.PermissionMapper;
import com.scb.askopt_backend.mapper.UserMapper;
import com.scb.askopt_backend.security.AuthContext;
import com.scb.askopt_backend.security.AuthUser;
import com.scb.askopt_backend.security.JwtUtil;
import com.scb.askopt_backend.vo.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private PermissionMapper permissionMapper;

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody LoginRequest request){

        SysUser user = userMapper.findByUsername(request.getUsername());
        if(user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())){
            return ApiResponse.error(401,"用户名或密码错误");
        }

        // 生成 JWT
        String token = jwtUtil.generateToken(user.getUsername());

        // 加载权限（via roles）
        Set<String> permissions = permissionMapper.findCodesByUserId(user.getId());

        // load roles
        List<String> roles = userMapper.findRoleCodesByUserId(user.getId());

        // 构建 AuthUser（可以放入上下文或缓存）
        AuthUser authUser = new AuthUser();
        authUser.setUserId(user.getId());
        authUser.setUsername(user.getUsername());
        authUser.setPermissions(permissions);
        AuthContext.set(authUser); // 可选，供拦截器读取

        // 返回 token + 权限 + 角色
        Map<String,Object> result = new HashMap<>();
        result.put("token", token);
        result.put("permissions", permissions);
        result.put("roles", roles);

        return ApiResponse.success(result);
    }

    @PostMapping("/register")
    public ApiResponse<Void> register(@RequestBody LoginRequest request) {

        // 1. 参数基础校验
        if (request.getUsername() == null || request.getPassword() == null) {
            return ApiResponse.error(400, "用户名或密码不能为空");
        }

        // 2. 用户是否已存在
        SysUser existUser = userMapper.findByUsername(request.getUsername());
        if (existUser != null) {
            return ApiResponse.error(409, "用户名已存在");
        }

        // 3. 构建用户对象
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true);

        // 4. 保存
        userMapper.insert(user);

        // 5. assign default role "OTHER" (if exists)
        userMapper.assignRoleToUserByCode(user.getId(), "OTHER");

        return ApiResponse.success();
    }


}
