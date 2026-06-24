package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.common.util.JwtUtil;
import com.neusoft.cloud_brain_diagnosis.entity.StaffAccount;
import com.neusoft.cloud_brain_diagnosis.service.StaffAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * StaffAccountController Web层测试
 * 覆盖：员工账号登录、信息、修改密码、创建账号
 */
@WebMvcTest(StaffAccountController.class)
class StaffAccountControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private StaffAccountService staffAccountService;
    @MockBean private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        when(jwtUtil.validateToken(anyString())).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(1L);
    }

    // ========== 登录 ==========

    @Test
    void login_ShouldReturnToken_AsPharmacy() throws Exception {
        when(staffAccountService.login("pharmacy_user", "123456", "pharmacy"))
                .thenReturn("pharmacy-token-xyz");

        mockMvc.perform(post("/api/staff/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"pharmacy_user\",\"password\":\"123456\",\"role\":\"pharmacy\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("pharmacy-token-xyz"));
    }

    @Test
    void login_ShouldReturnToken_AsLab() throws Exception {
        when(staffAccountService.login("lab_user", "123456", "lab"))
                .thenReturn("lab-token-xyz");

        mockMvc.perform(post("/api/staff/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"lab_user\",\"password\":\"123456\",\"role\":\"lab\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("lab-token-xyz"));
    }

    @Test
    void login_ShouldReturn500_WhenAccountNotFound() throws Exception {
        when(staffAccountService.login("wrong_user", "123456", "pharmacy"))
                .thenThrow(new BusinessException("账号不存在"));

        mockMvc.perform(post("/api/staff/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"wrong_user\",\"password\":\"123456\",\"role\":\"pharmacy\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void login_ShouldReturn500_WhenPasswordIsWrong() throws Exception {
        when(staffAccountService.login("pharmacy_user", "wrongpwd", "pharmacy"))
                .thenThrow(new BusinessException("密码错误"));

        mockMvc.perform(post("/api/staff/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"pharmacy_user\",\"password\":\"wrongpwd\",\"role\":\"pharmacy\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void login_ShouldReturn500_WhenAccountDisabled() throws Exception {
        when(staffAccountService.login("disabled_user", "123456", "pharmacy"))
                .thenThrow(new BusinessException("账号已停用"));

        mockMvc.perform(post("/api/staff/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"disabled_user\",\"password\":\"123456\",\"role\":\"pharmacy\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ========== 获取信息 ==========

    @Test
    void info_ShouldReturnAccount_AsPharmacy() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PHARMACY.getCode());

        StaffAccount account = new StaffAccount();
        account.setId(1L);
        account.setUsername("pharmacy_user");
        account.setName("药房小王");
        account.setRole("pharmacy");

        when(staffAccountService.getInfo(1L)).thenReturn(account);

        mockMvc.perform(get("/api/staff/info")
                        .header("Authorization", "Bearer pharmacy-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("pharmacy_user"))
                .andExpect(jsonPath("$.data.name").value("药房小王"));
    }

    @Test
    void info_ShouldReturnAccount_AsLab() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.LAB.getCode());

        StaffAccount account = new StaffAccount();
        account.setId(2L);
        account.setUsername("lab_user");
        account.setName("检验科小李");
        account.setRole("lab");

        when(staffAccountService.getInfo(1L)).thenReturn(account);

        mockMvc.perform(get("/api/staff/info")
                        .header("Authorization", "Bearer lab-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("检验科小李"));
    }

    @Test
    void info_ShouldReturn500_WhenAccountNotFound() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PHARMACY.getCode());
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(99L);

        when(staffAccountService.getInfo(99L))
                .thenThrow(new BusinessException("工作人员不存在"));

        mockMvc.perform(get("/api/staff/info")
                        .header("Authorization", "Bearer pharmacy-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ========== 修改密码 ==========

    @Test
    void changePassword_ShouldSucceed() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PHARMACY.getCode());

        when(staffAccountService.changePassword(1L, "old123", "new456"))
                .thenReturn("密码修改成功");

        mockMvc.perform(put("/api/staff/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"old123\",\"newPassword\":\"new456\"}")
                        .header("Authorization", "Bearer pharmacy-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("密码修改成功"));
    }

    @Test
    void changePassword_ShouldReturn500_WhenOldPasswordIsWrong() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.LAB.getCode());

        when(staffAccountService.changePassword(1L, "wrong", "new456"))
                .thenThrow(new BusinessException("旧密码错误"));

        mockMvc.perform(put("/api/staff/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"wrong\",\"newPassword\":\"new456\"}")
                        .header("Authorization", "Bearer lab-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void changePassword_ShouldReturn500_WhenNewPasswordTooShort() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PHARMACY.getCode());

        when(staffAccountService.changePassword(1L, "old123", "123"))
                .thenThrow(new BusinessException("新密码长度不能少于6位"));

        mockMvc.perform(put("/api/staff/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"old123\",\"newPassword\":\"123\"}")
                        .header("Authorization", "Bearer pharmacy-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ========== 创建账号 ==========

    @Test
    void create_ShouldSucceed() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.ADMIN.getCode());

        StaffAccount created = new StaffAccount();
        created.setId(10L);
        created.setUsername("new_pharmacy");
        created.setName("新药房员工");
        created.setRole("pharmacy");

        when(staffAccountService.create(any())).thenReturn(created);

        mockMvc.perform(post("/api/staff/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"new_pharmacy\",\"password\":\"123456\",\"name\":\"新药房员工\",\"role\":\"pharmacy\"}")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("new_pharmacy"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void create_ShouldReturn500_WhenUsernameAlreadyExists() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.ADMIN.getCode());

        when(staffAccountService.create(any()))
                .thenThrow(new BusinessException("用户名已存在"));

        mockMvc.perform(post("/api/staff/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"existing_user\",\"password\":\"123456\",\"name\":\"员工\",\"role\":\"pharmacy\"}")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void create_ShouldReturn500_WhenRoleIsInvalid() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.ADMIN.getCode());

        when(staffAccountService.create(any()))
                .thenThrow(new BusinessException("角色不合法"));

        mockMvc.perform(post("/api/staff/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"new_user\",\"password\":\"123456\",\"name\":\"员工\",\"role\":\"invalid_role\"}")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }
}
