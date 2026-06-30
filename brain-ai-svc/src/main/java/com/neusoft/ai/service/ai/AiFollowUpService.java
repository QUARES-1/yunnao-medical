package com.neusoft.ai.service.ai;

import com.neusoft.ai.entity.FollowUpPlan;
import com.neusoft.ai.entity.FollowUpRecord;
import org.springframework.data.domain.Page;
import java.util.Map;

public interface AiFollowUpService {
    FollowUpPlan createPlan(Map<String, Object> request, Long doctorId);
    Page<FollowUpPlan> getPatientPlans(Long patientId, Integer page, Integer size);
    Page<FollowUpRecord> getPendingRecords(Long patientId, Integer page, Integer size);
    String submitRecord(Long id, String answerJson, Long patientId);
    Map<String, Object> getDetail(Long id);
    Page<FollowUpPlan> getDoctorList(Long doctorId, Integer page, Integer size);
    String doctorReply(Long id, String remark, Long doctorId);
}
