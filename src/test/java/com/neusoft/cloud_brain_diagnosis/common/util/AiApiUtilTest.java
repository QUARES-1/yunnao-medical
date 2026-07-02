package com.neusoft.cloud_brain_diagnosis.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class AiApiUtilTest {

    private AiApiUtil aiApiUtil;

    @BeforeEach
    void setUp() {
        aiApiUtil = new AiApiUtil();
        ReflectionTestUtils.setField(aiApiUtil, "provider", "mock");
        ReflectionTestUtils.setField(aiApiUtil, "apiKey", "");
        ReflectionTestUtils.setField(aiApiUtil, "model", "test-model");
        ReflectionTestUtils.setField(aiApiUtil, "baseUrl", "");
        ReflectionTestUtils.setField(aiApiUtil, "timeout", 50);
        ReflectionTestUtils.setField(aiApiUtil, "objectMapper", new ObjectMapper());
    }

    @Test
    void callAi_ShouldReturnSpecializedMockResponses_ForKnownPrompts() {
        assertJsonContains("triage", "recommendDepartment");
        assertJsonContains("prescription", "reviewResult");
        assertJsonContains("medical record", "chiefComplaint");
        assertJsonContains("medication", "guideContent");
        assertJsonContains("examination interpret", "abnormalItems");
        assertJsonContains("critical", "criticalItems");
        assertJsonContains("health", "answer");
        assertJsonContains("follow", "questionnaire");
        assertJsonContains("operation", "summary");
        assertJsonContains("quality", "totalCount");
        assertJsonContains("stock", "forecastData");
        assertJsonContains("general", "source");
    }

    @Test
    void callAi_ShouldHandleNullPromptInMockMode() {
        String response = aiApiUtil.callAi(null, "triage");

        assertTrue(response.contains("recommendDepartmentId"));
    }

    @Test
    void callAiJson_ShouldParseMockJson() {
        Map<?, ?> response = aiApiUtil.callAiJson("headache", "triage", Map.class);

        assertTrue(response.containsKey("recommendDepartment"));
        assertTrue(response.containsKey("confidence"));
    }

    @Test
    void callAiStream_ShouldInvokeCallbackWithAiResult() {
        AtomicReference<String> streamed = new AtomicReference<>();

        aiApiUtil.callAiStream("question", "health", streamed::set);

        assertNotNull(streamed.get());
        assertTrue(streamed.get().contains("answer"));
    }

    @Test
    void callAi_ShouldFallbackToMock_WhenRealProviderHasNoApiKey() {
        ReflectionTestUtils.setField(aiApiUtil, "provider", "real");
        ReflectionTestUtils.setField(aiApiUtil, "apiKey", "your_api_key");

        String response = aiApiUtil.callAi("question", "health");

        assertTrue(response.contains("answer"));
    }

    @Test
    void buildChatCompletionsUrl_ShouldNormalizeBaseUrl() {
        ReflectionTestUtils.setField(aiApiUtil, "baseUrl", "https://example.test/v1///");

        String url = ReflectionTestUtils.invokeMethod(aiApiUtil, "buildChatCompletionsUrl");

        assertEquals("https://example.test/v1/chat/completions", url);
    }

    @Test
    void buildChatCompletionsUrl_ShouldKeepCompletedEndpoint() {
        ReflectionTestUtils.setField(aiApiUtil, "baseUrl", "https://example.test/v1/chat/completions");

        String url = ReflectionTestUtils.invokeMethod(aiApiUtil, "buildChatCompletionsUrl");

        assertEquals("https://example.test/v1/chat/completions", url);
    }

    @Test
    void normalizeJsonContent_ShouldRemoveMarkdownFenceAndOuterText() {
        String normalized = ReflectionTestUtils.invokeMethod(
                aiApiUtil,
                "normalizeJsonContent",
                "prefix ```json\n{\"ok\":true}\n``` suffix"
        );

        assertEquals("{\"ok\":true}", normalized);
    }

    @Test
    void normalizeJsonContent_ShouldHandleNullAsBlank() {
        String normalized = ReflectionTestUtils.invokeMethod(aiApiUtil, "normalizeJsonContent", (String) null);

        assertEquals("", normalized);
    }

    private void assertJsonContains(String systemPrompt, String expectedKey) {
        String response = aiApiUtil.callAi("test prompt", systemPrompt);

        assertTrue(response.contains(expectedKey), () -> systemPrompt + " should contain " + expectedKey);
    }
}
