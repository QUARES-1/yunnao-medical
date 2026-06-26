package com.neusoft.cloud_brain_diagnosis.service.ai;

import com.neusoft.cloud_brain_diagnosis.entity.PrescriptionAiReview;
import org.springframework.data.domain.Page;

import java.util.Map;

public interface AiPrescriptionService {
    /**
     * AI审核处方
     */
    Map<String, Object> checkPrescription(Map<String, Object> request, Long doctorId);

    /**
     * 获取审核记录列表
     */
    Page<PrescriptionAiReview> getReviewList(Long doctorId, Integer page, Integer size);

    /**
     * 获取审核详情
     */
    PrescriptionAiReview getReviewDetail(Long id);
}
