package com.neusoft.cloud_brain_diagnosis.service;

import com.neusoft.cloud_brain_diagnosis.websocket.DoctorNotificationWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * NotificationService 单元测试 - WebSocket 推送通知
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private DoctorNotificationWebSocketHandler webSocketHandler;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(webSocketHandler);
    }

    // ========== notifyHighRiskMedication() ==========

    @Test
    void notifyHighRiskMedication_ShouldSend_WhenDoctorOnline() {
        when(webSocketHandler.sendToDoctor(eq(10L), anyString())).thenReturn(true);

        notificationService.notifyHighRiskMedication(10L, 1L,
                List.of(Map.of("level", "high", "content", "剂量过大")),
                "降低剂量");

        verify(webSocketHandler).sendToDoctor(eq(10L), contains("HIGH_RISK_MEDICATION"));
    }

    @Test
    void notifyHighRiskMedication_ShouldNotThrow_WhenDoctorOffline() {
        when(webSocketHandler.sendToDoctor(eq(99L), anyString())).thenReturn(false);

        // Should not throw, just log debug
        notificationService.notifyHighRiskMedication(99L, 1L,
                List.of(Map.of("level", "high", "content", "风险")),
                "建议");

        verify(webSocketHandler).sendToDoctor(eq(99L), contains("HIGH_RISK_MEDICATION"));
    }

    @Test
    void notifyHighRiskMedication_ShouldIncludeReviewId() {
        when(webSocketHandler.sendToDoctor(eq(10L), anyString())).thenReturn(true);

        notificationService.notifyHighRiskMedication(10L, 999L,
                List.of(), "建议");

        verify(webSocketHandler).sendToDoctor(eq(10L), contains("999"));
    }

    // ========== notifyMediumRiskMedication() ==========

    @Test
    void notifyMediumRiskMedication_ShouldSend_WhenDoctorOnline() {
        when(webSocketHandler.sendToDoctor(eq(10L), anyString())).thenReturn(true);

        notificationService.notifyMediumRiskMedication(10L, 2L,
                List.of(Map.of("level", "medium", "content", "注意")),
                "建议复核");

        verify(webSocketHandler).sendToDoctor(eq(10L), contains("MEDIUM_RISK_MEDICATION"));
    }

    @Test
    void notifyMediumRiskMedication_ShouldNotThrow_WhenDoctorOffline() {
        when(webSocketHandler.sendToDoctor(eq(88L), anyString())).thenReturn(false);

        notificationService.notifyMediumRiskMedication(88L, 2L,
                List.of(), "建议");

        verify(webSocketHandler).sendToDoctor(eq(88L), contains("MEDIUM_RISK_MEDICATION"));
    }

    // ========== notify() ==========

    @Test
    void notify_ShouldSend_WithExtraData() {
        when(webSocketHandler.sendToDoctor(eq(5L), anyString())).thenReturn(true);

        notificationService.notify(5L, "CUSTOM_TYPE", "自定义标题", "自定义内容",
                Map.of("orderId", 123, "status", "pending"));

        verify(webSocketHandler).sendToDoctor(eq(5L), argThat(json ->
                json.contains("CUSTOM_TYPE") && json.contains("自定义标题")));
    }

    @Test
    void notify_ShouldSend_WithoutExtraData() {
        when(webSocketHandler.sendToDoctor(eq(5L), anyString())).thenReturn(true);

        notificationService.notify(5L, "SIMPLE_TYPE", "标题", "内容", null);

        verify(webSocketHandler).sendToDoctor(eq(5L), argThat(json ->
                json.contains("SIMPLE_TYPE")));
    }

    @Test
    void notify_ShouldNotThrow_WhenDoctorOffline() {
        when(webSocketHandler.sendToDoctor(eq(77L), anyString())).thenReturn(false);

        notificationService.notify(77L, "TYPE", "标题", "内容", null);

        verify(webSocketHandler).sendToDoctor(eq(77L), anyString());
    }
}
