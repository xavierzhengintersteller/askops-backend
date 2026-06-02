package com.scb.askopt_backend.context;

import lombok.experimental.UtilityClass;

@UtilityClass
public class AuthContext {

    private final ThreadLocal<Long> USER_ID_CONTEXT = new ThreadLocal<>();
    private final ThreadLocal<Long> PERMISSION_VERSION = new ThreadLocal<>();
    private final ThreadLocal<Boolean> IS_SUPER_ADMIN = new ThreadLocal<>();

    // ====================== userId
    public void setUserId(Long userId) {
        USER_ID_CONTEXT.set(userId);
    }

    public Long getUserId() {
        return USER_ID_CONTEXT.get();
    }

    // ====================== permissionVersion
    public void setPermissionVersion(Long version) {
        PERMISSION_VERSION.set(version);
    }

    public Long getPermissionVersion() {
        return PERMISSION_VERSION.get();
    }

    // ====================== superAdmin
    public void setSuperAdmin(boolean superAdmin) {
        IS_SUPER_ADMIN.set(superAdmin);
    }

    public boolean isSuperAdmin() {
        return Boolean.TRUE.equals(IS_SUPER_ADMIN.get());
    }

    // ====================== clear
    public void clear() {
        USER_ID_CONTEXT.remove();
        PERMISSION_VERSION.remove();
        IS_SUPER_ADMIN.remove();
    }
}