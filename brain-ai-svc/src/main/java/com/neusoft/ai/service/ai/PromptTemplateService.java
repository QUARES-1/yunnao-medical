package com.neusoft.ai.service.ai;

import java.util.Map;

public interface PromptTemplateService {
    Map<String, String> getMedicalRecordTemplate(String departmentName, String inputText);
    Map<String, String> getPrescriptionReviewTemplate(String departmentName, String patientInfo, String drugsDesc);
}
