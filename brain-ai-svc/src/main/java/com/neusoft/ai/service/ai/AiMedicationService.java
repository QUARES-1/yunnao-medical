package com.neusoft.ai.service.ai;

import java.util.Map;

public interface AiMedicationService {
    Map<String, Object> getGuide(Long prescriptionId, Long patientId);
}
