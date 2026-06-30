package com.neusoft.cloud_brain_diagnosis.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign 拦截器 — 调用 AI 微服务时传递 JWT Token 和用户上下文
 */
@Configuration
public class FeignAuthConfig {

    @Bean
    public RequestInterceptor feignAuthInterceptor() {
        return (RequestTemplate template) -> {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return;

            HttpServletRequest request = attrs.getRequest();

            // 传递 Authorization token
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null) {
                template.header("Authorization", authHeader);
            }

            // 传递兼容的 token 头
            String token = request.getHeader("token");
            if (token != null) {
                template.header("token", token);
            }

            // 传递用户上下文（供 AI 微服务直接使用）
            String userId = request.getHeader("X-User-Id");
            String role = request.getHeader("X-User-Role");
            if (userId != null) template.header("X-User-Id", userId);
            if (role != null) template.header("X-User-Role", role);
        };
    }
}
