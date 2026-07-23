package com.cityart.context;

/**
 * ThreadLocal 持有当前登录管理员信息
 * JwtInterceptor preHandle 设置，afterCompletion 清除
 */
public class BuyerContext {

    private static final ThreadLocal<Long> BUYER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> BUYER_ROLE = new ThreadLocal<>();

    public static void setBuyerId(Long id) {
        BUYER_ID.set(id);
    }

    public static Long getBuyerId() {
        return BUYER_ID.get();
    }

    public static void setBuyerRole(String role) {
        BUYER_ROLE.set(role);
    }

    public static String getBuyerRole() {
        return BUYER_ROLE.get();
    }

    /** 清除 — 必须在 afterCompletion 中调用，防止线程池复用导致串号 */
    public static void clear() {
        BUYER_ID.remove();
        BUYER_ROLE.remove();
    }
}