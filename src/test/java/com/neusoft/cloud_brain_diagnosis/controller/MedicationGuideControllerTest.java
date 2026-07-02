package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.util.JwtUtil;
import com.neusoft.cloud_brain_diagnosis.service.ai.AiMedicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = MedicationGuideController.class, properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration," +
                "org.springframework.boot.autoconfigure.websocket.reactive.WebSocketReactiveAutoConfiguration"
})
class MedicationGuideControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private AiMedicationService medicationService;
    @MockBean private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        when(jwtUtil.validateToken(anyString())).thenReturn(true);
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PHARMACY.getCode());
    }

    // ========== generateGuide ==========

    @Test
    void generateGuide_ShouldReturnGuide() throws Exception {
        when(medicationService.generateGuide(1L))
                .thenReturn(Map.of("id", 1L, "prescriptionId", 1L));

        mockMvc.perform(post("/api/medication/guide/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prescriptionId\":1}")
                        .header("Authorization", "Bearer pharmacy-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void generateGuide_ShouldThrow_WhenPrescriptionIdIsNull() throws Exception {
        when(medicationService.generateGuide(isNull()))
                .thenThrow(new RuntimeException("处方编号不能为空"));

        mockMvc.perform(post("/api/medication/guide/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header("Authorization", "Bearer pharmacy-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ========== getGuide ==========

    @Test
    void getGuide_ShouldReturnGuide() throws Exception {
        when(medicationService.getGuide(1L))
                .thenReturn(Map.of("id", 1L, "patientName", "张三"));

        mockMvc.perform(get("/api/medication/guide/1")
                        .header("Authorization", "Bearer patient-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.patientName").value("张三"));
    }

    // ========== markPrinted ==========

    @Test
    void markPrinted_ShouldReturnMessage() throws Exception {
        when(medicationService.markPrinted(1L)).thenReturn("打印记录已保存");

        mockMvc.perform(post("/api/medication/guide/print/1")
                        .header("Authorization", "Bearer pharmacy-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("打印记录已保存"));
    }
}
