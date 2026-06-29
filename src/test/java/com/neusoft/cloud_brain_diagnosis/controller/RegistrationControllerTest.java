package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.util.JwtUtil;
import com.neusoft.cloud_brain_diagnosis.entity.Registration;
import com.neusoft.cloud_brain_diagnosis.service.RegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * RegistrationController Web层测试
 * 覆盖：患者创建/取消挂号、医生接诊流程、历史挂号列表
 */
@WebMvcTest(RegistrationController.class)
class RegistrationControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private RegistrationService registrationService;
    @MockBean private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        when(jwtUtil.validateToken(anyString())).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(1L);
    }

    @Test
    void createRegistration_ShouldSucceed_AsPatient() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PATIENT.getCode());

        Registration reg = new Registration();
        reg.setId(100L);
        reg.setStatus("待就诊");

        when(registrationService.createRegistration(any())).thenReturn(reg);

        mockMvc.perform(post("/api/registration/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"doctorId\":10,\"registrationDate\":\"2026-06-20\",\"timeSlot\":\"上午\"}")
                        .header("Authorization", "Bearer patient-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("待就诊"));
    }

    @Test
    void cancelRegistration_ShouldSucceed_AsPatient() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PATIENT.getCode());

        when(registrationService.cancelRegistration(100L, 1L)).thenReturn("取消挂号成功");

        mockMvc.perform(put("/api/registration/cancel/100")
                        .header("Authorization", "Bearer patient-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("取消挂号成功"));
    }

    @Test
    void getPatientList_ShouldReturnPage() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PATIENT.getCode());

        when(registrationService.getPatientRegistrationList(eq(1L), isNull(), eq(1), eq(10)))
                .thenReturn(new PageImpl<>(List.of(new Registration())));

        mockMvc.perform(get("/api/registration/patient/list")
                        .header("Authorization", "Bearer patient-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void getDetail_ShouldReturnRegistration() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PATIENT.getCode());

        Registration reg = new Registration();
        reg.setId(100L);
        when(registrationService.getDetail(eq(100L), eq(1L), eq("patient"))).thenReturn(reg);

        mockMvc.perform(get("/api/registration/detail/100")
                        .header("Authorization", "Bearer patient-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(100));
    }

    @Test
    void getDoctorTodayList_ShouldReturnList() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(10L);

        when(registrationService.getDoctorTodayList(10L)).thenReturn(List.of(new Registration()));

        mockMvc.perform(get("/api/registration/doctor/today")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void startConsultation_ShouldSucceed() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(10L); // doctorId

        when(registrationService.startConsultation(100L, 10L)).thenReturn("开始看诊");

        mockMvc.perform(put("/api/registration/start/100")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("开始看诊"));
    }

    @Test
    void completeConsultation_ShouldSucceed() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(10L); // doctorId

        when(registrationService.completeConsultation(100L, 10L)).thenReturn("看诊完成");

        mockMvc.perform(put("/api/registration/complete/100")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("看诊完成"));
    }

    // ========== 医生-历史挂号列表 ==========

    @Test
    void getDoctorList_ShouldReturnPage() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(10L);

        when(registrationService.getDoctorList(eq(10L), isNull(), isNull(), eq(1), eq(10)))
                .thenReturn(new PageImpl<>(List.of(new Registration())));

        mockMvc.perform(get("/api/registration/doctor/list")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void getDoctorList_ShouldFilterByKeywordAndStatus() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(10L);

        when(registrationService.getDoctorList(eq(10L), eq("张三"), eq("待就诊"), eq(1), eq(10)))
                .thenReturn(new PageImpl<>(List.of(new Registration())));

        mockMvc.perform(get("/api/registration/doctor/list")
                        .param("keyword", "张三")
                        .param("status", "待就诊")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void getDoctorList_ShouldReturnEmptyPage() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(10L);

        when(registrationService.getDoctorList(eq(10L), isNull(), isNull(), eq(2), eq(10)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/registration/doctor/list")
                        .param("page", "2")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty());
    }
}
