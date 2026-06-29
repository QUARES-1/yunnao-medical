package com.neusoft.cloud_brain_diagnosis.service;

import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.entity.StaffAccount;
import com.neusoft.cloud_brain_diagnosis.repository.StaffAccountRepository;
import com.neusoft.cloud_brain_diagnosis.service.impl.StaffAccountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * StaffAccountService 白盒单元测试 — 药房/检验人员账号管理
 * 覆盖：登录(明文/BCrypt兼容)、信息查询、密码修改、账号创建
 */
@ExtendWith(MockitoExtension.class)
class StaffAccountServiceImplTest {

    @Mock private StaffAccountRepository staffAccountRepository;
    @Mock private com.neusoft.cloud_brain_diagnosis.common.util.JwtUtil jwtUtil;

    private StaffAccountServiceImpl staffAccountService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        staffAccountService = new StaffAccountServiceImpl(staffAccountRepository, jwtUtil);
    }

    // ========== 登录 ==========

    @Test
    void login_ShouldReturnToken_WithPlainPassword() {
        StaffAccount account = new StaffAccount();
        account.setId(1L);
        account.setUsername("staff1");
        account.setPassword("plain123"); // 明文密码（兼容旧数据）
        account.setEnabled(true);
        account.setRole("pharmacy");

        when(staffAccountRepository.findByUsername("staff1")).thenReturn(Optional.of(account));
        when(jwtUtil.generateToken(1L, "pharmacy")).thenReturn("mock-token");

        String token = staffAccountService.login("staff1", "plain123", "pharmacy");
        assertEquals("mock-token", token);
        // 明文密码应在首次登录后升级为 BCrypt
        assertTrue(account.getPassword().startsWith("$2"));
    }

    @Test
    void login_ShouldReturnToken_WithBcryptPassword() {
        String encoded = encoder.encode("password123");
        StaffAccount account = new StaffAccount();
        account.setId(1L);
        account.setUsername("staff1");
        account.setPassword(encoded);
        account.setEnabled(true);
        account.setRole("pharmacy");

        when(staffAccountRepository.findByUsername("staff1")).thenReturn(Optional.of(account));
        when(jwtUtil.generateToken(1L, "pharmacy")).thenReturn("mock-token");

        String token = staffAccountService.login("staff1", "password123", "pharmacy");
        assertEquals("mock-token", token);
        // BCrypt 密码不应被重复加密
        assertEquals(encoded, account.getPassword());
        verify(staffAccountRepository, never()).save(any());
    }

    @Test
    void login_ShouldThrow_WhenUsernameIsNull() {
        assertThrows(BusinessException.class,
                () -> staffAccountService.login(null, "pass", "pharmacy"));
    }

    @Test
    void login_ShouldThrow_WhenUsernameIsBlank() {
        assertThrows(BusinessException.class,
                () -> staffAccountService.login("   ", "pass", "pharmacy"));
    }

    @Test
    void login_ShouldThrow_WhenPasswordIsNull() {
        assertThrows(BusinessException.class,
                () -> staffAccountService.login("staff1", null, "pharmacy"));
    }

    @Test
    void login_ShouldThrow_WhenPasswordIsBlank() {
        assertThrows(BusinessException.class,
                () -> staffAccountService.login("staff1", "   ", "pharmacy"));
    }

    @Test
    void login_ShouldThrow_WhenUsernameNotFound() {
        when(staffAccountRepository.findByUsername("unknown")).thenReturn(Optional.empty());
        assertThrows(BusinessException.class,
                () -> staffAccountService.login("unknown", "pass", "pharmacy"));
    }

    @Test
    void login_ShouldThrow_WhenAccountDisabled() {
        StaffAccount account = new StaffAccount();
        account.setId(1L);
        account.setUsername("staff1");
        account.setPassword("pass");
        account.setEnabled(false);

        when(staffAccountRepository.findByUsername("staff1")).thenReturn(Optional.of(account));
        assertThrows(BusinessException.class,
                () -> staffAccountService.login("staff1", "pass", "pharmacy"));
    }

    @Test
    void login_ShouldThrow_WhenRoleMismatch() {
        StaffAccount account = new StaffAccount();
        account.setId(1L);
        account.setUsername("staff1");
        account.setPassword("pass");
        account.setEnabled(true);
        account.setRole("lab");

        when(staffAccountRepository.findByUsername("staff1")).thenReturn(Optional.of(account));
        assertThrows(BusinessException.class,
                () -> staffAccountService.login("staff1", "pass", "pharmacy"));
    }

    @Test
    void login_ShouldSucceed_WhenRoleIsNull() {
        // role 为 null 时不进行角色校验（兼容部分场景）
        StaffAccount account = new StaffAccount();
        account.setId(1L);
        account.setUsername("staff1");
        account.setPassword("pass");
        account.setEnabled(true);
        account.setRole("pharmacy");

        when(staffAccountRepository.findByUsername("staff1")).thenReturn(Optional.of(account));
        when(jwtUtil.generateToken(1L, "pharmacy")).thenReturn("token");

        String token = staffAccountService.login("staff1", "pass", null);
        assertEquals("token", token);
    }

    @Test
    void login_ShouldThrow_WhenPasswordWrong_WithPlainPassword() {
        StaffAccount account = new StaffAccount();
        account.setId(1L);
        account.setUsername("staff1");
        account.setPassword("correctpass");
        account.setEnabled(true);
        account.setRole("pharmacy");

        when(staffAccountRepository.findByUsername("staff1")).thenReturn(Optional.of(account));
        assertThrows(BusinessException.class,
                () -> staffAccountService.login("staff1", "wrongpass", "pharmacy"));
    }

    @Test
    void login_ShouldThrow_WhenPasswordWrong_WithBcryptPassword() {
        String encoded = encoder.encode("correctpass");
        StaffAccount account = new StaffAccount();
        account.setId(1L);
        account.setUsername("staff1");
        account.setPassword(encoded);
        account.setEnabled(true);
        account.setRole("pharmacy");

        when(staffAccountRepository.findByUsername("staff1")).thenReturn(Optional.of(account));
        assertThrows(BusinessException.class,
                () -> staffAccountService.login("staff1", "wrongpass", "pharmacy"));
    }

    // ========== 获取信息 ==========

    @Test
    void getInfo_ShouldReturnAccountWithoutPassword() {
        StaffAccount account = new StaffAccount();
        account.setId(1L);
        account.setUsername("staff1");
        account.setPassword("secret");
        account.setName("张药房");

        when(staffAccountRepository.findById(1L)).thenReturn(Optional.of(account));

        StaffAccount result = staffAccountService.getInfo(1L);
        assertEquals("staff1", result.getUsername());
        assertEquals("张药房", result.getName());
        assertNull(result.getPassword());
    }

    @Test
    void getInfo_ShouldThrow_WhenNotFound() {
        when(staffAccountRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> staffAccountService.getInfo(99L));
    }

    // ========== 修改密码 ==========

    @Test
    void changePassword_ShouldSucceed() {
        String encoded = encoder.encode("oldpass");
        StaffAccount account = new StaffAccount();
        account.setId(1L);
        account.setPassword(encoded);

        when(staffAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(staffAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = staffAccountService.changePassword(1L, "oldpass", "newpass123");
        assertEquals("密码修改成功", result);
        assertNotEquals(encoded, account.getPassword());
        assertTrue(encoder.matches("newpass123", account.getPassword()));
    }

    @Test
    void changePassword_ShouldThrow_WhenNewPasswordTooShort() {
        assertThrows(BusinessException.class,
                () -> staffAccountService.changePassword(1L, "oldpass", "123"));
    }

    @Test
    void changePassword_ShouldThrow_WhenOldPasswordWrong() {
        String encoded = encoder.encode("correctpass");
        StaffAccount account = new StaffAccount();
        account.setId(1L);
        account.setPassword(encoded);

        when(staffAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        assertThrows(BusinessException.class,
                () -> staffAccountService.changePassword(1L, "wrongpass", "newpass123"));
    }

    @Test
    void changePassword_ShouldThrow_WhenAccountNotFound() {
        when(staffAccountRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class,
                () -> staffAccountService.changePassword(99L, "oldpass", "newpass123"));
    }

    @Test
    void changePassword_ShouldHandlePlainPassword() {
        StaffAccount account = new StaffAccount();
        account.setId(1L);
        account.setPassword("plainold");

        when(staffAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(staffAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = staffAccountService.changePassword(1L, "plainold", "newpass123");
        assertEquals("密码修改成功", result);
        assertTrue(account.getPassword().startsWith("$2"));
    }

    // ========== 创建账号 ==========

    @Test
    void create_ShouldSucceed_WithPharmacyRole() {
        StaffAccount input = new StaffAccount();
        input.setUsername("newstaff");
        input.setPassword("pass123");
        input.setRole("pharmacy");
        input.setName("新员工");

        when(staffAccountRepository.existsByUsername("newstaff")).thenReturn(false);
        when(staffAccountRepository.save(any())).thenAnswer(inv -> {
            StaffAccount saved = inv.getArgument(0);
            // 模拟 JPA 自动生成 ID
            if (saved.getId() == null) {
                try {
                    java.lang.reflect.Field idField = StaffAccount.class.getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(saved, 1L);
                } catch (Exception ignored) {}
            }
            return saved;
        });

        StaffAccount result = staffAccountService.create(input);
        assertNotNull(result.getId());
        assertTrue(result.getPassword().startsWith("$2"));
        assertEquals("pharmacy", result.getRole());
    }

    @Test
    void create_ShouldSucceed_WithLabRole() {
        StaffAccount input = new StaffAccount();
        input.setUsername("labstaff");
        input.setPassword("pass123");
        input.setRole("lab");

        when(staffAccountRepository.existsByUsername("labstaff")).thenReturn(false);
        when(staffAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StaffAccount result = staffAccountService.create(input);
        assertEquals("lab", result.getRole());
    }

    @Test
    void create_ShouldThrow_WhenUsernameTooShort() {
        StaffAccount input = new StaffAccount();
        input.setUsername("ab");
        input.setPassword("pass123");
        input.setRole("pharmacy");

        assertThrows(BusinessException.class, () -> staffAccountService.create(input));
    }

    @Test
    void create_ShouldThrow_WhenUsernameAlreadyExists() {
        StaffAccount input = new StaffAccount();
        input.setUsername("existing");
        input.setPassword("pass123");
        input.setRole("pharmacy");

        when(staffAccountRepository.existsByUsername("existing")).thenReturn(true);
        assertThrows(BusinessException.class, () -> staffAccountService.create(input));
    }

    @Test
    void create_ShouldThrow_WhenRoleIsInvalid() {
        StaffAccount input = new StaffAccount();
        input.setUsername("newstaff");
        input.setPassword("pass123");
        input.setRole("doctor");

        when(staffAccountRepository.existsByUsername("newstaff")).thenReturn(false);
        assertThrows(BusinessException.class, () -> staffAccountService.create(input));
    }

    @Test
    void create_ShouldThrow_WhenPasswordTooShort() {
        StaffAccount input = new StaffAccount();
        input.setUsername("newstaff");
        input.setPassword("12345");
        input.setRole("pharmacy");

        when(staffAccountRepository.existsByUsername("newstaff")).thenReturn(false);
        assertThrows(BusinessException.class, () -> staffAccountService.create(input));
    }
}
