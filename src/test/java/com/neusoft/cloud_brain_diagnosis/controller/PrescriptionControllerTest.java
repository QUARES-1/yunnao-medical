package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.common.util.JwtUtil;
import com.neusoft.cloud_brain_diagnosis.entity.Prescription;
import com.neusoft.cloud_brain_diagnosis.service.PrescriptionService;
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
 * PrescriptionController Web层测试
 * 覆盖：处方开具、取消、查询列表、详情、发药
 */
@WebMvcTest(PrescriptionController.class)
class PrescriptionControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private PrescriptionService prescriptionService;
    @MockBean private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        when(jwtUtil.validateToken(anyString())).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(10L);
    }

    // ========== 医生-开具处方 ==========

    @Test
    void createPrescription_ShouldSucceed() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());

        Prescription prescription = new Prescription();
        prescription.setId(1L);
        prescription.setStatus("待发药");

        when(prescriptionService.createPrescription(any(), eq(10L))).thenReturn(prescription);

        mockMvc.perform(post("/api/prescription/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"registrationId\":100,\"drugs\":\"阿莫西林 x2\"}")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("待发药"));
    }

    @Test
    void createPrescription_ShouldReturn500_WhenRegistrationIdMissing() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());

        when(prescriptionService.createPrescription(any(), eq(10L)))
                .thenThrow(new BusinessException("请先完成挂号"));

        mockMvc.perform(post("/api/prescription/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"drugs\":\"阿莫西林\"}")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void createPrescription_ShouldReturn500_WhenDrugsEmpty() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());

        when(prescriptionService.createPrescription(any(), eq(10L)))
                .thenThrow(new BusinessException("药品信息不能为空"));

        mockMvc.perform(post("/api/prescription/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"registrationId\":100,\"drugs\":\"\"}")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void createPrescription_ShouldReturn500_WhenNotConsulting() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());

        when(prescriptionService.createPrescription(any(), eq(10L)))
                .thenThrow(new BusinessException("当前状态不能开立处方"));

        mockMvc.perform(post("/api/prescription/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"registrationId\":100,\"drugs\":\"阿莫西林\"}")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ========== 取消处方 ==========

    @Test
    void cancelPrescription_ShouldSucceed() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());

        when(prescriptionService.cancelPrescription(1L, 10L)).thenReturn("处方已撤销");

        mockMvc.perform(put("/api/prescription/cancel/1")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("处方已撤销"));
    }

    @Test
    void cancelPrescription_ShouldReturn500_WhenNotOwner() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());

        when(prescriptionService.cancelPrescription(1L, 10L))
                .thenThrow(new BusinessException("无权操作他人的处方"));

        mockMvc.perform(put("/api/prescription/cancel/1")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ========== 根据挂号ID查询 ==========

    @Test
    void getByRegistrationId_ShouldReturnList() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());

        Prescription prescription = new Prescription();
        prescription.setId(1L);
        prescription.setDrugs("阿莫西林 x2");

        when(prescriptionService.getByRegistrationId(eq(100L), eq(10L)))
                .thenReturn(List.of(prescription));

        mockMvc.perform(get("/api/prescription/registration/100")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].drugs").value("阿莫西林 x2"));
    }

    // ========== 处方详情 ==========

    @Test
    void getDetail_ShouldReturnPrescription() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PATIENT.getCode());
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(1L);

        Prescription prescription = new Prescription();
        prescription.setId(1L);
        prescription.setStatus("待发药");
        prescription.setDrugs("阿莫西林");

        when(prescriptionService.getDetail(1L, 1L, "patient")).thenReturn(prescription);

        mockMvc.perform(get("/api/prescription/detail/1")
                        .header("Authorization", "Bearer patient-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.drugs").value("阿莫西林"));
    }

    @Test
    void getDetail_ShouldReturn500_WhenNotFound() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PATIENT.getCode());
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(1L);

        when(prescriptionService.getDetail(99L, 1L, "patient"))
                .thenThrow(new BusinessException("处方不存在"));

        mockMvc.perform(get("/api/prescription/detail/99")
                        .header("Authorization", "Bearer patient-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ========== 患者-我的处方 ==========

    @Test
    void getPatientList_ShouldReturnPage() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PATIENT.getCode());
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(1L);

        when(prescriptionService.getPatientList(eq(1L), eq(1), eq(10)))
                .thenReturn(new PageImpl<>(List.of(new Prescription())));

        mockMvc.perform(get("/api/prescription/patient/list")
                        .header("Authorization", "Bearer patient-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    // ========== 医生-我开的处方 ==========

    @Test
    void getDoctorList_ShouldReturnPage() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(10L);

        when(prescriptionService.getDoctorList(eq(10L), eq(1), eq(10)))
                .thenReturn(new PageImpl<>(List.of(new Prescription())));

        mockMvc.perform(get("/api/prescription/doctor/list")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    // ========== 药房-待发药列表 ==========

    @Test
    void getPharmacyList_ShouldReturnPage() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PHARMACY.getCode());

        when(prescriptionService.getPharmacyList(eq("待发药"), eq(1), eq(10)))
                .thenReturn(new PageImpl<>(List.of(new Prescription())));

        mockMvc.perform(get("/api/prescription/pharmacy/list")
                        .header("Authorization", "Bearer pharmacy-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void getPharmacyList_ShouldReturnAll_WhenStatusEmpty() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PHARMACY.getCode());

        when(prescriptionService.getPharmacyList(eq(""), eq(1), eq(10)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/prescription/pharmacy/list")
                        .param("status", "")
                        .header("Authorization", "Bearer pharmacy-token"))
                .andExpect(status().isOk());
    }

    // ========== 药房-发药 ==========

    @Test
    void dispense_ShouldSucceed() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PHARMACY.getCode());

        when(prescriptionService.dispense(1L)).thenReturn("发药成功");

        mockMvc.perform(put("/api/prescription/dispense/1")
                        .header("Authorization", "Bearer pharmacy-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("发药成功"));
    }

    @Test
    void dispense_ShouldReturn500_WhenAlreadyDispensed() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PHARMACY.getCode());

        when(prescriptionService.dispense(1L))
                .thenThrow(new BusinessException("该处方已发药"));

        mockMvc.perform(put("/api/prescription/dispense/1")
                        .header("Authorization", "Bearer pharmacy-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void dispense_ShouldReturn500_WhenAlreadyCancelled() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PHARMACY.getCode());

        when(prescriptionService.dispense(1L))
                .thenThrow(new BusinessException("该处方已撤销，无法发药"));

        mockMvc.perform(put("/api/prescription/dispense/1")
                        .header("Authorization", "Bearer pharmacy-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }
}
