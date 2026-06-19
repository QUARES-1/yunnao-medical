package com.neusoft.cloud_brain_diagnosis.common;

import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Result 通用响应封装测试
 */
class ResultTest {

    @Test
    void testSuccessWithData() {
        Result<String> result = Result.success("hello");
        assertEquals(200, result.getCode());
        assertEquals("操作成功", result.getMsg());
        assertEquals("hello", result.getData());
    }

    @Test
    void testSuccessWithoutData() {
        Result<Object> result = Result.success();
        assertEquals(200, result.getCode());
        assertEquals("操作成功", result.getMsg());
        assertNull(result.getData());
    }

    @Test
    void testError() {
        Result<Object> result = Result.error("出错了");
        assertEquals(500, result.getCode());
        assertEquals("出错了", result.getMsg());
        assertNull(result.getData());
    }

    @Test
    void testUnauthorized() {
        Result<Object> result = Result.unauthorized("请先登录");
        assertEquals(401, result.getCode());
        assertEquals("请先登录", result.getMsg());
        assertNull(result.getData());
    }

    @Test
    void testForbidden() {
        Result<Object> result = Result.forbidden("无权限");
        assertEquals(403, result.getCode());
        assertEquals("无权限", result.getMsg());
        assertNull(result.getData());
    }

    @Test
    void testSetAndGet() {
        Result<String> result = new Result<>();
        result.setCode(200);
        result.setMsg("OK");
        result.setData("data");
        assertEquals(200, result.getCode());
        assertEquals("OK", result.getMsg());
        assertEquals("data", result.getData());
    }
}
