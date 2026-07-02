package com.neusoft.cloud_brain_diagnosis.common.result;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    @Test
    void success_ShouldReturnWithData() {
        Result<String> result = Result.success("test data");

        assertEquals(200, result.getCode());
        assertNotNull(result.getMsg());
        assertEquals("test data", result.getData());
    }

    @Test
    void success_ShouldReturnWithNullData() {
        Result<String> result = Result.success();

        assertEquals(200, result.getCode());
        assertNotNull(result.getMsg());
        assertNull(result.getData());
    }

    @Test
    void success_ShouldReturnWithMap() {
        Result<?> result = Result.success(new java.util.HashMap<String, Object>() {{
            put("key", "value");
        }});

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
    }

    @Test
    void success_ShouldReturnWithList() {
        Result<?> result = Result.success(java.util.List.of(1, 2, 3));

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
    }

    @Test
    void error_ShouldReturnWithMessage() {
        Result<?> result = Result.error("Test error");

        assertEquals(500, result.getCode());
        assertEquals("Test error", result.getMsg());
        assertNull(result.getData());
    }

    @Test
    void unauthorized_ShouldReturnWithMessage() {
        Result<?> result = Result.unauthorized("Unauthorized access");

        assertEquals(401, result.getCode());
        assertEquals("Unauthorized access", result.getMsg());
        assertNull(result.getData());
    }

    @Test
    void forbidden_ShouldReturnWithMessage() {
        Result<?> result = Result.forbidden("Access forbidden");

        assertEquals(403, result.getCode());
        assertEquals("Access forbidden", result.getMsg());
        assertNull(result.getData());
    }

    @Test
    void settersAndGetters_ShouldWork() {
        Result<String> result = new Result<>();
        result.setCode(100);
        result.setMsg("Custom message");
        result.setData("Custom data");

        assertEquals(100, result.getCode());
        assertEquals("Custom message", result.getMsg());
        assertEquals("Custom data", result.getData());
    }

    @Test
    void allArgsConstructor_ShouldWork() {
        Result<String> result = new Result<>(999, "Custom", "Data");

        assertEquals(999, result.getCode());
        assertEquals("Custom", result.getMsg());
        assertEquals("Data", result.getData());
    }

    @Test
    void lombokGeneratedMethods_ShouldCoverEqualsHashCodeAndToString() {
        Result<String> first = new Result<>(200, "OK", "data");
        Result<String> same = new Result<>(200, "OK", "data");
        Result<String> differentCode = new Result<>(201, "OK", "data");
        Result<String> differentMsg = new Result<>(200, "Created", "data");
        Result<String> differentData = new Result<>(200, "OK", "other");

        assertEquals(first, first);
        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
        assertNotEquals(first, null);
        assertNotEquals(first, "not a result");
        assertNotEquals(first, differentCode);
        assertNotEquals(first, differentMsg);
        assertNotEquals(first, differentData);
        assertTrue(first.toString().contains("code=200"));
        assertTrue(first.canEqual(same));
    }

    @Test
    void lombokEquals_ShouldHandleNullFields() {
        Result<String> empty = new Result<>();
        Result<String> sameEmpty = new Result<>();
        Result<String> withCode = new Result<>(200, null, null);
        Result<String> withMsg = new Result<>(null, "OK", null);
        Result<String> withData = new Result<>(null, null, "data");

        assertEquals(empty, sameEmpty);
        assertEquals(empty.hashCode(), sameEmpty.hashCode());
        assertNotEquals(empty, withCode);
        assertNotEquals(empty, withMsg);
        assertNotEquals(empty, withData);
    }
}
