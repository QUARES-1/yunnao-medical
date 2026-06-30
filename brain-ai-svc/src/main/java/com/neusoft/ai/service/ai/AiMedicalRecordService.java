package com.neusoft.ai.service.ai;

import com.neusoft.ai.entity.MedicalRecordAiGenerate;
import org.springframework.data.domain.Page;
import java.util.Map;

public interface AiMedicalRecordService {
    Map<String, Object> generateRecord(Long patientId, String inputText, String inputType, Long doctorId);
    Page<MedicalRecordAiGenerate> getGenerateList(Long doctorId, Integer page, Integer size);
    MedicalRecordAiGenerate getGenerateDetail(Long id);
}
