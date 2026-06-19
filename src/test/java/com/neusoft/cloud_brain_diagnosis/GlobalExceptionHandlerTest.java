package com.neusoft.cloud_brain_diagnosis;

import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * GlobalExceptionHandler 单元测试
 * 覆盖：BusinessException、参数校验异常、通用异常
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBusinessException_ShouldReturnErrorResult() {
        BusinessException ex = new BusinessException("业务异常测试");
        var result = handler.handleBusinessException(ex);

        assertEquals(500, result.getCode());
        assertEquals("业务异常测试", result.getMsg());
    }

    @Test
    void handleMethodArgumentNotValidException_ShouldReturnFieldErrorMessage() throws Exception {
        // 模拟MethodArgumentNotValidException
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("obj", "name", "名称不能为空");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldError()).thenReturn(fieldError);

        var result = handler.handleValidException(ex);
        assertEquals(500, result.getCode());
        assertEquals("名称不能为空", result.getMsg());
    }

    @Test
    void handleMethodArgumentNotValidException_ShouldReturnDefaultMessage_WhenNoFieldError() throws Exception {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldError()).thenReturn(null);

        var result = handler.handleValidException(ex);
        assertEquals(500, result.getCode());
        assertEquals("参数校验失败", result.getMsg());
    }

    @Test
    void handleBindException_ShouldReturnFieldErrorMessage() {
        BindException ex = mock(BindException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("obj", "age", "年龄不能为负");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldError()).thenReturn(fieldError);

        var result = handler.handleBindException(ex);
        assertEquals(500, result.getCode());
        assertEquals("年龄不能为负", result.getMsg());
    }

    @Test
    void handleBindException_ShouldReturnDefaultMessage_WhenNoFieldError() {
        BindException ex = mock(BindException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldError()).thenReturn(null);

        var result = handler.handleBindException(ex);
        assertEquals(500, result.getCode());
        assertEquals("参数绑定失败", result.getMsg());
    }

    @Test
    void handleGenericException_ShouldReturnErrorMessage() {
        Exception ex = new RuntimeException("未知错误");

        var result = handler.handleException(ex);
        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("未知错误"));
    }
}
