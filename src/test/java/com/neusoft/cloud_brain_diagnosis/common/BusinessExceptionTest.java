package com.neusoft.cloud_brain_diagnosis.common;

import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BusinessException 业务异常测试
 */
class BusinessExceptionTest {

    @Test
    void testExceptionMessage() {
        BusinessException ex = new BusinessException("账号不存在");
        assertEquals("账号不存在", ex.getMessage());
    }

    @Test
    void testIsRuntimeException() {
        BusinessException ex = new BusinessException("test");
        assertInstanceOf(RuntimeException.class, ex);
    }

    @Test
    void testThrowAndCatch() {
        try {
            throw new BusinessException("密码错误");
        } catch (BusinessException e) {
            assertEquals("密码错误", e.getMessage());
        }
    }

    @Test
    void testExceptionWithNullMessage() {
        BusinessException ex = new BusinessException(null);
        assertNull(ex.getMessage());
    }
}
