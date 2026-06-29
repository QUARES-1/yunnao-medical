package com.neusoft.cloud_brain_diagnosis.service.ai;

import java.util.Map;

public interface AiMedicationService {
    /**
     * 生成用药指导
     */
    Map<String, Object> generateGuide(Long prescriptionId);

    /**
     * 查看用药指导
     */
    Map<String, Object> getGuide(Long prescriptionId);

    /**
     * 标记已打印
     */
    String markPrinted(Long id);
}
