package com.neusoft.cloud_brain_diagnosis.common;

import com.neusoft.cloud_brain_diagnosis.common.context.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserContext ThreadLocal上下文测试
 */
class UserContextTest {

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void testSetAndGetUserId() {
        UserContext.setUserId(100L);
        assertEquals(100L, UserContext.getUserId());
    }

    @Test
    void testSetAndGetRole() {
        UserContext.setRole("admin");
        assertEquals("admin", UserContext.getRole());
    }

    @Test
    void testClear() {
        UserContext.setUserId(100L);
        UserContext.setRole("doctor");
        UserContext.clear();
        assertNull(UserContext.getUserId());
        assertNull(UserContext.getRole());
    }

    @Test
    void testThreadIsolation() throws InterruptedException {
        UserContext.setUserId(1L);
        UserContext.setRole("admin");

        Thread other = new Thread(() -> {
            // 另一个线程应该看不到主线程的值
            assertNull(UserContext.getUserId());
            assertNull(UserContext.getRole());

            UserContext.setUserId(2L);
            UserContext.setRole("doctor");
            assertEquals(2L, UserContext.getUserId());
            assertEquals("doctor", UserContext.getRole());
        });
        other.start();
        other.join();

        // 主线程值不变
        assertEquals(1L, UserContext.getUserId());
        assertEquals("admin", UserContext.getRole());
    }

    @Test
    void testDefaultValues() {
        assertNull(UserContext.getUserId());
        assertNull(UserContext.getRole());
    }
}
