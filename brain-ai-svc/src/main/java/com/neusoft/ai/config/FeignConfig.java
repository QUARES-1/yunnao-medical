package com.neusoft.ai.config;

import com.neusoft.ai.common.context.UserContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign 配置 — 调用主服务时传递用户上下文
 */
@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor userContextInterceptor() {
        return (RequestTemplate template) -> {
            // 传递用户上下文到主服务
            if (UserContext.getUserId() != null) {
                template.header("X-User-Id", String.valueOf(UserContext.getUserId()));
                template.header("X-User-Role", UserContext.getRole());
            }

            // 尝试透传原始请求的 token
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null) {
                    template.header("Authorization", authHeader);
                }
            }
        };
    }
}
