package com.neusoft.cloud_brain_diagnosis.service.ai;

import com.neusoft.cloud_brain_diagnosis.entity.ExaminationAiInterpretation;
import com.neusoft.cloud_brain_diagnosis.entity.ExaminationAiReview;
import com.neusoft.cloud_brain_diagnosis.entity.CriticalValueWarning;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface AiExaminationService {
    // ========== 检验报告解读 ==========
    /**
     * 生成AI解读报告
     */
    Map<String, Object> interpret(Long examinationId);

    /**
     * 查看患者版解读
     */
    ExaminationAiInterpretation getPatientInterpretation(Long examinationId);

    /**
     * 查看专业版解读
     */
    ExaminationAiInterpretation getProInterpretation(Long examinationId);

    // ========== 危急值预警 ==========
    /**
     * 待处理预警列表
     */
    Page<CriticalValueWarning> getCriticalList(Long userId, String role, Integer page, Integer size);

    /**
     * 检验结果发布后自动识别并生成危急值预警
     */
    CriticalValueWarning detectCriticalValue(Long examinationId);

    /**
     * 确认预警
     */
    String confirmWarning(Long id, Long doctorId);

    /**
     * 处理预警
     */
    String processWarning(Long id, String remark, Long doctorId);

    /**
     * 历史预警列表
     */
    Page<CriticalValueWarning> getCriticalHistory(Integer page, Integer size);

    // ========== 检验AI审核 ==========
    /**
     * AI审核检验结果
     */
    Map<String, Object> reviewExamination(Long examinationId, Long labStaffId);

    /**
     * 需要人工复核列表
     */
    Page<ExaminationAiReview> getManualList(Integer page, Integer size);

    /**
     * AI审核记录列表
     */
    Page<ExaminationAiReview> getReviewList(String reviewResult, Integer page, Integer size);

    /**
     * 审核详情
     */
    ExaminationAiReview getReviewDetail(Long id);

    /**
     * 人工确认审核结果
     */
    String manualConfirm(Long id);

    /**
     * 退回重测
     */
    String reject(Long id, String reason);

    /**
     * 审核统计
     */
    Map<String, Object> getReviewStats();
}
