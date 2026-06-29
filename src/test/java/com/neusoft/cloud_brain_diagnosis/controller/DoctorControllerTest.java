package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.util.JwtUtil;
import com.neusoft.cloud_brain_diagnosis.entity.Doctor;
import com.neusoft.cloud_brain_diagnosis.service.DoctorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * DoctorController Web层测试
 * 覆盖：公开接口、医生端接口、管理员端接口
 */
@WebMvcTest(DoctorController.class)
class DoctorControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private DoctorService doctorService;
    @MockBean private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        when(jwtUtil.validateToken(anyString())).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(1L);
    }

    @Test
    void login_ShouldReturnToken() throws Exception {
        when(doctorService.login("doc1", "123456")).thenReturn("doc-token");

        mockMvc.perform(post("/api/doctor/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"doc1\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("doc-token"));
    }

    @Test
    void register_ShouldReturnToken() throws Exception {
        when(doctorService.register("newdoc", "123456", "医生"))
                .thenReturn("reg-token");

        mockMvc.perform(post("/api/doctor/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newdoc\",\"password\":\"123456\",\"name\":\"医生\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("reg-token"));
    }

    @Test
    void getDoctorList_ShouldReturnList() throws Exception {
        Doctor doc = new Doctor();
        doc.setId(1L);
        doc.setName("李医生");

        when(doctorService.getDoctorList(null)).thenReturn(List.of(doc));

        mockMvc.perform(get("/api/doctor/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("李医生"));
    }

    @Test
    void getDoctorDetail_ShouldReturnDetail() throws Exception {
        Doctor doc = new Doctor();
        doc.setId(1L);
        doc.setName("李医生");

        when(doctorService.getDoctorDetail(1L)).thenReturn(doc);

        mockMvc.perform(get("/api/doctor/detail/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("李医生"));
    }

    @Test
    void getSchedule_ShouldReturnSlots() throws Exception {
        when(doctorService.getSchedule(1L))
                .thenReturn(java.util.Map.of("dates", List.of(), "timeSlots", List.of("上午", "下午")));

        mockMvc.perform(get("/api/doctor/1/schedule"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.timeSlots[0]").value("上午"));
    }

    @Test
    void addDoctor_ShouldSucceed_WhenAdminLoggedIn() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.ADMIN.getCode());

        when(doctorService.addDoctor(any())).thenReturn("医生添加成功");

        mockMvc.perform(post("/api/doctor/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newdoc\",\"name\":\"新医生\",\"password\":\"123456\"}")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("医生添加成功"));
    }

    // ========== 医生端接口 ==========

    @Test
    void getDoctorInfo_ShouldReturnInfo() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(10L);

        Doctor doc = new Doctor();
        doc.setId(10L);
        doc.setName("李医生");
        when(doctorService.getDoctorInfo(10L)).thenReturn(doc);

        mockMvc.perform(get("/api/doctor/info")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("李医生"));
    }

    @Test
    void updateDoctorInfo_ShouldSucceed() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(10L);

        when(doctorService.updateDoctorInfo(any())).thenReturn("信息更新成功");

        mockMvc.perform(put("/api/doctor/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"新名字\",\"phone\":\"13800138000\"}")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("信息更新成功"));
    }

    @Test
    void changePassword_ShouldSucceed() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(10L);

        when(doctorService.changePassword(10L, "old123", "new456"))
                .thenReturn("密码修改成功");

        mockMvc.perform(put("/api/doctor/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"old123\",\"newPassword\":\"new456\"}")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("密码修改成功"));
    }

    @Test
    void changePassword_ShouldFail_WhenOldPasswordIsWrong() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(10L);

        when(doctorService.changePassword(10L, "wrong", "new456"))
                .thenThrow(new com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException("旧密码错误"));

        mockMvc.perform(put("/api/doctor/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"wrong\",\"newPassword\":\"new456\"}")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ========== 管理员端接口 ==========

    @Test
    void getDoctorPage_ShouldReturnPage() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.ADMIN.getCode());

        when(doctorService.getDoctorPage(1, 10))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of(new Doctor())));

        mockMvc.perform(get("/api/doctor/page")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void resetPassword_ShouldSucceed() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.ADMIN.getCode());

        when(doctorService.resetPassword(10L)).thenReturn("密码已重置为123456");

        mockMvc.perform(put("/api/doctor/reset-pwd/10")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("密码已重置为123456"));
    }

    @Test
    void deleteDoctor_ShouldSucceed() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.ADMIN.getCode());

        when(doctorService.deleteDoctor(10L)).thenReturn("医生删除成功");

        mockMvc.perform(delete("/api/doctor/delete/10")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("医生删除成功"));
    }

    @Test
    void deleteDoctor_ShouldFail_WhenDoctorHasPendingRegistrations() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.ADMIN.getCode());

        when(doctorService.deleteDoctor(10L))
                .thenThrow(new com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException("该医生有待就诊患者，无法删除"));

        mockMvc.perform(delete("/api/doctor/delete/10")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }
}
