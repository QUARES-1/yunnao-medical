package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.context.UserContext;
import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import com.neusoft.cloud_brain_diagnosis.common.util.JwtUtil;
import com.neusoft.cloud_brain_diagnosis.entity.MedicalRecordAiGenerate;
import com.neusoft.cloud_brain_diagnosis.feign.AiMedicalRecordFeignClient;
import com.neusoft.cloud_brain_diagnosis.service.ai.AiMedicalRecordService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = AiMedicalRecordController.class, properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration," +
                "org.springframework.boot.autoconfigure.websocket.reactive.WebSocketReactiveAutoConfiguration"
})
class AiMedicalRecordControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private AiMedicalRecordFeignClient medicalRecordFeignClient;
    @MockBean private AiMedicalRecordService medicalRecordService;
    @MockBean private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        when(jwtUtil.validateToken(anyString())).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(10L);
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());
        when(medicalRecordFeignClient.generateRecord(anyMap())).thenAnswer(inv -> {
            Map<String, Object> request = inv.getArgument(0);
            Long patientId = ((Number) request.get("patientId")).longValue();
            String inputText = String.valueOf(request.getOrDefault("inputText", ""));
            String inputType = String.valueOf(request.getOrDefault("inputType", "keyword"));
            return success(medicalRecordService.generateRecord(patientId, inputText, inputType, jwtUtil.getUserIdFromToken("")));
        });
        when(medicalRecordFeignClient.getGenerateList(anyInt(), anyInt()))
                .thenAnswer(inv -> success(medicalRecordService.getGenerateList(jwtUtil.getUserIdFromToken(""),
                        inv.getArgument(0), inv.getArgument(1))));
        when(medicalRecordFeignClient.getGenerateDetail(anyLong()))
                .thenAnswer(inv -> success(medicalRecordService.getGenerateDetail(inv.getArgument(0))));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Result<Map<String, Object>> success(Object data) {
        return (Result) Result.success(data);
    }

    // ========== generateRecord() ==========

    @Test
    void generateRecord_ShouldReturnGeneratedRecord() throws Exception {
        when(medicalRecordService.generateRecord(eq(1L), eq("头痛3天"), eq("keyword"), eq(10L)))
                .thenReturn(Map.of(
                        "id", 100L,
                        "chiefComplaint", "头痛3天",
                        "presentIllness", "轻微头痛",
                        "diagnosis", "偏头痛"
                ));

        mockMvc.perform(post("/api/medical-record/ai/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":1,\"inputText\":\"头痛3天\",\"inputType\":\"keyword\"}")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chiefComplaint").value("头痛3天"))
                .andExpect(jsonPath("$.data.presentIllness").value("轻微头痛"))
                .andExpect(jsonPath("$.data.diagnosis").value("偏头痛"));
    }

    @Test
    void generateRecord_ShouldUseDefaultInputType() throws Exception {
        when(medicalRecordService.generateRecord(eq(1L), eq("测试"), eq("keyword"), eq(10L)))
                .thenReturn(Map.of("id", 100L));

        mockMvc.perform(post("/api/medical-record/ai/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":1,\"inputText\":\"测试\"}")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk());
    }

    // ========== getGenerateList() ==========

    @Test
    void getGenerateList_ShouldReturnPage() throws Exception {
        MedicalRecordAiGenerate gen = new MedicalRecordAiGenerate();
        gen.setId(1L);
        gen.setPatientId(1L);

        when(medicalRecordService.getGenerateList(eq(10L), eq(1), eq(10)))
                .thenReturn(new PageImpl<>(List.of(gen)));

        mockMvc.perform(get("/api/medical-record/ai/generate-list")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(1));
    }

    @Test
    void getGenerateList_ShouldSupportPagination() throws Exception {
        when(medicalRecordService.getGenerateList(eq(10L), eq(2), eq(20)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/medical-record/ai/generate-list")
                        .param("page", "2")
                        .param("size", "20")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk());
    }

    // ========== getGenerateDetail() ==========

    @Test
    void getGenerateDetail_ShouldReturnRecord() throws Exception {
        MedicalRecordAiGenerate gen = new MedicalRecordAiGenerate();
        gen.setId(1L);
        gen.setGeneratedChiefComplaint("头痛");

        when(medicalRecordService.getGenerateDetail(1L)).thenReturn(gen);

        mockMvc.perform(get("/api/medical-record/ai/generate/1")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.generatedChiefComplaint").value("头痛"));
    }
    @Test
    void toLong_ShouldHandleNumberStringBlankNullAndInvalidValues() {
        AiMedicalRecordController controller = new AiMedicalRecordController(medicalRecordService);

        assertEquals(Long.valueOf(1L), ReflectionTestUtils.invokeMethod(controller, "toLong", 1));
        assertEquals(Long.valueOf(2L), ReflectionTestUtils.invokeMethod(controller, "toLong", " 2 "));
        assertNull(ReflectionTestUtils.invokeMethod(controller, "toLong", ""));
        assertNull(ReflectionTestUtils.invokeMethod(controller, "toLong", "null"));
        assertNull(ReflectionTestUtils.invokeMethod(controller, "toLong", "abc"));
        assertNull(ReflectionTestUtils.invokeMethod(controller, "toLong", new Object[]{null}));
    }

    @Test
    void generateRecord_ShouldAcceptStringPatientIdAndDefaultMissingValues() {
        UserContext.setUserId(10L);
        AiMedicalRecordController controller = new AiMedicalRecordController(medicalRecordService);
        when(medicalRecordService.generateRecord(eq(7L), eq(""), eq("symptom"), eq(10L)))
                .thenReturn(Map.of("id", 7L));

        Result<Map<String, Object>> result = controller.generateRecord(Map.of("patientId", "7"));

        assertEquals(200, result.getCode());
        assertEquals(7L, result.getData().get("id"));
    }

    @Test
    void generateRecordStream_ShouldInvokeServiceAndReturnEmitter() {
        UserContext.setUserId(10L);
        AiMedicalRecordController controller = new AiMedicalRecordController(medicalRecordService);
        when(medicalRecordService.generateRecord(eq(1L), eq("cough"), eq("symptom"), eq(10L)))
                .thenReturn(Map.of(
                        "id", 100L,
                        "chiefComplaint", "A",
                        "presentIllness", "",
                        "pastHistory", "null",
                        "physicalExamination", "",
                        "diagnosis", "",
                        "treatment", ""
                ));

        SseEmitter emitter = controller.generateRecordStream(Map.of("patientId", 1L, "inputText", "cough"));

        assertNotNull(emitter);
        verify(medicalRecordService, timeout(1000))
                .generateRecord(1L, "cough", "symptom", 10L);
    }

    @Test
    void generateRecordStream_ShouldCompleteWithError_WhenInputTextIsBlank() {
        UserContext.setUserId(10L);
        AiMedicalRecordController controller = new AiMedicalRecordController(medicalRecordService);

        SseEmitter emitter = controller.generateRecordStream(Map.of("patientId", 1L, "inputText", "   "));

        assertNotNull(emitter);
        verify(medicalRecordService, after(200).never())
                .generateRecord(any(), anyString(), anyString(), any());
    }

    @Test
    void generateRecordStream_ShouldHandleServiceException() {
        UserContext.setUserId(10L);
        AiMedicalRecordController controller = new AiMedicalRecordController(medicalRecordService);
        when(medicalRecordService.generateRecord(eq(1L), eq("cough"), eq("symptom"), eq(10L)))
                .thenThrow(new RuntimeException("service down"));

        SseEmitter emitter = controller.generateRecordStream(Map.of("patientId", 1L, "inputText", "cough"));

        assertNotNull(emitter);
        verify(medicalRecordService, timeout(1000))
                .generateRecord(1L, "cough", "symptom", 10L);
    }

    @Test
    void sendError_ShouldIgnoreEmitterSendFailure() {
        AiMedicalRecordController controller = new AiMedicalRecordController(medicalRecordService);
        SseEmitter emitter = new SseEmitter(1000L) {
            @Override
            public void send(SseEventBuilder builder) throws IOException {
                throw new IOException("closed");
            }
        };

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(controller, "sendError", emitter, "failed"));
    }
}
