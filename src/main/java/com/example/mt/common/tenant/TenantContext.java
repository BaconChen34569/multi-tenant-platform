package com.example.mt.common.tenant;

/**
 * 租户上下文：基于 ThreadLocal 在当前请求线程内传递 tenantId。
 * 这是多租户隔离的“上下文透传”载体 —— 请求进来时写入，SQL 改写时读取，请求结束时清理。
 */
public class TenantContext {

    private static final ThreadLocal<Long> CONTEXT = new ThreadLocal<>();

    public static void set(Long tenantId) {
        CONTEXT.set(tenantId);
    }

    public static Long get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
