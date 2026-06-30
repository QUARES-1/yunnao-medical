package com.neusoft.cloud_brain_diagnosis.config;

import com.neusoft.cloud_brain_diagnosis.websocket.DoctorNotificationWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(doctorNotificationWebSocketHandler(), "/ws/doctor/**")
                .setAllowedOriginPatterns("*");
    }

    @Bean
    public DoctorNotificationWebSocketHandler doctorNotificationWebSocketHandler() {
        return new DoctorNotificationWebSocketHandler();
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(65536);
        container.setMaxBinaryMessageBufferSize(65536);
        container.setMaxSessionIdleTimeout(600000L); // 10分钟无活动断开
        return container;
    }
}
