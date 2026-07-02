package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.common.util.JwtUtil;
import com.neusoft.cloud_brain_diagnosis.entity.TriageRecord;
import com.neusoft.cloud_brain_diagnosis.feign.AiOtherFeignClient;
import com.neusoft.cloud_brain_diagnosis.service.ai.AiTriageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = TriageController.class, properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration," +
                "org.springframework.boot.autoconfigure.websocket.reactive.WebSocketReactiveAutoConfiguration"
})
class TriageControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private AiOtherFeignClient otherFeignClient;
    @MockBean private AiTriageService triageService;
    @MockBean private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        when(jwtUtil.validateToken(anyString())).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(1L);
    }

    // ========== consult() ==========

    @Test
    void consult_ShouldReturnRecommendation() throws Exception {
        when(triageService.consult(eq("头痛"), isNull()))
                .thenReturn(Map.of(
                        "id", 100L,
                        "recommendDepartment", "神经内科",
                        "recommendDepartmentId", 8,
                        "recommendDoctors", List.of(),
                        "analysis", "头痛建议看神经内科",
                        "confidence", 85
                ));

        mockMvc.perform(post("/api/triage/consult")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"chiefComplaint\":\"头痛\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommendDepartment").value("神经内科"))
                .andExpect(jsonPath("$.data.recommendDepartmentId").value(8))
                .andExpect(jsonPath("$.data.confidence").value(85));
    }

    @Test
    void consult_ShouldPassPatientId_WhenProvided() throws Exception {
        when(triageService.consult(eq("胃痛"), eq(1L)))
                .thenReturn(Map.of(
                        "id", 100L,
                        "recommendDepartment", "消化内科",
                        "recommendDepartmentId", 11
                ));

        mockMvc.perform(post("/api/triage/consult")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"chiefComplaint\":\"胃痛\",\"patientId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommendDepartment").value("消化内科"));
    }

    // ========== getPatientList() ==========

    @Test
    void getPatientList_ShouldReturnPage() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PATIENT.getCode());

        TriageRecord record = new TriageRecord();
        record.setId(1L);
        record.setChiefComplaint("头痛");
        record.setRecommendDepartment("神经内科");

        when(triageService.getPatientList(eq(1L), eq(1), eq(10)))
                .thenReturn(new PageImpl<>(List.of(record)));

        mockMvc.perform(get("/api/triage/patient/list")
                        .header("Authorization", "Bearer patient-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].chiefComplaint").value("头痛"))
                .andExpect(jsonPath("$.data.content[0].recommendDepartment").value("神经内科"));
    }

    @Test
    void getPatientList_ShouldSupportPagination() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PATIENT.getCode());

        when(triageService.getPatientList(eq(1L), eq(2), eq(20)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/triage/patient/list")
                        .param("page", "2")
                        .param("size", "20")
                        .header("Authorization", "Bearer patient-token"))
                .andExpect(status().isOk());
    }

    // ========== getDetail() ==========

    @Test
    void getDetail_ShouldReturnRecord() throws Exception {
        TriageRecord record = new TriageRecord();
        record.setId(1L);
        record.setChiefComplaint("头痛");
        record.setRecommendDepartment("神经内科");
        record.setRecommendDepartmentId(8L);
        record.setAiAnalysis("建议看神经内科");
        record.setConfidence(85);
        record.setCreateTime(LocalDateTime.now());

        when(triageService.getDetail(1L)).thenReturn(record);

        mockMvc.perform(get("/api/triage/detail/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.chiefComplaint").value("头痛"))
                .andExpect(jsonPath("$.data.recommendDepartment").value("神经内科"));
    }

    @Test
    void getDetail_ShouldThrow_WhenNotFound() throws Exception {
        when(triageService.getDetail(99L))
                .thenThrow(new BusinessException("分诊记录不存在"));

        mockMvc.perform(get("/api/triage/detail/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }
}
