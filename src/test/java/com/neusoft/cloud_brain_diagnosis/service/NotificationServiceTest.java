package com.neusoft.cloud_brain_diagnosis.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.neusoft.cloud_brain_diagnosis.websocket.DoctorNotificationWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private DoctorNotificationWebSocketHandler webSocketHandler;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(webSocketHandler);
    }

    @Test
    void notifyHighRiskMedication_ShouldSendRejectPayload() {
        when(webSocketHandler.sendToDoctor(eq(10L), anyString())).thenReturn(true);
        List<Map<String, Object>> warnings = List.of(Map.of("level", "high", "content", "risk"));

        notificationService.notifyHighRiskMedication(10L, 99L, warnings, "change dosage");

        JSONObject payload = capturePayload();
        assertEquals("HIGH_RISK_MEDICATION", payload.getStr("type"));
        JSONObject data = payload.getJSONObject("data");
        assertEquals(99L, data.getLong("reviewId"));
        assertEquals("reject", data.getStr("reviewResult"));
        assertEquals("change dosage", data.getStr("suggestions"));
        assertNotNull(data.getStr("timestamp"));
    }

    @Test
    void notifyMediumRiskMedication_ShouldSendWarningPayloadEvenWhenOffline() {
        when(webSocketHandler.sendToDoctor(eq(11L), anyString())).thenReturn(false);

        notificationService.notifyMediumRiskMedication(11L, 100L, List.of(), "watch");

        JSONObject payload = capturePayload();
        assertEquals("MEDIUM_RISK_MEDICATION", payload.getStr("type"));
        assertEquals("warning", payload.getJSONObject("data").getStr("reviewResult"));
    }

    @Test
    void notify_ShouldIncludeExtraDataWhenProvided() {
        Map<String, Object> data = Map.of("recordId", 123L, "flag", true);

        notificationService.notify(12L, "CUSTOM", "title", "content", data);

        JSONObject payload = capturePayload();
        assertEquals("CUSTOM", payload.getStr("type"));
        assertEquals("title", payload.getStr("title"));
        assertEquals("content", payload.getStr("content"));
        assertEquals(123L, payload.getJSONObject("data").getLong("recordId"));
        assertTrue(payload.getJSONObject("data").getBool("flag"));
        assertNotNull(payload.getStr("timestamp"));
    }

    @Test
    void notify_ShouldOmitDataWhenExtraDataIsNull() {
        notificationService.notify(13L, "CUSTOM", "title", "content", null);

        JSONObject payload = capturePayload();
        assertEquals("CUSTOM", payload.getStr("type"));
        assertNull(payload.get("data"));
    }

    private JSONObject capturePayload() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(webSocketHandler).sendToDoctor(anyLong(), captor.capture());
        return JSONUtil.parseObj(captor.getValue());
    }
}
