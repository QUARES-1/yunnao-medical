package com.neusoft.ai.service.ai;

import com.neusoft.ai.entity.ExaminationAiInterpretation;
import org.springframework.data.domain.Page;
import java.util.Map;

public interface AiExaminationService {
    Map<String, Object> interpret(Long examinationId, Long patientId);
    Page<ExaminationAiInterpretation> getPatientInterpretation(Long patientId, Integer page, Integer size);
    Map<String, Object> getProInterpretation(Long id);
    Page<Map<String, Object>> getCriticalList(String status, Integer page, Integer size);
    Map<String, Object> detectCriticalValue(Long examinationId);
    Map<String, Object> confirmWarning(Long warningId);
    Map<String, Object> processWarning(Long warningId, String note);
    Page<Map<String, Object>> getCriticalHistory(Long patientId, Integer page, Integer size);
    Map<String, Object> reviewExamination(Map<String, Object> request, Long doctorId);
    Page<Map<String, Object>> getManualList(Long doctorId, Integer page, Integer size);
    Page<Map<String, Object>> getReviewList(Long doctorId, Integer page, Integer size);
    Map<String, Object> getReviewDetail(Long id);
    Map<String, Object> manualConfirm(Long reviewId);
    Map<String, Object> reject(Long reviewId);
    Map<String, Object> getReviewStats();
}
