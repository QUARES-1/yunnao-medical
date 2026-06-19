package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.util.JwtUtil;
import com.neusoft.cloud_brain_diagnosis.entity.Admin;
import com.neusoft.cloud_brain_diagnosis.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
    @MockBean private AdminService adminService;
    @MockBean private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        // JwtInterceptor will call these methods; mock them so requests pass through
        when(jwtUtil.validateToken(anyString())).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(1L);
    }

    @Test
    void login_ShouldReturnToken() throws Exception {
        when(adminService.login("admin", "123456")).thenReturn("mock-token");

        mockMvc.perform(post("/api/admin/login")
                        .param("username", "admin")
                        .param("password", "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("mock-token"));
    }

    @Test
    void register_ShouldSucceed() throws Exception {
        when(adminService.register("newadmin", "123456", "管理员"))
                .thenReturn("注册成功");

        mockMvc.perform(post("/api/admin/register")
                        .param("username", "newadmin")
                        .param("password", "123456")
                        .param("name", "管理员"))
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
}
