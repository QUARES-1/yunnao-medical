package com.neusoft.cloud_brain_diagnosis.service.ai;

import com.neusoft.cloud_brain_diagnosis.entity.QualityCheckRecord;
import com.neusoft.cloud_brain_diagnosis.entity.QualityCheckDetail;
import com.neusoft.cloud_brain_diagnosis.entity.OperationAiReport;
import org.springframework.data.domain.Page;

import java.util.Map;

public interface AiOperationService {
    // ========== 运营分析 ==========
    /**
     * 生成运营报告
     */
    Map<String, Object> generateReport(String reportType, String startDate, String endDate);

    /**
     * 运营报告详情
     */
    OperationAiReport getReportDetail(Long id);

    /**
     * 报告列表
     */
    Page<OperationAiReport> getReportList(String reportType, Integer page, Integer size);

    /**
     * 首页AI概览
     */
    Map<String, Object> getOperationOverview();
}

