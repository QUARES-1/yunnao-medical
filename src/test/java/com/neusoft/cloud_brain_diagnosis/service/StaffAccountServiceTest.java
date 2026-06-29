package com.neusoft.cloud_brain_diagnosis.service;

import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.common.util.JwtUtil;
import com.neusoft.cloud_brain_diagnosis.entity.StaffAccount;
import com.neusoft.cloud_brain_diagnosis.repository.StaffAccountRepository;
import com.neusoft.cloud_brain_diagnosis.service.impl.StaffAccountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * StaffAccountService 白盒单元测试
 * 覆盖：登录、获取信息、改密、创建账号
 */
@ExtendWith(MockitoExtension.class)
class StaffAccountServiceTest {

    @Mock
    private StaffAccountRepository repository;

    @Mock
    private JwtUtil jwtUtil;

    private StaffAccountServiceImpl staffAccountService;

    @BeforeEach
    void setUp() {
        staffAccountService = new StaffAccountServiceImpl(repository, jwtUtil);
    }

    // ========== 登录 ==========

    @Test
    void login_ShouldSucceed_ForPharmacy() {
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
            new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        String encodedPw = encoder.encode("password");

        StaffAccount account = new StaffAccount();
        account.setId(1L);
        account.setUsername("pharmacy1");
        account.setPassword(encodedPw); // 正确编码的密码
        account.setRole("pharmacy");
        account.setEnabled(true);

        when(repository.findByUsername("pharmacy1")).thenReturn(Optional.of(account));
        when(jwtUtil.generateToken(1L, "pharmacy")).thenReturn("token");

        String token = staffAccountService.login("pharmacy1", "password", "pharmacy");
        assertEquals("token", token);
    }

    @Test
    void login_ShouldSucceed_WhenPlainTextPasswordMatches() {
        // 明文密码（未加密），触发自动升级 BCrypt 分支
        StaffAccount account = new StaffAccount();
        account.setId(1L);
        account.setUsername("staff1");
        account.setPassword("plaintext_pass"); // 非 BCrypt 格式
        account.setRole("lab");
        account.setEnabled(true);

        when(repository.findByUsername("staff1")).thenReturn(Optional.of(account));
        when(repository.save(any())).thenReturn(account);
        when(jwtUtil.generateToken(1L, "lab")).thenReturn("token_upgraded");

        String token = staffAccountService.login("staff1", "plaintext_pass", "lab");
        assertEquals("token_upgraded", token);
        // 验证密码被升级为 BCrypt
        assertTrue(account.getPassword().startsWith("$2"));
    }

    @Test
    void login_ShouldThrow_WhenAccountNotFound() {
        when(repository.findByUsername("notexist")).thenReturn(Optional.empty());
        assertThrows(BusinessException.class,
                () -> staffAccountService.login("notexist", "pass", "pharmacy"));
    }

    @Test
    void login_ShouldThrow_WhenWrongPassword() {
        StaffAccount account = new StaffAccount();
        account.setId(1L);
        account.setUsername("user1");
        account.setPassword("$2a$10$encoded");
        account.setRole("pharmacy");
        account.setEnabled(true);

        when(repository.findByUsername("user1")).thenReturn(Optional.of(account));
        assertThrows(BusinessException.class,
                () -> staffAccountService.login("user1", "wrongpass", "pharmacy"));
    }

    @Test
    void login_ShouldThrow_WhenAccountDisabled() {
        StaffAccount account = new StaffAccount();
        account.setId(1L);
        account.setUsername("disabled");
        account.setPassword("pass");
        account.setRole("pharmacy");
        account.setEnabled(false);

        when(repository.findByUsername("disabled")).thenReturn(Optional.of(account));
        assertThrows(BusinessException.class,
                () -> staffAccountService.login("disabled", "pass", "pharmacy"));
    }

    @Test
    void login_ShouldThrow_WhenRoleMismatch() {
        StaffAccount account = new StaffAccount();
        account.setId(1L);
        account.setUsername("user1");
        account.setPassword("pass");
        account.setRole("pharmacy");
        account.setEnabled(true);

        when(repository.findByUsername("user1")).thenReturn(Optional.of(account));
        assertThrows(BusinessException.class,
                () -> staffAccountService.login("user1", "pass", "lab"));
    }

    @Test
    void login_ShouldThrow_WhenUsernameOrPasswordBlank() {
        assertThrows(BusinessException.class,
                () -> staffAccountService.login("", "pass", "pharmacy"));
        assertThrows(BusinessException.class,
                () -> staffAccountService.login("user", "", "pharmacy"));
        assertThrows(BusinessException.class,
                () -> staffAccountService.login(null, "pass", "pharmacy"));
        assertThrows(BusinessException.class,
                () -> staffAccountService.login("user", null, "pharmacy"));
    }

    // ========== 获取信息 ==========

    @Test
    void getInfo_ShouldReturnAccount_WithoutPassword() {
        StaffAccount account = new StaffAccount();
        account.setId(1L);
        account.setUsername("pharmacy1");
        account.setPassword("$2a$10$encoded");
        account.setRole("pharmacy");

        when(repository.findById(1L)).thenReturn(Optional.of(account));

        StaffAccount result = staffAccountService.getInfo(1L);
        assertEquals(1L, result.getId());
        assertNull(result.getPassword());
    }

    @Test
    void getInfo_ShouldThrow_WhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> staffAccountService.getInfo(99L));
    }

    // ========== 改密 ==========

    @Test
    void changePassword_ShouldSucceed_WithBCryptOldPassword() {
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
            new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        String encodedOld = encoder.encode("old_pass");

        StaffAccount account = new StaffAccount();
        account.setId(1L);
        account.setPassword(encodedOld);

        when(repository.findById(1L)).thenReturn(Optional.of(account));
        when(repository.save(any())).thenReturn(account);

        String result = staffAccountService.changePassword(1L, "old_pass", "new_pass");
        assertEquals("密码修改成功", result);
        assertTrue(account.getPassword().startsWith("$2"));
    }

    @Test
    void changePassword_ShouldSucceed_WithPlainTextOldPassword() {
        // 明文旧密码（非 BCrypt 格式），触发 line 59 明文匹配分支
        StaffAccount account = new StaffAccount();
        account.setId(1L);
        account.setPassword("plain_old_pass"); // 非 BCrypt 格式

        when(repository.findById(1L)).thenReturn(Optional.of(account));
        when(repository.save(any())).thenReturn(account);

        String result = staffAccountService.changePassword(1L, "plain_old_pass", "new_pass");
        assertEquals("密码修改成功", result);
        assertTrue(account.getPassword().startsWith("$2"));
    }

    @Test
    void changePassword_ShouldThrow_WhenOldPasswordWrong() {
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
            new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        String encodedOld = encoder.encode("correct_old");

        StaffAccount account = new StaffAccount();
        account.setId(1L);
        account.setPassword(encodedOld);

        when(repository.findById(1L)).thenReturn(Optional.of(account));
        assertThrows(BusinessException.class,
                () -> staffAccountService.changePassword(1L, "wrong_old", "new_pass"));
    }

    @Test
    void changePassword_ShouldThrow_WhenNewPasswordTooShort() {
        assertThrows(BusinessException.class,
                () -> staffAccountService.changePassword(1L, "old", "12345"));
    }

    @Test
    void changePassword_ShouldThrow_WhenNewPasswordNull() {
        assertThrows(BusinessException.class,
                () -> staffAccountService.changePassword(1L, "old", null));
    }

    @Test
    void changePassword_ShouldThrow_WhenAccountNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class,
                () -> staffAccountService.changePassword(99L, "old", "new_pass"));
    }

    // ========== 创建账号 ==========

    @Test
    void create_ShouldSucceed() {
        StaffAccount account = new StaffAccount();
        account.setUsername("newpharmacy");
        account.setPassword("password123");
        account.setRole("pharmacy");

        when(repository.existsByUsername("newpharmacy")).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StaffAccount result = staffAccountService.create(account);
        assertNotNull(result.getPassword());
        assertTrue(result.getPassword().startsWith("$2"));
    }

    @Test
    void create_ShouldThrow_WhenUsernameExists() {
        StaffAccount account = new StaffAccount();
        account.setUsername("exists");
        account.setPassword("password123");
        account.setRole("pharmacy");

        when(repository.existsByUsername("exists")).thenReturn(true);
        assertThrows(BusinessException.class, () -> staffAccountService.create(account));
    }

    @Test
    void create_ShouldThrow_WhenInvalidRole() {
        StaffAccount account = new StaffAccount();
        account.setUsername("user1");
        account.setPassword("password123");
        account.setRole("admin"); // 非 pharmacy/lab

        assertThrows(BusinessException.class, () -> staffAccountService.create(account));
    }

    @Test
    void create_ShouldThrow_WhenPasswordTooShort() {
        StaffAccount account = new StaffAccount();
        account.setUsername("user1");
        account.setPassword("12345"); // 不足6位
        account.setRole("pharmacy");

        assertThrows(BusinessException.class, () -> staffAccountService.create(account));
    }

    @Test
    void create_ShouldThrow_WhenUsernameTooShort() {
        StaffAccount account = new StaffAccount();
        account.setUsername("ab"); // 不足3位
        account.setPassword("password123");
        account.setRole("pharmacy");

        assertThrows(BusinessException.class, () -> staffAccountService.create(account));
    }
}
