package com.neusoft.ai.common.context;

/**
 * 用户上下文 — 从请求头中注入用户信息
 * 主服务通过 Feign 拦截器将 X-User-Id / X-User-Role 传入
 */
public class UserContext {

    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE_HOLDER = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }

    public static void setRole(String role) {
        ROLE_HOLDER.set(role);
    }

    public static String getRole() {
        return ROLE_HOLDER.get();
    }

    public static void clear() {
        USER_ID_HOLDER.remove();
        ROLE_HOLDER.remove();
    }
}
