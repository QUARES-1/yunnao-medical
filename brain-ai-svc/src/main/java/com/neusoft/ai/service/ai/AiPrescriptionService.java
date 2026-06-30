package com.neusoft.ai.service.ai;

import com.neusoft.ai.entity.PrescriptionAiReview;
import org.springframework.data.domain.Page;
import java.util.Map;

public interface AiPrescriptionService {
    Map<String, Object> checkPrescription(Map<String, Object> request, Long doctorId);
    Page<PrescriptionAiReview> getReviewList(Long doctorId, Integer page, Integer size);
    PrescriptionAiReview getReviewDetail(Long id);
}
