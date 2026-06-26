package com.neusoft.cloud_brain_diagnosis.service.ai;

import com.neusoft.cloud_brain_diagnosis.entity.MedicalRecordAiGenerate;
import org.springframework.data.domain.Page;

import java.util.Map;

public interface AiMedicalRecordService {
    /**
     * AI生成病历
     */
    Map<String, Object> generateRecord(Long patientId, String inputText, String inputType, Long doctorId);

    /**
     * 获取生成记录列表
     */
    Page<MedicalRecordAiGenerate> getGenerateList(Long doctorId, Integer page, Integer size);

    /**
     * 获取生成记录详情
     */
    MedicalRecordAiGenerate getGenerateDetail(Long id);
}
