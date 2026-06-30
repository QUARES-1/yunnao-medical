package com.neusoft.ai.service.ai;

import com.neusoft.ai.entity.OperationAiReport;
import org.springframework.data.domain.Page;
import java.util.Map;

public interface AiOperationService {
    Map<String, Object> generateReport(String reportType, String startDate, String endDate);
    OperationAiReport getReportDetail(Long id);
    Page<OperationAiReport> getReportList(String reportType, Integer page, Integer size);
    Map<String, Object> getOperationOverview();
}
