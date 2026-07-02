package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import com.neusoft.cloud_brain_diagnosis.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InternalNotificationControllerTest {

    private NotificationService notificationService;
    private InternalNotificationController controller;

    @BeforeEach
    void setUp() {
        notificationService = mock(NotificationService.class);
        controller = new InternalNotificationController(notificationService);
    }

    @Test
    void notifyHighRisk_ShouldDelegateAndReturnSuccess() {
        List<Map<String, Object>> warnings = List.of(Map.of("level", "high"));

        Result<Void> result = controller.notifyHighRisk(1L, 2L, warnings, "fix");

        assertEquals(200, result.getCode());
        assertNull(result.getData());
        verify(notificationService).notifyHighRiskMedication(1L, 2L, warnings, "fix");
    }

    @Test
    void notifyMediumRisk_ShouldDelegateAndReturnSuccess() {
        List<Map<String, Object>> warnings = List.of(Map.of("level", "medium"));

        Result<Void> result = controller.notifyMediumRisk(3L, 4L, warnings, "watch");

        assertEquals(200, result.getCode());
        assertNull(result.getData());
        verify(notificationService).notifyMediumRiskMedication(3L, 4L, warnings, "watch");
    }
}
