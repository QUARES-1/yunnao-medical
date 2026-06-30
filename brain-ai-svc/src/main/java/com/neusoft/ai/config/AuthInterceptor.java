package com.neusoft.ai.config;

import com.neusoft.ai.common.context.UserContext;
import com.neusoft.ai.common.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 优先从请求头读取主服务传入的用户上下文
        String userIdHeader = request.getHeader("X-User-Id");
        String roleHeader = request.getHeader("X-User-Role");

        if (userIdHeader != null && roleHeader != null) {
            // 主服务 Feign 调用时透传的用户上下文
            UserContext.setUserId(Long.valueOf(userIdHeader));
            UserContext.setRole(roleHeader);
        } else {
            // 直接从 JWT token 解析（前端直接调用时）
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            } else if (token == null) {
                token = request.getHeader("token");
            }
            if (token != null) {
                Long userId = jwtUtil.getUserId(token);
                String role = jwtUtil.getRole(token);
                if (userId != null) UserContext.setUserId(userId);
                if (role != null) UserContext.setRole(role);
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
