package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.common.util.JwtUtil;
import com.neusoft.cloud_brain_diagnosis.entity.MedicalRecord;
import com.neusoft.cloud_brain_diagnosis.service.MedicalRecordService;
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
 * MedicalRecordController Web层测试
 * 覆盖：保存病历、详情、按挂号ID查询、患者列表、医生列表
 */
@WebMvcTest(MedicalRecordController.class)
class MedicalRecordControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private MedicalRecordService medicalRecordService;
    @MockBean private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        when(jwtUtil.validateToken(anyString())).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(10L);
    }

    // ========== 医生-保存病历 ==========

    @Test
    void saveRecord_ShouldSucceed_NewRecord() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());

        MedicalRecord record = new MedicalRecord();
        record.setId(1L);
        record.setChiefComplaint("头痛");
        record.setDiagnosis("感冒");

        when(medicalRecordService.saveRecord(any(), eq(10L))).thenReturn(record);

        mockMvc.perform(post("/api/medical-record/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"registrationId\":100,\"chiefComplaint\":\"头痛\",\"diagnosis\":\"感冒\"}")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chiefComplaint").value("头痛"));
    }

    @Test
    void saveRecord_ShouldSucceed_UpdateExisting() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());

        MedicalRecord record = new MedicalRecord();
        record.setId(1L);
        record.setChiefComplaint("头痛（更新）");
        record.setDiagnosis("感冒");
        record.setTreatment("休息三天");

        when(medicalRecordService.saveRecord(any(), eq(10L))).thenReturn(record);

        mockMvc.perform(post("/api/medical-record/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"registrationId\":100,\"chiefComplaint\":\"头痛（更新）\"}")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chiefComplaint").value("头痛（更新）"));
    }

    @Test
    void saveRecord_ShouldReturn500_WhenRegistrationIdMissing() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());

        when(medicalRecordService.saveRecord(any(), eq(10L)))
                .thenThrow(new BusinessException("请先完成挂号"));

        mockMvc.perform(post("/api/medical-record/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"chiefComplaint\":\"头痛\"}")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void saveRecord_ShouldReturn500_WhenRegistrationNotFound() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());

        when(medicalRecordService.saveRecord(any(), eq(10L)))
                .thenThrow(new BusinessException("挂号记录不存在"));

        mockMvc.perform(post("/api/medical-record/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"registrationId\":999}")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void saveRecord_ShouldReturn500_WhenNotAuthorized() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());

        when(medicalRecordService.saveRecord(any(), eq(10L)))
                .thenThrow(new BusinessException("无权为该患者书写病历"));

        mockMvc.perform(post("/api/medical-record/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"registrationId\":100}")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ========== 病历详情 ==========

    @Test
    void getDetail_ShouldReturnRecord_AsPatient() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PATIENT.getCode());
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(1L);

        MedicalRecord record = new MedicalRecord();
        record.setId(1L);
        record.setDiagnosis("感冒");
        record.setTreatment("休息三天");

        when(medicalRecordService.getDetail(1L, 1L, "patient")).thenReturn(record);

        mockMvc.perform(get("/api/medical-record/detail/1")
                        .header("Authorization", "Bearer patient-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.diagnosis").value("感冒"));
    }

    @Test
    void getDetail_ShouldReturnRecord_AsDoctor() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(10L);

        MedicalRecord record = new MedicalRecord();
        record.setId(1L);
        record.setDiagnosis("感冒");

        when(medicalRecordService.getDetail(1L, 10L, "doctor")).thenReturn(record);

        mockMvc.perform(get("/api/medical-record/detail/1")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.diagnosis").value("感冒"));
    }

    @Test
    void getDetail_ShouldReturn500_WhenNotFound() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PATIENT.getCode());
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(1L);

        when(medicalRecordService.getDetail(99L, 1L, "patient"))
                .thenThrow(new BusinessException("病历不存在"));

        mockMvc.perform(get("/api/medical-record/detail/99")
                        .header("Authorization", "Bearer patient-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void getDetail_ShouldReturn500_WhenUnauthorizedRole() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PHARMACY.getCode());
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(1L);

        when(medicalRecordService.getDetail(1L, 1L, "pharmacy"))
                .thenThrow(new BusinessException("当前角色无权查看病历"));

        mockMvc.perform(get("/api/medical-record/detail/1")
                        .header("Authorization", "Bearer pharmacy-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ========== 根据挂号ID查询 ==========

    @Test
    void getByRegistrationId_ShouldReturnRecord() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(10L);

        MedicalRecord record = new MedicalRecord();
        record.setId(1L);
        record.setRegistrationId(100L);

        when(medicalRecordService.getByRegistrationId(eq(100L), eq(10L), eq("doctor")))
                .thenReturn(record);

        mockMvc.perform(get("/api/medical-record/registration/100")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.registrationId").value(100));
    }

    @Test
    void getByRegistrationId_ShouldReturn500_WhenNotFound() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(10L);

        when(medicalRecordService.getByRegistrationId(eq(100L), eq(10L), eq("doctor")))
                .thenThrow(new BusinessException("病历不存在"));

        mockMvc.perform(get("/api/medical-record/registration/100")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ========== 患者-我的病历列表 ==========

    @Test
    void getPatientList_ShouldReturnPage() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PATIENT.getCode());
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(1L);

        when(medicalRecordService.getPatientList(eq(1L), eq(1), eq(10)))
                .thenReturn(new PageImpl<>(List.of(new MedicalRecord())));

        mockMvc.perform(get("/api/medical-record/patient/list")
                        .header("Authorization", "Bearer patient-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void getPatientList_ShouldSupportPagination() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PATIENT.getCode());
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(1L);

        when(medicalRecordService.getPatientList(eq(1L), eq(2), eq(5)))
                .thenReturn(new PageImpl<>(List.of(), org.springframework.data.domain.PageRequest.of(1, 5), 0));

        mockMvc.perform(get("/api/medical-record/patient/list")
                        .param("page", "2")
                        .param("size", "5")
                        .header("Authorization", "Bearer patient-token"))
                .andExpect(status().isOk());
    }

    // ========== 医生-我的病历列表 ==========

    @Test
    void getDoctorList_ShouldReturnPage() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(10L);

        when(medicalRecordService.getDoctorList(eq(10L), eq(1), eq(10)))
                .thenReturn(new PageImpl<>(List.of(new MedicalRecord())));

        mockMvc.perform(get("/api/medical-record/doctor/list")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }
}
