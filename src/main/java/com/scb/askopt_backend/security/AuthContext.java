package com.scb.askopt_backend.security;

public class AuthContext {

    private static final ThreadLocal<AuthUser> CONTEXT = new ThreadLocal<>();

    public static void set(AuthUser user) {
        CONTEXT.set(user);
    }

    public static AuthUser get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
