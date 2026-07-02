package com.neusoft.cloud_brain_diagnosis;

import com.neusoft.cloud_brain_diagnosis.common.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtil 集成测试
 * 覆盖：生成token、解析token、验证token
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // 注入测试密钥（至少32字节）
        ReflectionTestUtils.setField(jwtUtil, "secretKey",
                "test-secret-key-which-is-long-enough-for-hmac-sha-256!!");
    }

    @Test
    void generateToken_ShouldReturnValidToken() {
        String token = jwtUtil.generateToken(100L, "admin");
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3); // JWT 由三部分组成
    }

    @Test
    void getUserIdFromToken_ShouldReturnCorrectId() {
        String token = jwtUtil.generateToken(42L, "doctor");
        Long userId = jwtUtil.getUserIdFromToken(token);
        assertEquals(42L, userId);
    }

    @Test
    void getRoleFromToken_ShouldReturnCorrectRole() {
        String token = jwtUtil.generateToken(1L, "patient");
        String role = jwtUtil.getRoleFromToken(token);
        assertEquals("patient", role);
    }

    @Test
    void validateToken_ShouldReturnTrue_ForValidToken() {
        String token = jwtUtil.generateToken(1L, "admin");
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void validateToken_ShouldReturnFalse_ForInvalidToken() {
        assertFalse(jwtUtil.validateToken("invalid.token.here"));
    }

    @Test
    void validateToken_ShouldReturnFalse_ForEmptyString() {
        assertFalse(jwtUtil.validateToken(""));
    }

    @Test
    void validateToken_ShouldReturnFalse_ForTamperedToken() {
        String token = jwtUtil.generateToken(1L, "admin");
        // 篡改签名部分（第三段）
        String[] parts = token.split("\\.");
        String tampered = parts[0] + "." + parts[1] + ".invalidsignature";
        assertFalse(jwtUtil.validateToken(tampered));
    }

    // ========== validateToken(token, userId) ==========

    @Test
    void validateTokenWithUserId_ShouldReturnTrue_WhenUserIdMatches() {
        String token = jwtUtil.generateToken(42L, "doctor");
        assertTrue(jwtUtil.validateToken(token, 42L));
    }

    @Test
    void validateTokenWithUserId_ShouldReturnFalse_WhenUserIdDoesNotMatch() {
        String token = jwtUtil.generateToken(42L, "doctor");
        assertFalse(jwtUtil.validateToken(token, 99L));
    }

    @Test
    void validateTokenWithUserId_ShouldReturnFalse_ForInvalidToken() {
        assertFalse(jwtUtil.validateToken("invalid.token.here", 1L));
    }

    @Test
    void validateTokenWithUserId_ShouldReturnFalse_ForEmptyToken() {
        assertFalse(jwtUtil.validateToken("", 1L));
    }

    @Test
    void validateTokenWithUserId_ShouldReturnFalse_ForTamperedToken() {
        String token = jwtUtil.generateToken(42L, "admin");
        String[] parts = token.split("\\.");
        String tampered = parts[0] + "." + parts[1] + ".invalidsignature";
        assertFalse(jwtUtil.validateToken(tampered, 42L));
    }
}
