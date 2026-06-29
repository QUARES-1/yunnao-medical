package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.common.util.JwtUtil;
import com.neusoft.cloud_brain_diagnosis.entity.Patient;
import com.neusoft.cloud_brain_diagnosis.service.PatientService;
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

/**
 * PatientController Web层测试
 * 覆盖：微信登录、患者信息、修改信息、绑定手机号
 */
@WebMvcTest(PatientController.class)
class PatientControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private PatientService patientService;
    @MockBean private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        when(jwtUtil.validateToken(anyString())).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(1L);
    }

    // ========== 微信登录 ==========

    @Test
    void wxLogin_ShouldReturnTokenAndPatientInfo() throws Exception {
        java.util.Map<String, Object> resultMap = new java.util.HashMap<>();
        resultMap.put("token", "wx-token-123");
        resultMap.put("patientId", 1L);
        resultMap.put("name", "微信用户");
        resultMap.put("phone", null);
        resultMap.put("needCompleteInfo", true);
        when(patientService.wxLogin("test-code")).thenReturn(resultMap);

        mockMvc.perform(post("/api/patient/wx-login")
                        .param("code", "test-code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("wx-token-123"))
                .andExpect(jsonPath("$.data.patientId").value(1))
                .andExpect(jsonPath("$.data.needCompleteInfo").value(true));
    }

    @Test
    void wxLogin_ShouldReturnExistingPatient() throws Exception {
        java.util.Map<String, Object> resultMap = new java.util.HashMap<>();
        resultMap.put("token", "wx-token-existing");
        resultMap.put("patientId", 5L);
        resultMap.put("name", "张三");
        resultMap.put("phone", "13800138000");
        resultMap.put("needCompleteInfo", false);
        when(patientService.wxLogin("test-code")).thenReturn(resultMap);

        mockMvc.perform(post("/api/patient/wx-login")
                        .param("code", "test-code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.needCompleteInfo").value(false));
    }

    @Test
    void wxLogin_ShouldReturn500_WhenCodeIsInvalid() throws Exception {
        when(patientService.wxLogin("invalid-code"))
                .thenThrow(new BusinessException("微信授权失败：invalid code"));

        mockMvc.perform(post("/api/patient/wx-login")
                        .param("code", "invalid-code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void wxLogin_ShouldReturn500_WhenWechatServerError() throws Exception {
        when(patientService.wxLogin("error-code"))
                .thenThrow(new BusinessException("微信登录失败，请稍后重试"));

        mockMvc.perform(post("/api/patient/wx-login")
                        .param("code", "error-code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ========== 获取患者信息 ==========

    @Test
    void getPatientInfo_ShouldReturnInfo() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PATIENT.getCode());

        Patient patient = new Patient();
        patient.setId(1L);
        patient.setName("张三");
        patient.setPhone("13800138000");
        patient.setGender("男");
        patient.setAge(30);

        when(patientService.getPatientInfo(1L)).thenReturn(patient);

        mockMvc.perform(get("/api/patient/info")
                        .header("Authorization", "Bearer patient-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("张三"))
                .andExpect(jsonPath("$.data.phone").value("13800138000"));
    }

    @Test
    void getPatientInfo_ShouldReturn500_WhenPatientNotFound() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PATIENT.getCode());
        when(patientService.getPatientInfo(1L))
                .thenThrow(new BusinessException("患者不存在"));

        mockMvc.perform(get("/api/patient/info")
                        .header("Authorization", "Bearer patient-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ========== 修改患者信息 ==========

    @Test
    void updatePatientInfo_ShouldSucceed() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PATIENT.getCode());

        when(patientService.updatePatientInfo(any())).thenReturn("信息更新成功");

        mockMvc.perform(put("/api/patient/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"张三改\",\"age\":25}")
                        .header("Authorization", "Bearer patient-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("信息更新成功"));
    }

    @Test
    void updatePatientInfo_ShouldReturn500_WhenPatientNotFound() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PATIENT.getCode());

        when(patientService.updatePatientInfo(any()))
                .thenThrow(new BusinessException("患者不存在"));

        mockMvc.perform(put("/api/patient/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"新名字\"}")
                        .header("Authorization", "Bearer patient-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ========== 绑定手机号 ==========

    @Test
    void bindPhone_ShouldSucceed() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PATIENT.getCode());

        when(patientService.bindPhone(1L, "13899998888")).thenReturn("手机号绑定成功");

        mockMvc.perform(post("/api/patient/bind-phone")
                        .param("phone", "13899998888")
                        .header("Authorization", "Bearer patient-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("手机号绑定成功"));
    }

    @Test
    void bindPhone_ShouldReturn500_WhenPhoneAlreadyBound() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PATIENT.getCode());

        when(patientService.bindPhone(1L, "13800138000"))
                .thenThrow(new BusinessException("该手机号已被绑定"));

        mockMvc.perform(post("/api/patient/bind-phone")
                        .param("phone", "13800138000")
                        .header("Authorization", "Bearer patient-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void bindPhone_ShouldReturn500_WhenPatientNotFound() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PATIENT.getCode());
        when(patientService.bindPhone(1L, "13899998888"))
                .thenThrow(new BusinessException("患者不存在"));

        mockMvc.perform(post("/api/patient/bind-phone")
                        .param("phone", "13899998888")
                        .header("Authorization", "Bearer patient-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }
}
