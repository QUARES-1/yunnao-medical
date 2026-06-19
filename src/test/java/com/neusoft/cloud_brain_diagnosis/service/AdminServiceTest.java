package com.neusoft.cloud_brain_diagnosis.service;

import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.common.util.JwtUtil;
import com.neusoft.cloud_brain_diagnosis.entity.Admin;
import com.neusoft.cloud_brain_diagnosis.repository.*;
import com.neusoft.cloud_brain_diagnosis.service.impl.AdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AdminService 单元测试
 * 覆盖：登录、注册、修改密码、获取信息、统计
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private AdminRepository adminRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private RegistrationRepository registrationRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private JwtUtil jwtUtil;

    private AdminServiceImpl adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminServiceImpl(
                adminRepository, patientRepository, doctorRepository,
                registrationRepository, departmentRepository, jwtUtil);
    }

    // ========== 登录 ==========

    @Test
    void login_ShouldReturnToken_WhenCredentialsValid() {
        Admin admin = new Admin();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setPassword("123456");

        when(adminRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(jwtUtil.generateToken(1L, RoleEnum.ADMIN.getCode())).thenReturn("mock-token");

        String token = adminService.login("admin", "123456");
        assertEquals("mock-token", token);
    }

    @Test
    void login_ShouldThrow_WhenUsernameNotFound() {
        when(adminRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminService.login("unknown", "pass"));
        assertEquals("账号不存在", ex.getMessage());
    }

    @Test
    void login_ShouldThrow_WhenPasswordWrong() {
        Admin admin = new Admin();
        admin.setUsername("admin");
        admin.setPassword("correct");

        when(adminRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminService.login("admin", "wrong"));
        assertEquals("密码错误", ex.getMessage());
    }

    // ========== 获取信息 ==========

    @Test
    void getAdminInfo_ShouldReturnAdminWithoutPassword() {
        Admin admin = new Admin();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setPassword("secret");
        admin.setName("管理员");

        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));

        Admin result = adminService.getAdminInfo(1L);
        assertEquals("admin", result.getUsername());
        assertNull(result.getPassword()); // 密码应被置空
    }

    @Test
    void getAdminInfo_ShouldThrow_WhenNotFound() {
        when(adminRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> adminService.getAdminInfo(99L));
    }

    // ========== 修改密码 ==========

    @Test
    void changePassword_ShouldSucceed_WhenOldPasswordCorrect() {
        Admin admin = new Admin();
        admin.setId(1L);
        admin.setPassword("oldPass");

        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(adminRepository.save(any())).thenReturn(admin);

        String result = adminService.changePassword(1L, "oldPass", "newPass");
        assertEquals("密码修改成功", result);
        assertEquals("newPass", admin.getPassword());
    }

    @Test
    void changePassword_ShouldThrow_WhenOldPasswordWrong() {
        Admin admin = new Admin();
        admin.setId(1L);
        admin.setPassword("oldPass");

        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThrows(BusinessException.class,
                () -> adminService.changePassword(1L, "wrongOld", "newPass"));
    }

    // ========== 注册 ==========

    @Test
    void register_ShouldSucceed() {
        when(adminRepository.findByUsername("newAdmin")).thenReturn(Optional.empty());
        when(adminRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = adminService.register("newAdmin", "123456", "新管理员");
        assertEquals("注册成功", result);
    }

    @Test
    void register_ShouldThrow_WhenUsernameExists() {
        when(adminRepository.findByUsername("exists")).thenReturn(Optional.of(new Admin()));
        assertThrows(BusinessException.class,
                () -> adminService.register("exists", "123456", "test"));
    }

    @Test
    void register_ShouldThrow_WhenUsernameTooShort() {
        assertThrows(BusinessException.class,
                () -> adminService.register("ab", "123456", "test"));
    }

    @Test
    void register_ShouldThrow_WhenPasswordTooShort() {
        assertThrows(BusinessException.class,
                () -> adminService.register("abc", "12345", "test"));
    }

    // ========== 统计 ==========

    @Test
    void getOverviewStatistics_ShouldReturnAllCounts() {
        when(patientRepository.count()).thenReturn(10L);
        when(doctorRepository.count()).thenReturn(5L);
        when(departmentRepository.count()).thenReturn(8L);
        when(registrationRepository.count()).thenReturn(100L);

        Map<String, Object> stats = adminService.getOverviewStatistics();
        assertEquals(10L, stats.get("patientCount"));
        assertEquals(5L, stats.get("doctorCount"));
        assertEquals(8L, stats.get("departmentCount"));
        assertEquals(100L, stats.get("registrationCount"));
    }
}
