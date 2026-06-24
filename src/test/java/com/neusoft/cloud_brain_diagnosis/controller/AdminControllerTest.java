package com.neusoft.cloud_brain_diagnosis.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.util.JwtUtil;
import com.neusoft.cloud_brain_diagnosis.entity.Admin;
import com.neusoft.cloud_brain_diagnosis.service.AdminService;
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
 * AdminController Web层测试
 * 覆盖：登录、注册、信息获取、密码修改、统计
 */
@WebMvcTest(AdminController.class)
class AdminControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private AdminService adminService;
    @MockBean private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        when(jwtUtil.validateToken(anyString())).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(1L);
    }

    @Test
    void login_ShouldReturnToken() throws Exception {
        when(adminService.login("admin", "123456")).thenReturn("mock-token");

        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("mock-token"));
    }

    @Test
    void register_ShouldSucceed() throws Exception {
        when(adminService.register("newadmin", "123456", "管理员"))
                .thenReturn("注册成功");

        mockMvc.perform(post("/api/admin/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newadmin\",\"password\":\"123456\",\"name\":\"管理员\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("注册成功"));
    }

    @Test
    void getAdminInfo_ShouldReturnInfo() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.ADMIN.getCode());

        Admin admin = new Admin();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setName("管理员");

        when(adminService.getAdminInfo(1L)).thenReturn(admin);

        mockMvc.perform(get("/api/admin/info")
                        .header("Authorization", "Bearer mock-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("admin"));
    }

    @Test
    void getOverviewStatistics_ShouldReturnStats() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.ADMIN.getCode());

        when(adminService.getOverviewStatistics())
                .thenReturn(Map.of("patientCount", 10L, "doctorCount", 5L));

        mockMvc.perform(get("/api/admin/statistics/overview")
                        .header("Authorization", "Bearer mock-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.patientCount").value(10));
    }

    @Test
    void changePassword_ShouldSucceed() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.ADMIN.getCode());

        when(adminService.changePassword(1L, "old123", "new123"))
                .thenReturn("密码修改成功");

        mockMvc.perform(put("/api/admin/change-pwd")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"old123\",\"newPassword\":\"new123\"}")
                        .header("Authorization", "Bearer mock-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("密码修改成功"));
    }

    @Test
    void changePassword_ShouldReturn500_WhenOldPasswordIsWrong() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.ADMIN.getCode());

        when(adminService.changePassword(1L, "wrong", "new123"))
                .thenThrow(new com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException("原密码错误"));

        mockMvc.perform(put("/api/admin/change-pwd")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"wrong\",\"newPassword\":\"new123\"}")
                        .header("Authorization", "Bearer mock-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("原密码错误"));
    }

    @Test
    void changePassword_ShouldReturn500_WhenOldPasswordIsNull() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.ADMIN.getCode());

        // adminId 为空时 service 抛出异常
        when(adminService.changePassword(eq(1L), isNull(), anyString()))
                .thenThrow(new com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException("管理员不存在"));

        mockMvc.perform(put("/api/admin/change-pwd")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":null,\"newPassword\":\"new123\"}")
                        .header("Authorization", "Bearer mock-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }
}
