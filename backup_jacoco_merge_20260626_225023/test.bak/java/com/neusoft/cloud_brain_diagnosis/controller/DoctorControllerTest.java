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
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * DoctorController Web层测试
 * 覆盖：公开接口和需要登录的接口
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
                        .param("username", "doc1")
                        .param("password", "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("doc-token"));
    }

    @Test
    void register_ShouldReturnToken() throws Exception {
        when(doctorService.register("newdoc", "123456", "医生"))
                .thenReturn("reg-token");

        mockMvc.perform(post("/api/doctor/register")
                        .param("username", "newdoc")
                        .param("password", "123456")
                        .param("name", "医生"))
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
                .thenReturn(Map.of("dates", List.of(), "timeSlots", List.of("上午", "下午")));

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
}
