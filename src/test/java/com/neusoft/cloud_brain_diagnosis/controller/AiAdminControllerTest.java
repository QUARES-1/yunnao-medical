package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import com.neusoft.cloud_brain_diagnosis.common.util.JwtUtil;
import com.neusoft.cloud_brain_diagnosis.entity.AiChatRecord;
import com.neusoft.cloud_brain_diagnosis.entity.OperationAiReport;
import com.neusoft.cloud_brain_diagnosis.entity.QualityCheckDetail;
import com.neusoft.cloud_brain_diagnosis.entity.QualityCheckRecord;
import com.neusoft.cloud_brain_diagnosis.feign.AiAdminFeignClient;
import com.neusoft.cloud_brain_diagnosis.repository.*;
import com.neusoft.cloud_brain_diagnosis.service.ai.AiChatService;
import com.neusoft.cloud_brain_diagnosis.service.ai.AiOperationService;
import com.neusoft.cloud_brain_diagnosis.service.ai.AiQualityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = AiAdminController.class, properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration," +
                "org.springframework.boot.autoconfigure.websocket.reactive.WebSocketReactiveAutoConfiguration"
})
class AiAdminControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private AiAdminFeignClient adminFeignClient;
    @MockBean private AiOperationService operationService;
    @MockBean private AiQualityService qualityService;
    @MockBean private AiChatService chatService;
    @MockBean private AiChatRecordRepository aiChatRecordRepository;
    @MockBean private TriageRecordRepository triageRecordRepository;
    @MockBean private QualityCheckRecordRepository qualityCheckRecordRepository;
    @MockBean private AiKnowledgeBaseRepository aiKnowledgeBaseRepository;
    @MockBean private OperationAiReportRepository operationAiReportRepository;
    @MockBean private ExaminationAiInterpretationRepository examinationAiInterpretationRepository;
    @MockBean private MedicationGuideRepository medicationGuideRepository;
    @MockBean private CriticalValueWarningRepository criticalValueWarningRepository;
    @MockBean private FollowUpPlanRepository followUpPlanRepository;
    @MockBean private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        when(jwtUtil.validateToken(anyString())).thenReturn(true);
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.ADMIN.getCode());
        lenient().when(aiChatRecordRepository.findAll()).thenReturn(List.of());
        lenient().when(aiChatRecordRepository.findByOrderByCreateTimeDesc(any()))
                .thenReturn(new PageImpl<>(List.of()));
        lenient().when(operationAiReportRepository.findByOrderByCreateTimeDesc(any()))
                .thenReturn(new PageImpl<>(List.of()));
        lenient().when(operationAiReportRepository.findByReportTypeOrderByCreateTimeDesc(anyString(), any()))
                .thenReturn(new PageImpl<>(List.of()));
        lenient().when(operationAiReportRepository.save(any(OperationAiReport.class))).thenAnswer(inv -> {
            OperationAiReport report = inv.getArgument(0);
            if (report.getId() == null) {
                report.setId(100L);
            }
            return report;
        });
        when(adminFeignClient.generateReport(anyMap())).thenAnswer(inv -> {
            Map<String, Object> request = inv.getArgument(0);
            String reportType = String.valueOf(request.getOrDefault("reportType", "daily"));
            return success(operationService.generateReport(reportType,
                    (String) request.get("startDate"), (String) request.get("endDate")));
        });
        when(adminFeignClient.getReportDetail(anyLong()))
                .thenAnswer(inv -> success(operationService.getReportDetail(inv.getArgument(0))));
        when(adminFeignClient.getReportList(nullable(String.class), anyInt(), anyInt()))
                .thenAnswer(inv -> success(operationService.getReportList(inv.getArgument(0), inv.getArgument(1), inv.getArgument(2))));
        when(adminFeignClient.getOperationOverview())
                .thenAnswer(inv -> success(operationService.getOperationOverview()));
        when(adminFeignClient.startQualityCheck(anyMap())).thenAnswer(inv -> {
            Map<String, Object> request = inv.getArgument(0);
            String checkType = String.valueOf(request.getOrDefault("checkType", "medical_record"));
            Integer sampleSize = ((Number) request.getOrDefault("sampleSize", 10)).intValue();
            return success(qualityService.startQualityCheck(checkType, sampleSize));
        });
        when(adminFeignClient.getCheckList(anyInt(), anyInt()))
                .thenAnswer(inv -> success(qualityService.getCheckList(inv.getArgument(0), inv.getArgument(1))));
        when(adminFeignClient.getCheckDetail(anyLong()))
                .thenAnswer(inv -> success(qualityService.getCheckDetail(inv.getArgument(0))));
        when(adminFeignClient.getCheckDetails(anyLong(), anyInt(), anyInt()))
                .thenAnswer(inv -> success(qualityService.getCheckDetails(inv.getArgument(0), inv.getArgument(1), inv.getArgument(2))));
        when(adminFeignClient.getDoctorStats())
                .thenAnswer(inv -> success(qualityService.getDoctorStats()));
        when(adminFeignClient.getChatLogs(anyInt(), anyInt()))
                .thenAnswer(inv -> success(chatService.getChatLogs(inv.getArgument(0), inv.getArgument(1))));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Result<Map<String, Object>> success(Object data) {
        return (Result) Result.success(data);
    }

    // ========== generateReport() ==========

    @Test
    void generateReport_ShouldReturnReport() throws Exception {
        when(operationService.generateReport(eq("daily"), any(), any()))
                .thenReturn(Map.of("id", 100L, "reportType", "daily"));

        mockMvc.perform(post("/api/admin/ai/operation-report/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportType\":\"daily\",\"startDate\":\"2026-06-01\",\"endDate\":\"2026-06-30\"}")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(100))
                .andExpect(jsonPath("$.data.reportType").value("daily"));
    }

    @Test
    void generateReport_ShouldUseDefaultReportType_WhenNotProvided() throws Exception {
        when(operationService.generateReport(eq("daily"), any(), any()))
                .thenReturn(Map.of("id", 100L));

        mockMvc.perform(post("/api/admin/ai/operation-report/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startDate\":\"2026-06-01\"}")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
    }

    // ========== getReportDetail() ==========

    @Test
    void getReportDetail_ShouldReturnReport() throws Exception {
        OperationAiReport report = new OperationAiReport();
        report.setId(1L);
        report.setSummary("Summary");

        when(operationService.getReportDetail(1L)).thenReturn(report);

        mockMvc.perform(get("/api/admin/ai/operation-report/1")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    // ========== getReportList() ==========

    @Test
    void getReportList_ShouldReturnPage() throws Exception {
        OperationAiReport report = new OperationAiReport();
        report.setId(1L);

        when(operationAiReportRepository.findByOrderByCreateTimeDesc(any()))
                .thenReturn(new PageImpl<>(List.of(report)));

        mockMvc.perform(get("/api/admin/ai/operation-report/list")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(1));
    }

    @Test
    void getReportList_ShouldFilterByType() throws Exception {
        when(operationAiReportRepository.findByReportTypeOrderByCreateTimeDesc(eq("weekly"), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/admin/ai/operation-report/list")
                        .param("reportType", "weekly")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
    }

    @Test
    void getReportList_ShouldUseCustomPagination() throws Exception {
        when(operationAiReportRepository.findByOrderByCreateTimeDesc(any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/admin/ai/operation-report/list")
                        .param("page", "2")
                        .param("size", "20")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
    }

    // ========== getOperationOverview() ==========

    @Test
    void getOperationOverview_ShouldReturnOverview() throws Exception {
        AiChatRecord record = new AiChatRecord();
        record.setSource("knowledge");
        when(aiChatRecordRepository.count()).thenReturn(1L);
        when(aiChatRecordRepository.findAll()).thenReturn(List.of(record));
        when(triageRecordRepository.count()).thenReturn(100L);

        mockMvc.perform(get("/api/admin/ai/operation-overview")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.aiTriageCount").value(100))
                .andExpect(jsonPath("$.data.knowledgeHitRate").value("100.0%"));
    }

    // ========== startQualityCheck() ==========

    @Test
    void startQualityCheck_ShouldReturnResult() throws Exception {
        when(qualityService.startQualityCheck(eq("medical_record"), eq(10)))
                .thenReturn(Map.of("id", 100L, "checkType", "medical_record"));

        mockMvc.perform(post("/api/admin/ai/quality-check/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"checkType\":\"medical_record\",\"sampleSize\":10}")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checkType").value("medical_record"));
    }

    @Test
    void startQualityCheck_ShouldUseDefaultSampleSize_WhenNotProvided() throws Exception {
        when(qualityService.startQualityCheck(eq("medical_record"), eq(10)))
                .thenReturn(Map.of("id", 100L));

        mockMvc.perform(post("/api/admin/ai/quality-check/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"checkType\":\"medical_record\"}")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
    }

    @Test
    void startQualityCheck_ShouldUseDefaultCheckType_WhenNotProvided() throws Exception {
        when(qualityService.startQualityCheck(eq("medical_record"), eq(10)))
                .thenReturn(Map.of("id", 100L));

        mockMvc.perform(post("/api/admin/ai/quality-check/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sampleSize\":5}")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
    }

    // ========== getCheckList() ==========

    @Test
    void getCheckList_ShouldReturnPage() throws Exception {
        QualityCheckRecord record = new QualityCheckRecord();
        record.setId(1L);

        when(qualityService.getCheckList(eq(1), eq(10)))
                .thenReturn(new PageImpl<>(List.of(record)));

        mockMvc.perform(get("/api/admin/ai/quality-check/list")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(1));
    }

    @Test
    void getCheckList_ShouldUseCustomPagination() throws Exception {
        when(qualityService.getCheckList(eq(2), eq(20)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/admin/ai/quality-check/list")
                        .param("page", "2")
                        .param("size", "20")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
    }

    // ========== getCheckDetail() ==========

    @Test
    void getCheckDetail_ShouldReturnRecord() throws Exception {
        QualityCheckRecord record = new QualityCheckRecord();
        record.setId(1L);
        record.setCheckType("medical_record");

        when(qualityService.getCheckDetail(1L)).thenReturn(record);

        mockMvc.perform(get("/api/admin/ai/quality-check/1")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.checkType").value("medical_record"));
    }

    // ========== getCheckDetails() ==========

    @Test
    void getCheckDetails_ShouldReturnPage() throws Exception {
        QualityCheckDetail detail = new QualityCheckDetail();
        detail.setId(1L);

        when(qualityService.getCheckDetails(eq(1L), eq(1), eq(10)))
                .thenReturn(new PageImpl<>(List.of(detail)));

        mockMvc.perform(get("/api/admin/ai/quality-check/1/details")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(1));
    }

    @Test
    void getCheckDetails_ShouldUseCustomPagination() throws Exception {
        when(qualityService.getCheckDetails(eq(1L), eq(2), eq(20)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/admin/ai/quality-check/1/details")
                        .param("page", "2")
                        .param("size", "20")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
    }

    // ========== getDoctorStats() ==========

    @Test
    void getDoctorStats_ShouldReturnStats() throws Exception {
        when(qualityService.getDoctorStats())
                .thenReturn(Map.of("total", 100, "passRate", 0.85));

        mockMvc.perform(get("/api/admin/ai/quality-check/doctor-stats")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(100))
                .andExpect(jsonPath("$.data.passRate").value(0.85));
    }

    // ========== getChatLogs() ==========

    @Test
    void getChatLogs_ShouldReturnPage() throws Exception {
        AiChatRecord record = new AiChatRecord();
        record.setId(1L);
        record.setQuestion("Question");

        when(aiChatRecordRepository.findByOrderByCreateTimeDesc(any()))
                .thenReturn(new PageImpl<>(List.of(record)));

        mockMvc.perform(get("/api/admin/ai/chat-log")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(1));
    }

    @Test
    void getChatLogs_ShouldUseCustomPagination() throws Exception {
        when(aiChatRecordRepository.findByOrderByCreateTimeDesc(any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/admin/ai/chat-log")
                        .param("page", "2")
                        .param("size", "20")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
    }

    @Test
    void buildLocalOverview_ShouldCalculateCountsAndHitRate() {
        AiChatRecordRepository chatRepo = mock(AiChatRecordRepository.class);
        TriageRecordRepository triageRepo = mock(TriageRecordRepository.class);
        QualityCheckRecordRepository qualityRepo = mock(QualityCheckRecordRepository.class);
        AiKnowledgeBaseRepository knowledgeRepo = mock(AiKnowledgeBaseRepository.class);
        OperationAiReportRepository reportRepo = mock(OperationAiReportRepository.class);
        ExaminationAiInterpretationRepository interpretationRepo = mock(ExaminationAiInterpretationRepository.class);
        MedicationGuideRepository medicationRepo = mock(MedicationGuideRepository.class);
        CriticalValueWarningRepository warningRepo = mock(CriticalValueWarningRepository.class);
        FollowUpPlanRepository followUpRepo = mock(FollowUpPlanRepository.class);
        AiChatRecord knowledgeRecord = new AiChatRecord();
        knowledgeRecord.setSource("knowledge");
        AiChatRecord aiRecord = new AiChatRecord();
        aiRecord.setSource("ai");
        when(chatRepo.count()).thenReturn(2L);
        when(chatRepo.findAll()).thenReturn(List.of(knowledgeRecord, aiRecord));
        when(triageRepo.count()).thenReturn(3L);
        when(qualityRepo.count()).thenReturn(4L);
        when(knowledgeRepo.count()).thenReturn(5L);
        when(interpretationRepo.count()).thenReturn(6L);
        when(medicationRepo.count()).thenReturn(7L);
        when(warningRepo.count()).thenReturn(8L);
        when(followUpRepo.count()).thenReturn(9L);

        AiAdminController controller = new AiAdminController(
                adminFeignClient, chatRepo, triageRepo, qualityRepo, knowledgeRepo, reportRepo,
                interpretationRepo, medicationRepo, warningRepo, followUpRepo);

        Map<String, Object> overview = ReflectionTestUtils.invokeMethod(controller, "buildLocalOverview");

        assertEquals(2L, overview.get("aiChatCount"));
        assertEquals(3L, overview.get("aiTriageCount"));
        assertEquals("50.0%", overview.get("knowledgeHitRate"));
    }

    @Test
    void generateReport_ShouldCreateLocalReportAndMapFallbackFields() {
        AiChatRecordRepository chatRepo = mock(AiChatRecordRepository.class);
        TriageRecordRepository triageRepo = mock(TriageRecordRepository.class);
        QualityCheckRecordRepository qualityRepo = mock(QualityCheckRecordRepository.class);
        AiKnowledgeBaseRepository knowledgeRepo = mock(AiKnowledgeBaseRepository.class);
        OperationAiReportRepository reportRepo = mock(OperationAiReportRepository.class);
        ExaminationAiInterpretationRepository interpretationRepo = mock(ExaminationAiInterpretationRepository.class);
        MedicationGuideRepository medicationRepo = mock(MedicationGuideRepository.class);
        CriticalValueWarningRepository warningRepo = mock(CriticalValueWarningRepository.class);
        FollowUpPlanRepository followUpRepo = mock(FollowUpPlanRepository.class);
        when(chatRepo.findAll()).thenReturn(List.of());
        when(reportRepo.save(any(OperationAiReport.class))).thenAnswer(inv -> {
            OperationAiReport report = inv.getArgument(0);
            report.setId(77L);
            return report;
        });
        AiAdminController controller = new AiAdminController(
                adminFeignClient, chatRepo, triageRepo, qualityRepo, knowledgeRepo, reportRepo,
                interpretationRepo, medicationRepo, warningRepo, followUpRepo);

        Result<Map<String, Object>> result = controller.generateReport(Map.of("reportType", "monthly"));

        assertEquals(200, result.getCode());
        assertEquals(77L, result.getData().get("id"));
        assertEquals("monthly", result.getData().get("reportType"));
        verify(reportRepo).save(any(OperationAiReport.class));
    }

    @Test
    void formatHelpers_ShouldHandleJsonBlankAndInvalidInput() {
        AiAdminController controller = new AiAdminController(
                adminFeignClient,
                mock(AiChatRecordRepository.class),
                mock(TriageRecordRepository.class),
                mock(QualityCheckRecordRepository.class),
                mock(AiKnowledgeBaseRepository.class),
                mock(OperationAiReportRepository.class),
                mock(ExaminationAiInterpretationRepository.class),
                mock(MedicationGuideRepository.class),
                mock(CriticalValueWarningRepository.class),
                mock(FollowUpPlanRepository.class));

        String metrics = ReflectionTestUtils.invokeMethod(controller, "formatMetrics", "{\"aiChatCount\":2,\"knowledgeHitRate\":\"50%\"}");
        String blankMetrics = ReflectionTestUtils.invokeMethod(controller, "formatMetrics", "");
        String warnings = ReflectionTestUtils.invokeMethod(controller, "formatWarnings", "[{\"content\":\"warn\"},\"plain\"]");
        String invalidWarnings = ReflectionTestUtils.invokeMethod(controller, "formatWarnings", "not-json");
        String suggestions = ReflectionTestUtils.invokeMethod(controller, "formatSuggestions", "[\"one\",\"two\"]");
        String invalidSuggestions = ReflectionTestUtils.invokeMethod(controller, "formatSuggestions", "not-json");
        String streamText = ReflectionTestUtils.invokeMethod(controller, "buildStreamReportText", Map.of(
                "summary", "summary",
                "keyMetrics", "{\"aiChatCount\":2}",
                "trendsAnalysis", "trend",
                "warnings", "[{\"content\":\"warn\"}]",
                "suggestions", "[\"one\"]"
        ));
        String nullStreamText = ReflectionTestUtils.invokeMethod(controller, "buildStreamReportText", (Object) null);

        assertTrue(metrics.contains("2"));
        assertNotNull(blankMetrics);
        assertTrue(warnings.contains("warn"));
        assertTrue(invalidWarnings.contains("not-json"));
        assertTrue(suggestions.contains("one"));
        assertEquals("not-json", invalidSuggestions);
        assertTrue(streamText.contains("summary"));
        assertNotNull(nullStreamText);
    }
}
