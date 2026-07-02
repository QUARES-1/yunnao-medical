package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.util.JwtUtil;
import com.neusoft.cloud_brain_diagnosis.entity.AiChatRecord;
import com.neusoft.cloud_brain_diagnosis.entity.OperationAiReport;
import com.neusoft.cloud_brain_diagnosis.entity.QualityCheckDetail;
import com.neusoft.cloud_brain_diagnosis.entity.QualityCheckRecord;
import com.neusoft.cloud_brain_diagnosis.feign.AiAdminFeignClient;
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
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

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
    @MockBean private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        when(jwtUtil.validateToken(anyString())).thenReturn(true);
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.ADMIN.getCode());
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

        when(operationService.getReportList(isNull(), eq(1), eq(10)))
                .thenReturn(new PageImpl<>(List.of(report)));

        mockMvc.perform(get("/api/admin/ai/operation-report/list")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(1));
    }

    @Test
    void getReportList_ShouldFilterByType() throws Exception {
        when(operationService.getReportList(eq("weekly"), eq(1), eq(10)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/admin/ai/operation-report/list")
                        .param("reportType", "weekly")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
    }

    @Test
    void getReportList_ShouldUseCustomPagination() throws Exception {
        when(operationService.getReportList(isNull(), eq(2), eq(20)))
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
        when(operationService.getOperationOverview())
                .thenReturn(Map.of(
                        "aiTriageCount", 100,
                        "aiChatCount", 500,
                        "knowledgeHitRate", 0.75
                ));

        mockMvc.perform(get("/api/admin/ai/operation-overview")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.aiTriageCount").value(100))
                .andExpect(jsonPath("$.data.knowledgeHitRate").value(0.75));
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

        when(chatService.getChatLogs(eq(1), eq(10)))
                .thenReturn(new PageImpl<>(List.of(record)));

        mockMvc.perform(get("/api/admin/ai/chat-log")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(1));
    }

    @Test
    void getChatLogs_ShouldUseCustomPagination() throws Exception {
        when(chatService.getChatLogs(eq(2), eq(20)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/admin/ai/chat-log")
                        .param("page", "2")
                        .param("size", "20")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
    }
}
