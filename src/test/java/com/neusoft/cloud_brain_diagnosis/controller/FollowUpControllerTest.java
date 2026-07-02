package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.common.util.JwtUtil;
import com.neusoft.cloud_brain_diagnosis.entity.FollowUpPlan;
import com.neusoft.cloud_brain_diagnosis.entity.FollowUpRecord;
import com.neusoft.cloud_brain_diagnosis.feign.AiOtherFeignClient;
import com.neusoft.cloud_brain_diagnosis.service.ai.AiFollowUpService;
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

@WebMvcTest(value = FollowUpController.class, properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration," +
                "org.springframework.boot.autoconfigure.websocket.reactive.WebSocketReactiveAutoConfiguration"
})
class FollowUpControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private AiOtherFeignClient otherFeignClient;
    @MockBean private AiFollowUpService followUpService;
    @MockBean private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        when(jwtUtil.validateToken(anyString())).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(10L);
    }

    // ========== createPlan() ==========

    @Test
    void createPlan_ShouldReturnPlan() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());

        FollowUpPlan plan = new FollowUpPlan();
        plan.setId(100L);
        plan.setPatientId(1L);
        plan.setDoctorId(10L);
        plan.setDisease("感冒");
        plan.setStatus("ongoing");
        plan.setTotalTimes(3);

        when(followUpService.createPlan(anyMap(), eq(10L))).thenReturn(plan);

        mockMvc.perform(post("/api/follow-up/plan/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":1,\"disease\":\"感冒\",\"totalTimes\":3}")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(100))
                .andExpect(jsonPath("$.data.patientId").value(1))
                .andExpect(jsonPath("$.data.status").value("ongoing"))
                .andExpect(jsonPath("$.data.totalTimes").value(3));
    }

    // ========== getPatientPlans() ==========

    @Test
    void getPatientPlans_ShouldReturnPage() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PATIENT.getCode());
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(1L);

        FollowUpPlan plan = new FollowUpPlan();
        plan.setId(1L);
        plan.setDisease("感冒");

        when(followUpService.getPatientPlans(eq(1L), eq(1), eq(10)))
                .thenReturn(new PageImpl<>(List.of(plan)));

        mockMvc.perform(get("/api/follow-up/patient/plans")
                        .header("Authorization", "Bearer patient-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].disease").value("感冒"));
    }

    // ========== getPendingRecords() ==========

    @Test
    void getPendingRecords_ShouldReturnPage() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PATIENT.getCode());
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(1L);

        FollowUpRecord record = new FollowUpRecord();
        record.setId(1L);
        record.setStatus("pending");

        when(followUpService.getPendingRecords(eq(1L), eq(1), eq(10)))
                .thenReturn(new PageImpl<>(List.of(record)));

        mockMvc.perform(get("/api/follow-up/pending")
                        .header("Authorization", "Bearer patient-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("pending"));
    }

    // ========== submitRecord() ==========

    @Test
    void submitRecord_ShouldReturnSuccess() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PATIENT.getCode());
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(1L);

        when(followUpService.submitRecord(eq(1L), anyString(), eq(1L)))
                .thenReturn("提交成功");

        mockMvc.perform(post("/api/follow-up/submit/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":{\"recovery\":\"明显好转\"}}")
                        .header("Authorization", "Bearer patient-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("提交成功"));
    }

    @Test
    void submitRecord_ShouldHandleEmptyAnswers() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PATIENT.getCode());
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(1L);

        when(followUpService.submitRecord(eq(1L), eq("{}"), eq(1L)))
                .thenReturn("提交成功");

        mockMvc.perform(post("/api/follow-up/submit/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header("Authorization", "Bearer patient-token"))
                .andExpect(status().isOk());
    }

    @Test
    void submitRecord_ShouldThrow_WhenNotFound() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PATIENT.getCode());
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(1L);

        when(followUpService.submitRecord(eq(99L), anyString(), eq(1L)))
                .thenThrow(new BusinessException("随访记录不存在"));

        mockMvc.perform(post("/api/follow-up/submit/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header("Authorization", "Bearer patient-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ========== getDetail() ==========

    @Test
    void getDetail_ShouldReturnRecord() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PATIENT.getCode());
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(1L);

        when(followUpService.getDetail(1L))
                .thenReturn(Map.of(
                        "id", 1L,
                        "planId", 100L,
                        "status", "completed",
                        "disease", "感冒"
                ));

        mockMvc.perform(get("/api/follow-up/detail/1")
                        .header("Authorization", "Bearer patient-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("completed"));
    }

    @Test
    void getDetail_ShouldThrow_WhenNotFound() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PATIENT.getCode());
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(1L);

        when(followUpService.getDetail(99L))
                .thenThrow(new BusinessException("随访记录不存在"));

        mockMvc.perform(get("/api/follow-up/detail/99")
                        .header("Authorization", "Bearer patient-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ========== getDoctorList() ==========

    @Test
    void getDoctorList_ShouldReturnPage() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());

        FollowUpPlan plan = new FollowUpPlan();
        plan.setId(1L);
        plan.setStatus("ongoing");

        when(followUpService.getDoctorList(eq(10L), eq(1), eq(10)))
                .thenReturn(new PageImpl<>(List.of(plan)));

        mockMvc.perform(get("/api/follow-up/doctor/list")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(1));
    }

    // ========== doctorReply() ==========

    @Test
    void doctorReply_ShouldReturnSuccess() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());

        when(followUpService.doctorReply(eq(1L), eq("请尽快复诊"), eq(10L)))
                .thenReturn("回复成功");

        mockMvc.perform(post("/api/follow-up/doctor-reply/1")
                        .param("remark", "请尽快复诊")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("回复成功"));
    }

    @Test
    void doctorReply_ShouldThrow_WhenNotFound() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());

        when(followUpService.doctorReply(eq(99L), anyString(), eq(10L)))
                .thenThrow(new BusinessException("随访记录不存在"));

        mockMvc.perform(post("/api/follow-up/doctor-reply/99")
                        .param("remark", "测试")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }
}
