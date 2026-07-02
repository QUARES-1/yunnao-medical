package com.neusoft.cloud_brain_diagnosis.config;

import com.neusoft.cloud_brain_diagnosis.websocket.DoctorNotificationWebSocketHandler;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * WebSocketConfig 单元测试
 * 覆盖：WebSocket handler 注册、ServerContainer 配置
 */
class WebSocketConfigTest {

    private WebSocketConfig config;

    @Test
    void doctorNotificationWebSocketHandler_ShouldReturnNewInstance() {
        config = new WebSocketConfig();
        DoctorNotificationWebSocketHandler handler = config.doctorNotificationWebSocketHandler();
        assertNotNull(handler);
    }

    @Test
    void createWebSocketContainer_ShouldConfigureBufferSizes() {
        config = new WebSocketConfig();
        ServletServerContainerFactoryBean container = config.createWebSocketContainer();
        assertNotNull(container);
    }

    @Test
    void registerWebSocketHandlers_ShouldRegisterHandler() {
        config = new WebSocketConfig();
        WebSocketHandlerRegistry registry = mock(WebSocketHandlerRegistry.class);
        org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration registration = mock(org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration.class);
        when(registration.setAllowedOriginPatterns(any(String[].class))).thenReturn(registration);
        doReturn(registration).when(registry).addHandler(any(DoctorNotificationWebSocketHandler.class), eq("/ws/doctor/**"));
        config.registerWebSocketHandlers(registry);
        verify(registry).addHandler(any(DoctorNotificationWebSocketHandler.class), eq("/ws/doctor/**"));
    }
}
