package com.neusoft.cloud_brain_diagnosis.service.ai;

import com.neusoft.cloud_brain_diagnosis.common.util.AiApiUtil;
import com.neusoft.cloud_brain_diagnosis.entity.OperationAiReport;
import com.neusoft.cloud_brain_diagnosis.repository.OperationAiReportRepository;
import com.neusoft.cloud_brain_diagnosis.service.ai.impl.AiOperationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiOperationServiceImplTest {

    @Mock private OperationAiReportRepository reportRepository;
    @Mock private AiApiUtil aiApiUtil;

    private AiOperationServiceImpl operationService;

    @BeforeEach
    void setUp() {
        operationService = new AiOperationServiceImpl(reportRepository, aiApiUtil);
    }

    // ========== generateReport() ==========

    @Test
    void generateReport_ShouldCreateReport_WithAiResponse() {
        String aiResponse = "{\"summary\":\"运营良好\",\"keyMetrics\":{\"totalRegistrations\":1250},\"trendsAnalysis\":\"上升\"}";
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn(aiResponse);
        when(reportRepository.save(any())).thenAnswer(invocation -> {
            OperationAiReport report = invocation.getArgument(0);
            report.setId(1L);
            return report;
        });

        Map<String, Object> result = operationService.generateReport("daily", "2026-01-01", "2026-01-07");

        assertNotNull(result);
        assertEquals(1L, result.get("id"));
        assertEquals("daily", result.get("reportType"));
        assertEquals(LocalDate.of(2026, 1, 1), result.get("startDate"));
        assertEquals(LocalDate.of(2026, 1, 7), result.get("endDate"));

        verify(reportRepository).save(any(OperationAiReport.class));
    }

    @Test
    void generateReport_ShouldUseCurrentDate_WhenStartDateIsNull() {
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn("{\"summary\":\"报告\"}");
        when(reportRepository.save(any())).thenAnswer(invocation -> {
            OperationAiReport report = invocation.getArgument(0);
            report.setId(1L);
            return report;
        });

        Map<String, Object> result = operationService.generateReport("monthly", null, null);

        assertNotNull(result);
        assertEquals(LocalDate.now().minusWeeks(1), result.get("startDate"));
        assertEquals(LocalDate.now(), result.get("endDate"));
    }

    @Test
    void generateReport_ShouldHandleInvalidJson() {
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn("invalid json response");
        when(reportRepository.save(any())).thenAnswer(invocation -> {
            OperationAiReport report = invocation.getArgument(0);
            report.setId(1L);
            return report;
        });

        Map<String, Object> result = operationService.generateReport("weekly", "2026-01-01", "2026-01-07");

        assertNotNull(result);
        assertEquals(1L, result.get("id"));
    }

    @Test
    void generateReport_ShouldParseNestedJson() {
        String aiResponse = "{\"summary\":\"分析总结\",\"keyMetrics\":{\"totalRegistrations\":1000,\"totalRevenue\":50000},\"forecasts\":{\"nextWeekRegistrations\":1100}}";
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn(aiResponse);
        when(reportRepository.save(any())).thenAnswer(invocation -> {
            OperationAiReport report = invocation.getArgument(0);
            report.setId(1L);
            return report;
        });

        Map<String, Object> result = operationService.generateReport("daily", null, null);

        assertNotNull(result);
        verify(reportRepository).save(any(OperationAiReport.class));
    }

    // ========== getReportDetail() ==========

    @Test
    void getReportDetail_ShouldReturnReport_WhenExists() {
        OperationAiReport report = new OperationAiReport();
        report.setId(1L);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        OperationAiReport result = operationService.getReportDetail(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getReportDetail_ShouldReturnNull_WhenNotExists() {
        when(reportRepository.findById(99L)).thenReturn(Optional.empty());

        OperationAiReport result = operationService.getReportDetail(99L);

        assertNull(result);
    }

    // ========== getReportList() ==========

    @Test
    void getReportList_ShouldFilterByType() {
        Page<OperationAiReport> page = new PageImpl<>(List.of(new OperationAiReport()));
        when(reportRepository.findByReportTypeOrderByCreateTimeDesc(eq("daily"), any(Pageable.class)))
                .thenReturn(page);

        Page<OperationAiReport> result = operationService.getReportList("daily", 1, 10);

        assertEquals(1, result.getContent().size());
        verify(reportRepository).findByReportTypeOrderByCreateTimeDesc(eq("daily"), any(Pageable.class));
    }

    @Test
    void getReportList_ShouldReturnAll_WhenTypeIsNull() {
        Page<OperationAiReport> page = new PageImpl<>(List.of(new OperationAiReport()));
        when(reportRepository.findByOrderByCreateTimeDesc(any(Pageable.class))).thenReturn(page);

        Page<OperationAiReport> result = operationService.getReportList(null, 1, 10);

        assertEquals(1, result.getContent().size());
        verify(reportRepository).findByOrderByCreateTimeDesc(any(Pageable.class));
    }

    @Test
    void getReportList_ShouldReturnAll_WhenTypeIsEmpty() {
        Page<OperationAiReport> page = new PageImpl<>(List.of(new OperationAiReport()));
        when(reportRepository.findByOrderByCreateTimeDesc(any(Pageable.class))).thenReturn(page);

        Page<OperationAiReport> result = operationService.getReportList("", 1, 10);

        assertEquals(1, result.getContent().size());
    }

    // ========== getOperationOverview() ==========

    @Test
    void getOperationOverview_ShouldReturnLatestMetrics() {
        OperationAiReport report = new OperationAiReport();
        report.setId(1L);
        report.setSummary("运营良好");
        report.setKeyMetrics("{\"totalRegistrations\":1250}");
        report.setForecasts("{\"trend\":\"up\"}");
        report.setWarnings("[{\"level\":\"info\",\"content\":\"系统正常\"}]");

        when(reportRepository.findAll(any(Sort.class))).thenReturn(List.of(report));

        Map<String, Object> result = operationService.getOperationOverview();

        assertNotNull(result);
        assertEquals("运营良好", result.get("summary"));
        assertNotNull(result.get("keyMetrics"));
        assertNotNull(result.get("forecasts"));
        assertNotNull(result.get("warnings"));
    }

    @Test
    void getOperationOverview_ShouldReturnEmptyWarnings_WhenParseError() {
        OperationAiReport report = new OperationAiReport();
        report.setId(1L);
        report.setSummary("报告");
        report.setWarnings("invalid json");

        when(reportRepository.findAll(any(Sort.class))).thenReturn(List.of(report));

        Map<String, Object> result = operationService.getOperationOverview();

        assertNotNull(result);
        assertNotNull(result.get("warnings"));
    }

    @Test
    void generateReport_ShouldHandlePartialJson_WithOnlySummary() {
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn("{\"summary\":\"仅摘要\"}");
        when(reportRepository.save(any())).thenAnswer(invocation -> {
            OperationAiReport report = invocation.getArgument(0);
            report.setId(1L);
            return report;
        });

        Map<String, Object> result = operationService.generateReport("weekly", "2026-01-01", "2026-01-07");

        assertNotNull(result);
        assertEquals("仅摘要", result.get("summary"));
    }

    @Test
    void generateReport_ShouldSetDefaultForecastType_WhenTypeIsNull() {
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn("{\"summary\":\"报告\"}");
        when(reportRepository.save(any())).thenAnswer(invocation -> {
            OperationAiReport report = invocation.getArgument(0);
            report.setId(1L);
            return report;
        });

        Map<String, Object> result = operationService.generateReport(null, "2026-01-01", "2026-01-07");

        assertEquals("monthly", result.get("forecastType"));
    }

    @Test
    void generateReport_ShouldHandleKeyMetricsNull() {
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn("{\"summary\":\"报告\",\"forecasts\":{\"trend\":\"up\"}}");
        when(reportRepository.save(any())).thenAnswer(invocation -> {
            OperationAiReport report = invocation.getArgument(0);
            report.setId(1L);
            return report;
        });

        Map<String, Object> result = operationService.generateReport("daily", "2026-01-01", "2026-01-07");

        assertNotNull(result);
        verify(reportRepository).save(any(OperationAiReport.class));
    }

    @Test
    void getReportList_ShouldReturnAll_WhenReportTypeIsNull() {
        when(reportRepository.findByOrderByCreateTimeDesc(any(Pageable.class))).thenReturn(Page.empty());

        var result = operationService.getReportList(null, 1, 10);

        assertNotNull(result);
        verify(reportRepository).findByOrderByCreateTimeDesc(any(Pageable.class));
    }

    @Test
    void getReportList_ShouldFilterByType_WhenProvided() {
        when(reportRepository.findByReportTypeOrderByCreateTimeDesc(eq("daily"), any(Pageable.class))).thenReturn(Page.empty());

        var result = operationService.getReportList("daily", 1, 10);

        assertNotNull(result);
        verify(reportRepository).findByReportTypeOrderByCreateTimeDesc(eq("daily"), any(Pageable.class));
    }
}
