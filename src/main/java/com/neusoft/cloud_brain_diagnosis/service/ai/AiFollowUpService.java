package com.neusoft.cloud_brain_diagnosis.service.ai;

import com.neusoft.cloud_brain_diagnosis.entity.FollowUpPlan;
import com.neusoft.cloud_brain_diagnosis.entity.FollowUpRecord;
import org.springframework.data.domain.Page;

import java.util.Map;

public interface AiFollowUpService {
    /**
     * 创建随访计划
     */
    FollowUpPlan createPlan(Map<String, Object> request, Long doctorId);

    /**
     * 患者-我的随访计划
     */
    Page<FollowUpPlan> getPatientPlans(Long patientId, Integer page, Integer size);

    /**
     * 患者-待随访列表
     */
    Page<FollowUpRecord> getPendingRecords(Long patientId, Integer page, Integer size);

    /**
     * 提交随访问卷
     */
    String submitRecord(Long id, String answerJson, Long patientId);

    /**
     * 随访详情
     */
    Map<String, Object> getDetail(Long id);

    /**
     * 医生-我负责的随访
     */
    Page<FollowUpPlan> getDoctorList(Long doctorId, Integer page, Integer size);

    /**
     * 医生回复异常随访
     */
    String doctorReply(Long id, String remark, Long doctorId);
}
