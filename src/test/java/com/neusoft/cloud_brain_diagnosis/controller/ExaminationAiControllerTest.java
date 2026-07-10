package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.context.UserContext;
import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import com.neusoft.cloud_brain_diagnosis.common.util.JwtUtil;
import com.neusoft.cloud_brain_diagnosis.entity.Examination;
import com.neusoft.cloud_brain_diagnosis.entity.CriticalValueWarning;
import com.neusoft.cloud_brain_diagnosis.entity.ExaminationAiInterpretation;
import com.neusoft.cloud_brain_diagnosis.entity.ExaminationAiReview;
import com.neusoft.cloud_brain_diagnosis.feign.AiExaminationFeignClient;
import com.neusoft.cloud_brain_diagnosis.repository.CriticalValueWarningRepository;
import com.neusoft.cloud_brain_diagnosis.repository.ExaminationAiReviewRepository;
import com.neusoft.cloud_brain_diagnosis.repository.ExaminationRepository;
import com.neusoft.cloud_brain_diagnosis.service.ai.AiExaminationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = ExaminationAiController.class, properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration," +
                "org.springframework.boot.autoconfigure.websocket.reactive.WebSocketReactiveAutoConfiguration"
})
class ExaminationAiControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private AiExaminationFeignClient examinationFeignClient;
    @MockBean private AiExaminationService examinationService;
    @MockBean private ExaminationRepository examinationRepository;
    @MockBean private CriticalValueWarningRepository criticalValueWarningRepository;
    @MockBean private ExaminationAiReviewRepository examinationAiReviewRepository;
    @MockBean private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        lenient().when(jwtUtil.validateToken(anyString())).thenReturn(true);
        lenient().when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(1L);
        lenient().when(examinationRepository.findById(nullable(Long.class))).thenReturn(Optional.empty());
        when(examinationFeignClient.interpret(anyMap())).thenAnswer(inv -> {
            Map<String, Object> request = inv.getArgument(0);
            Long id = ((Number) request.get("examinationId")).longValue();
            return success(examinationService.interpret(id));
        });
        when(examinationFeignClient.getProInterpretation(anyLong()))
                .thenAnswer(inv -> success(examinationService.getProInterpretation(inv.getArgument(0))));
        when(examinationFeignClient.getCriticalHistory(anyLong(), anyInt(), anyInt()))
                .thenAnswer(inv -> success(examinationService.getCriticalHistory(inv.getArgument(1), inv.getArgument(2))));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Result<Map<String, Object>> success(Object data) {
        return (Result) Result.success(data);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    // ========== interpret ==========

    @Test
    void interpret_ShouldReturnInterpretation() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.LAB.getCode());
        when(examinationService.interpret(1L)).thenReturn(Map.of("id", 1L, "interpretation", "Test interpretation"));

        mockMvc.perform(post("/api/examination/ai/interpret/1")
                        .header("Authorization", "Bearer lab-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void getPatientInterpretation_ShouldReturnInterpretation() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PATIENT.getCode());
        Examination examination = new Examination();
        examination.setId(1L);
        examination.setItemName("血常规");
        examination.setResult("白细胞: 6 正常");
        when(examinationRepository.findById(1L)).thenReturn(Optional.of(examination));

        mockMvc.perform(get("/api/examination/ai/interpret-patient/1")
                        .header("Authorization", "Bearer patient-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.examinationId").value(1));
    }

    @Test
    void getProInterpretation_ShouldReturnInterpretation() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());
        ExaminationAiInterpretation interpretation = new ExaminationAiInterpretation();
        interpretation.setId(1L);

        when(examinationService.getProInterpretation(1L)).thenReturn(interpretation);

        mockMvc.perform(get("/api/examination/ai/interpret-pro/1")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    // ========== critical warning ==========

    @Test
    void getCriticalList_ShouldReturnPage() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.DOCTOR.getCode());
        CriticalValueWarning warning = new CriticalValueWarning();
        warning.setId(1L);

        when(criticalValueWarningRepository.findByPatientIdOrderByCreateTimeDesc(anyLong(), any()))
                .thenReturn(new PageImpl<>(List.of(warning)));

        mockMvc.perform(get("/api/examination/ai/critical-list")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(1));
    }

    @Test
    void getCriticalHistory_ShouldReturnPage() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.ADMIN.getCode());
        CriticalValueWarning warning = new CriticalValueWarning();
        warning.setId(1L);

        when(examinationService.getCriticalHistory(anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(warning)));

        mockMvc.perform(get("/api/examination/ai/critical-history")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(1));
    }

    // ========== review ==========

    @Test
    void getManualList_ShouldReturnPage() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.LAB.getCode());
        ExaminationAiReview review = new ExaminationAiReview();
        review.setId(1L);

        when(examinationService.getManualList(anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(review)));

        mockMvc.perform(get("/api/examination/ai/manual-list")
                        .header("Authorization", "Bearer lab-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(1));
    }

    @Test
    void getReviewList_ShouldReturnPage() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.LAB.getCode());
        ExaminationAiReview review = new ExaminationAiReview();
        review.setId(1L);

        when(examinationService.getReviewList(any(), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(review)));

        mockMvc.perform(get("/api/examination/ai/review-list")
                        .header("Authorization", "Bearer lab-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(1));
    }

    @Test
    void getReviewDetail_ShouldReturnReview() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.LAB.getCode());
        ExaminationAiReview review = new ExaminationAiReview();
        review.setId(1L);

        when(examinationService.getReviewDetail(1L)).thenReturn(review);

        mockMvc.perform(get("/api/examination/ai/review-detail/1")
                        .header("Authorization", "Bearer lab-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void getReviewStats_ShouldReturnStats() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.LAB.getCode());
        when(examinationService.getReviewStats())
                .thenReturn(Map.of("total", 100, "passRate", 0.85));

        mockMvc.perform(get("/api/examination/ai/review-stats")
                        .header("Authorization", "Bearer lab-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(100));
    }

    @Test
    void getPatientInterpretation_ShouldReturnError_WhenExaminationMissing() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PATIENT.getCode());
        when(examinationRepository.findById(404L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/examination/ai/interpret-patient/404")
                        .header("Authorization", "Bearer patient-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void patientInterpretationHelpers_ShouldHandleNormalAndAbnormalReports() {
        ExaminationAiController controller = new ExaminationAiController(
                examinationFeignClient,
                examinationService,
                examinationRepository,
                criticalValueWarningRepository,
                examinationAiReviewRepository);
        Examination normal = new Examination();
        normal.setId(1L);
        normal.setItemName("CT");
        normal.setResult("normal");

        Map<String, Object> normalResult = ReflectionTestUtils.invokeMethod(controller, "buildPatientInterpretation", normal);
        String crpText = ReflectionTestUtils.invokeMethod(controller, "buildPlainInterpretation", "CRP", "value", List.of(Map.of("name", "CRP")));
        String ctSuggestion = ReflectionTestUtils.invokeMethod(controller, "buildSuggestion", "CT", List.of(Map.of("name", "CT")));
        String crpReminder = ReflectionTestUtils.invokeMethod(controller, "buildReviewReminder", "CRP", List.of(Map.of("name", "CRP")));
        String emptySuggestion = ReflectionTestUtils.invokeMethod(controller, "buildSuggestion", "blood", List.of());

        assertEquals(1L, normalResult.get("examinationId"));
        assertNotNull(crpText);
        assertNotNull(ctSuggestion);
        assertNotNull(crpReminder);
        assertNotNull(emptySuggestion);
    }

    @Test
    void enrichReview_ShouldAttachExaminationSnapshot_WhenExamExists() {
        ExaminationAiController controller = new ExaminationAiController(
                examinationFeignClient,
                examinationService,
                examinationRepository,
                criticalValueWarningRepository,
                examinationAiReviewRepository);
        ExaminationAiReview review = new ExaminationAiReview();
        review.setId(9L);
        review.setExaminationId(5L);
        review.setReviewResult("manual");
        Examination exam = new Examination();
        exam.setId(5L);
        exam.setItemName("CT");
        exam.setPatientName("patient");
        exam.setDoctorName("doctor");
        exam.setResult("normal");
        exam.setStatus("done");
        when(examinationRepository.findById(5L)).thenReturn(Optional.of(exam));

        Map<String, Object> map = ReflectionTestUtils.invokeMethod(controller, "enrichReview", review);

        assertEquals(9L, map.get("id"));
        assertEquals("CT", map.get("itemName"));
        assertEquals("patient", map.get("patientName"));
    }

    @Test
    void reviewStreamHelpers_ShouldFormatPassManualAndRejectText() {
        ExaminationAiController controller = new ExaminationAiController(
                examinationFeignClient,
                examinationService,
                examinationRepository,
                criticalValueWarningRepository,
                examinationAiReviewRepository);
        Examination exam = new Examination();
        exam.setId(12L);
        exam.setItemName("CT");
        ExaminationAiReview manual = new ExaminationAiReview();
        manual.setReviewResult("manual");
        manual.setAbnormalItems("[{\"name\":\"A\"}]");
        manual.setLogicIssues("[{\"content\":\"logic\"}]");
        manual.setHistoryCompare("[{\"content\":\"history\"}]");
        manual.setWarnings("[{\"content\":\"warn\"}]");
        manual.setSuggestions("check manually");

        String manualText = ReflectionTestUtils.invokeMethod(controller, "buildReviewStreamText", exam, manual, Map.of());
        String rejectText = ReflectionTestUtils.invokeMethod(controller, "buildReviewStreamText", exam, null, Map.of("reviewResult", "reject"));
        String passText = ReflectionTestUtils.invokeMethod(controller, "buildReviewStreamText", null, null, Map.of("reviewResult", "pass"));
        String bullets = ReflectionTestUtils.invokeMethod(controller, "toBulletLines", List.of("a", "b"));
        List<Map<String, Object>> summary = ReflectionTestUtils.invokeMethod(controller, "parseReviewItemArray", "[{\"name\":\"A\"}]");
        Long longValue = ReflectionTestUtils.invokeMethod(controller, "toLong", "123");
        Long nullLong = ReflectionTestUtils.invokeMethod(controller, "toLong", "bad");
        String escaped = ReflectionTestUtils.invokeMethod(controller, "jsonEscape", "a\"b");

        assertTrue(manualText.contains("CT"));
        assertNotNull(rejectText);
        assertNotNull(passText);
        assertTrue(bullets.contains("a"));
        assertEquals("A", summary.get(0).get("name"));
        assertEquals(123L, longValue);
        assertNull(nullLong);
        assertEquals("a\\\"b", escaped);
    }

    @Test
    void reviewActionEndpoints_ShouldDelegateToService() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.LAB.getCode());
        when(examinationService.reviewExamination(eq(1L), eq(1L))).thenReturn(Map.of("reviewResult", "pass"));
        when(examinationService.manualConfirm(1L)).thenReturn("ok");
        when(examinationService.reject(1L, "bad sample")).thenReturn("rejected");

        mockMvc.perform(post("/api/examination/ai/review/1")
                        .header("Authorization", "Bearer lab-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewResult").value("pass"));
        mockMvc.perform(post("/api/examination/ai/manual-confirm/1")
                        .header("Authorization", "Bearer lab-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("ok"));
        mockMvc.perform(post("/api/examination/ai/reject/1")
                        .param("reason", "bad sample")
                        .header("Authorization", "Bearer lab-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("rejected"));
    }
}
