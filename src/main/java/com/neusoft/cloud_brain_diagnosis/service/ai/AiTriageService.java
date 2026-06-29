package com.neusoft.cloud_brain_diagnosis.service.ai;

import com.neusoft.cloud_brain_diagnosis.entity.TriageRecord;
import org.springframework.data.domain.Page;

import java.util.Map;

public interface AiTriageService {
    /**
     * 智能分诊：输入症状，返回推荐科室和医生
     */
    Map<String, Object> consult(String chiefComplaint, Long patientId);

    /**
     * 获取患者的分诊历史记录
     */
    Page<TriageRecord> getPatientList(Long patientId, Integer page, Integer size);

    /**
     * 获取分诊详情
     */
    TriageRecord getDetail(Long id);
}
