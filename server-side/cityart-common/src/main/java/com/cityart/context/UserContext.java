package com.cityart.context;

/**
 * ThreadLocal 持有当前登录用户信息
 * JwtInterceptor preHandle 设置，afterCompletion 清除
 */
public class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_ROLE = new ThreadLocal<>();

    public static void setUserId(Long id) {
        USER_ID.set(id);
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static void setUserRole(String role) {
        USER_ROLE.set(role);
    }

    public static String getUserRole() {
        return USER_ROLE.get();
    }

    /** 清除 — 必须在 afterCompletion 中调用，防止线程池复用导致串号 */
    public static void clear() {
        USER_ID.remove();
        USER_ROLE.remove();
    }
}
