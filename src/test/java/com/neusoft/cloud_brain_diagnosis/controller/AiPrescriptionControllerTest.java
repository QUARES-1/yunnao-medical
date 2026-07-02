package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.common.util.JwtUtil;
import com.neusoft.cloud_brain_diagnosis.entity.PrescriptionAiReview;
import com.neusoft.cloud_brain_diagnosis.feign.AiPrescriptionFeignClient;
import com.neusoft.cloud_brain_diagnosis.service.ai.AiPrescriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = AiPrescriptionController.class, properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration," +
                "org.springframework.boot.autoconfigure.websocket.reactive.WebSocketReactiveAutoConfiguration"
})
class AiPrescriptionControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private AiPrescriptionFeignClient prescriptionFeignClient;
    @MockBean private AiPrescriptionService prescriptionService;
    @MockBean private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        when(jwtUtil.validateToken(anyString())).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(10L);
    }

    // ========== checkPrescription() ==========

    @Test
    void checkPrescription_ShouldReturnReviewResult_AsDoctor() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());

        when(prescriptionService.checkPrescription(anyMap(), eq(10L)))
                .thenReturn(Map.of(
                        "id", 100L,
                        "reviewResult", "pass",
                        "reviewScore", 95,
                        "suggestions", "处方合理"
                ));

        mockMvc.perform(post("/api/prescription/ai/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":1,\"patientAge\":45,\"patientGender\":\"男\",\"drugs\":[{\"name\":\"阿司匹林\"}]}")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewResult").value("pass"))
                .andExpect(jsonPath("$.data.reviewScore").value(95))
                .andExpect(jsonPath("$.data.suggestions").value("处方合理"));
    }

    @Test
    void checkPrescription_ShouldReturnWarningResult() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());

        when(prescriptionService.checkPrescription(anyMap(), eq(10L)))
                .thenReturn(Map.of(
                        "id", 100L,
                        "reviewResult", "warning",
                        "reviewScore", 70,
                        "warnings", List.of(Map.of("level", "medium", "content", "剂量偏大"))
                ));

        mockMvc.perform(post("/api/prescription/ai/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":1,\"patientAge\":30,\"patientGender\":\"女\",\"drugs\":[]}")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewResult").value("warning"));
    }

    @Test
    void checkPrescription_ShouldWorkAsPharmacy() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PHARMACY.getCode());
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(20L);

        when(prescriptionService.checkPrescription(anyMap(), eq(20L)))
                .thenReturn(Map.of("id", 100L, "reviewResult", "pass", "reviewScore", 100));

        mockMvc.perform(post("/api/prescription/ai/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":1,\"patientAge\":50,\"patientGender\":\"男\",\"drugs\":[]}")
                        .header("Authorization", "Bearer pharmacy-token"))
                .andExpect(status().isOk());
    }

    // ========== getReviewList() ==========

    @Test
    void getReviewList_ShouldReturnPage_AsDoctor() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());

        PrescriptionAiReview review = new PrescriptionAiReview();
        review.setId(1L);
        review.setReviewResult("pass");

        when(prescriptionService.getReviewList(eq(10L), eq(1), eq(10)))
                .thenReturn(new PageImpl<>(List.of(review)));

        mockMvc.perform(get("/api/prescription/ai/review-list")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(1));
    }

    @Test
    void getReviewList_ShouldReturnAll_AsPharmacy() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PHARMACY.getCode());

        when(prescriptionService.getReviewList(isNull(), eq(1), eq(10)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/prescription/ai/review-list")
                        .header("Authorization", "Bearer pharmacy-token"))
                .andExpect(status().isOk());
    }

    // ========== getReviewDetail() ==========

    @Test
    void getReviewDetail_ShouldReturnRecord() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());

        PrescriptionAiReview review = new PrescriptionAiReview();
        review.setId(1L);
        review.setReviewResult("warning");
        review.setReviewScore(75);

        when(prescriptionService.getReviewDetail(1L)).thenReturn(review);

        mockMvc.perform(get("/api/prescription/ai/review/1")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.reviewResult").value("warning"))
                .andExpect(jsonPath("$.data.reviewScore").value(75));
    }

    @Test
    void getReviewDetail_ShouldThrow_WhenNotFound() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());

        when(prescriptionService.getReviewDetail(99L))
                .thenThrow(new BusinessException("审核记录不存在"));

        mockMvc.perform(get("/api/prescription/ai/review/99")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }
}
