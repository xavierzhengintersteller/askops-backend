添加agent注册，探测接口
添加权限管理接口
修改jwtfilter，增加权限验证（agent，和权限管理）
优化superuser结构

## 2026-3-24 ##
用户、角色、权限、分组的基础 CRUD；

角色 - 权限、用户 - 角色、角色 - 分组的关联关系 CRUD；

权限树查询、用户权限穿透查询等核心场景；

兼容现有表结构，无需修改已有数据。
---
结合你现在 **AskOps 后台 RBAC 权限架构**，一套标准、优雅、企业级的 **管理员（用户）CRUD 接口**，最少且必备的接口如下。我直接给你**接口清单 + 用途 + 建议 URL**，你照着开发就行，不多不少、刚好够用。

# 一、用户（Admin/User）标准 CRUD 接口
## 1. 分页查询用户列表
- `GET /api/admin/user/page`
- 参数：pageNum、pageSize、username、nickname、status、roleId
- 返回：分页列表 + 每个用户的角色信息

## 2. 根据 ID 查询单个用户详情
- `GET /api/admin/user/{id}`
- 用途：编辑回显、查看详情

## 3. 新增用户
- `POST /api/admin/user`
- 注意：密码加密存储
- 可同时绑定角色

## 4. 修改用户（基本信息）
- `PUT /api/admin/user`
- 不建议改密码，单独接口

## 5. 删除用户（单/批量）
- `DELETE /api/admin/user`
- 参数：ids → 批量删除
- 超管不可删自己

## 6. 修改用户状态（启用/禁用）
- `PUT /api/admin/user/change-status`
- 参数：id、status

## 7. 重置用户密码
- `PUT /api/admin/user/reset-pwd`
- 管理员给别人重置密码

## 8. 修改当前登录用户密码
- `PUT /api/user/update-pwd`
- 自己改自己密码，校验旧密码

---

# 二、角色相关（必带，否则用户没法管理）
## 9. 角色列表（不分页）
- `GET /api/admin/role/list`
- 用于用户新增/编辑时下拉选择角色

## 10. 给用户分配角色
- `PUT /api/admin/user/assign-role`
- 参数：userId、roleIds

---

# 三、权限/菜单相关（你已有，这里列出来保持完整）
## 11. 获取当前用户菜单树
- `GET /api/user/permission-menu`

## 12. 获取所有权限/菜单列表
- `GET /api/admin/permission/list`
- 超管专用

---

# 四、接口权限控制建议（超级优雅）
- 列表、详情：**普通管理员可看**
- 新增、编辑、删除、分配角色：**必须超管**
- 个人修改密码：**自己可操作**

判断方式直接用你现在的：
```java
if (AuthContext.isSuperAdmin()) {
    // 允许
}
```

---

# 五、完整接口清单（精简最终版）
```
/api/admin/user/page           // 分页查询
/api/admin/user/{id}           // 详情
/api/admin/user                // 新增
/api/admin/user                // 修改
/api/admin/user                // 删除
/api/admin/user/change-status  // 启停
/api/admin/user/reset-pwd      // 重置密码
/api/admin/user/assign-role    // 分配角色

/api/user/update-pwd           // 自己改密码
/api/admin/role/list           // 角色下拉

/api/user/permission-menu      // 个人菜单+权限
/api/admin/permission/list     // 全量权限（超管）
```

---

# 六、如果你需要，我可以一次性生成
- 全套 **Controller**
- 全套 **Service**
- 全套 **Mapper & XML**
- 全套 **VO/DTO**
- 全套 **接口权限注解（@RequireSuperAdmin）**

你只要说一声，我直接一次性全部给你可运行代码。