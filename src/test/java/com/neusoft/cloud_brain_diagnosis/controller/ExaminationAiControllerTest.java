package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.context.UserContext;
import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.util.JwtUtil;
import com.neusoft.cloud_brain_diagnosis.entity.CriticalValueWarning;
import com.neusoft.cloud_brain_diagnosis.entity.ExaminationAiInterpretation;
import com.neusoft.cloud_brain_diagnosis.entity.ExaminationAiReview;
import com.neusoft.cloud_brain_diagnosis.feign.AiExaminationFeignClient;
import com.neusoft.cloud_brain_diagnosis.service.ai.AiExaminationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = ExaminationAiController.class, properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration," +
                "org.springframework.boot.autoconfigure.websocket.reactive.WebSocketReactiveAutoConfiguration"
})
class ExaminationAiControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private AiExaminationFeignClient examinationFeignClient;
    @MockBean private AiExaminationService examinationService;
    @MockBean private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        lenient().when(jwtUtil.validateToken(anyString())).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    // ========== interpret ==========

    @Test
    void interpret_ShouldReturnInterpretation() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.LAB.getCode());
        when(examinationService.interpret(1L)).thenReturn(Map.of("id", 1L, "interpretation", "Test interpretation"));

        mockMvc.perform(post("/api/examination/ai/interpret/1")
                        .header("Authorization", "Bearer lab-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void getPatientInterpretation_ShouldReturnInterpretation() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PATIENT.getCode());
        ExaminationAiInterpretation interpretation = new ExaminationAiInterpretation();
        interpretation.setId(1L);

        when(examinationService.getPatientInterpretation(1L)).thenReturn(interpretation);

        mockMvc.perform(get("/api/examination/ai/interpret-patient/1")
                        .header("Authorization", "Bearer patient-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void getProInterpretation_ShouldReturnInterpretation() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());
        ExaminationAiInterpretation interpretation = new ExaminationAiInterpretation();
        interpretation.setId(1L);

        when(examinationService.getProInterpretation(1L)).thenReturn(interpretation);

        mockMvc.perform(get("/api/examination/ai/interpret-pro/1")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    // ========== critical warning ==========

    @Test
    void getCriticalList_ShouldReturnPage() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());
        CriticalValueWarning warning = new CriticalValueWarning();
        warning.setId(1L);

        when(examinationService.getCriticalList(anyLong(), anyString(), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(warning)));

        mockMvc.perform(get("/api/examination/ai/critical-list")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(1));
    }

    @Test
    void getCriticalHistory_ShouldReturnPage() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.ADMIN.getCode());
        CriticalValueWarning warning = new CriticalValueWarning();
        warning.setId(1L);

        when(examinationService.getCriticalHistory(anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(warning)));

        mockMvc.perform(get("/api/examination/ai/critical-history")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(1));
    }

    // ========== review ==========

    @Test
    void getManualList_ShouldReturnPage() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.LAB.getCode());
        ExaminationAiReview review = new ExaminationAiReview();
        review.setId(1L);

        when(examinationService.getManualList(anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(review)));

        mockMvc.perform(get("/api/examination/ai/manual-list")
                        .header("Authorization", "Bearer lab-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(1));
    }

    @Test
    void getReviewList_ShouldReturnPage() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.LAB.getCode());
        ExaminationAiReview review = new ExaminationAiReview();
        review.setId(1L);

        when(examinationService.getReviewList(any(), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(review)));

        mockMvc.perform(get("/api/examination/ai/review-list")
                        .header("Authorization", "Bearer lab-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(1));
    }

    @Test
    void getReviewDetail_ShouldReturnReview() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.LAB.getCode());
        ExaminationAiReview review = new ExaminationAiReview();
        review.setId(1L);

        when(examinationService.getReviewDetail(1L)).thenReturn(review);

        mockMvc.perform(get("/api/examination/ai/review-detail/1")
                        .header("Authorization", "Bearer lab-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void getReviewStats_ShouldReturnStats() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.LAB.getCode());
        when(examinationService.getReviewStats())
                .thenReturn(Map.of("total", 100, "passRate", 0.85));

        mockMvc.perform(get("/api/examination/ai/review-stats")
                        .header("Authorization", "Bearer lab-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(100));
    }
}
