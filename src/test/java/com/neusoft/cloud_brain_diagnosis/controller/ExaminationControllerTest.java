package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.common.util.JwtUtil;
import com.neusoft.cloud_brain_diagnosis.entity.Examination;
import com.neusoft.cloud_brain_diagnosis.entity.ExaminationItem;
import com.neusoft.cloud_brain_diagnosis.service.ExaminationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ExaminationController Web层测试
 * 覆盖：检查开立、取消、查询列表、详情、更新结果、项目列表
 */
@WebMvcTest(ExaminationController.class)
class ExaminationControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private ExaminationService examinationService;
    @MockBean private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        when(jwtUtil.validateToken(anyString())).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(10L);
    }

    // ========== 医生-开立检查 ==========

    @Test
    void createExamination_ShouldSucceed() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());

        Examination exam = new Examination();
        exam.setId(1L);
        exam.setStatus("待检查");

        when(examinationService.createExamination(any(), eq(10L))).thenReturn(exam);

        mockMvc.perform(post("/api/examination/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"registrationId\":100,\"itemId\":5,\"itemName\":\"血常规\"}")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("待检查"));
    }

    @Test
    void createExamination_ShouldReturn500_WhenRegistrationIdMissing() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());

        when(examinationService.createExamination(any(), eq(10L)))
                .thenThrow(new BusinessException("请先完成挂号"));

        mockMvc.perform(post("/api/examination/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":5}")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void createExamination_ShouldReturn500_WhenNotConsulting() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());

        when(examinationService.createExamination(any(), eq(10L)))
                .thenThrow(new BusinessException("当前状态不能开立检查"));

        mockMvc.perform(post("/api/examination/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"registrationId\":100,\"itemId\":5}")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ========== 取消检查 ==========

    @Test
    void cancelExamination_ShouldSucceed() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());

        when(examinationService.cancelExamination(1L, 10L)).thenReturn("检查已取消");

        mockMvc.perform(put("/api/examination/cancel/1")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("检查已取消"));
    }

    @Test
    void cancelExamination_ShouldReturn500_WhenNotOwner() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());

        when(examinationService.cancelExamination(1L, 10L))
                .thenThrow(new BusinessException("无权操作他人的检查单"));

        mockMvc.perform(put("/api/examination/cancel/1")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ========== 根据挂号ID查询 ==========

    @Test
    void getByRegistrationId_ShouldReturnList() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());

        Examination exam = new Examination();
        exam.setId(1L);
        exam.setItemName("血常规");

        when(examinationService.getByRegistrationId(eq(100L), eq(10L)))
                .thenReturn(List.of(exam));

        mockMvc.perform(get("/api/examination/registration/100")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].itemName").value("血常规"));
    }

    // ========== 检查详情 ==========

    @Test
    void getDetail_ShouldReturnExamination() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PATIENT.getCode());
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(1L);

        Examination exam = new Examination();
        exam.setId(1L);
        exam.setStatus("待检查");
        exam.setItemName("血常规");

        when(examinationService.getDetail(1L, 1L, "patient")).thenReturn(exam);

        mockMvc.perform(get("/api/examination/detail/1")
                        .header("Authorization", "Bearer patient-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemName").value("血常规"));
    }

    @Test
    void getDetail_ShouldReturn500_WhenNotFound() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PATIENT.getCode());
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(1L);

        when(examinationService.getDetail(99L, 1L, "patient"))
                .thenThrow(new BusinessException("检查记录不存在"));

        mockMvc.perform(get("/api/examination/detail/99")
                        .header("Authorization", "Bearer patient-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ========== 患者-我的检查 ==========

    @Test
    void getPatientList_ShouldReturnPage() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PATIENT.getCode());
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(1L);

        when(examinationService.getPatientList(eq(1L), eq(1), eq(10)))
                .thenReturn(new PageImpl<>(List.of(new Examination())));

        mockMvc.perform(get("/api/examination/patient/list")
                        .header("Authorization", "Bearer patient-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    // ========== 医生-我开的检查 ==========

    @Test
    void getDoctorList_ShouldReturnPage() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(10L);

        when(examinationService.getDoctorList(eq(10L), eq(1), eq(10)))
                .thenReturn(new PageImpl<>(List.of(new Examination())));

        mockMvc.perform(get("/api/examination/doctor/list")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    // ========== 检验科-待检查列表 ==========

    @Test
    void getLabList_ShouldReturnPage() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.LAB.getCode());

        when(examinationService.getLabList(eq("待检查"), eq(1), eq(10)))
                .thenReturn(new PageImpl<>(List.of(new Examination())));

        mockMvc.perform(get("/api/examination/lab/list")
                        .header("Authorization", "Bearer lab-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void getLabList_ShouldReturnAll_WhenStatusIsEmpty() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.LAB.getCode());

        when(examinationService.getLabList(eq(""), eq(1), eq(10)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/examination/lab/list")
                        .param("status", "")
                        .header("Authorization", "Bearer lab-token"))
                .andExpect(status().isOk());
    }

    // ========== 检验科-填写结果 ==========

    @Test
    void updateResult_ShouldSucceed() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.LAB.getCode());

        when(examinationService.updateResult(1L, "结果正常", null))
                .thenReturn("检查结果已填写");

        mockMvc.perform(put("/api/examination/update-result")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"result\":\"结果正常\"}")
                        .header("Authorization", "Bearer lab-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("检查结果已填写"));
    }

    @Test
    void updateResult_ShouldReturn500_WhenNotPending() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.LAB.getCode());

        when(examinationService.updateResult(1L, "正常", null))
                .thenThrow(new BusinessException("当前状态不能填写结果"));

        mockMvc.perform(put("/api/examination/update-result")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"result\":\"正常\"}")
                        .header("Authorization", "Bearer lab-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ========== 检查项目列表 ==========

    @Test
    void getItemList_ShouldReturnAllItems() throws Exception {
        ExaminationItem item = new ExaminationItem();
        item.setId(1L);
        item.setName("血常规");
        item.setType("检验");
        item.setPrice(BigDecimal.valueOf(30));

        when(examinationService.getItemList(isNull())).thenReturn(List.of(item));

        mockMvc.perform(get("/api/examination/item/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("血常规"));
    }

    @Test
    void getItemList_ShouldFilterByType() throws Exception {
        when(examinationService.getItemList("检查"))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/examination/item/list")
                        .param("type", "检查"))
                .andExpect(status().isOk());
    }
}
