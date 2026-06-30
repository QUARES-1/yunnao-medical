package com.neusoft.ai.service.ai.impl;

import com.neusoft.ai.service.ai.PromptTemplateService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class PromptTemplateServiceImpl implements PromptTemplateService {

    @Value("${ai.prompts.medical-record.default.system:}")
    private String defaultRecordSystem;

    @Value("${ai.prompts.medical-record.default.user:}")
    private String defaultRecordUser;

    @Value("${ai.prompts.medical-record.pediatric.system:}")
    private String pediatricRecordSystem;

    @Value("${ai.prompts.medical-record.cardiology.system:}")
    private String cardiologyRecordSystem;

    @Value("${ai.prompts.prescription-review.default.system:}")
    private String defaultReviewSystem;

    @Value("${ai.prompts.prescription-review.default.user:}")
    private String defaultReviewUser;

    @Override
    public Map<String, String> getMedicalRecordTemplate(String departmentName, String inputText) {
        String systemPrompt;
        String userPrompt;

        if (departmentName != null) {
            String dept = departmentName.replace(" ", "").toLowerCase();
            if (dept.contains("儿科") || dept.contains("pediatric") || dept.contains("儿")) {
                systemPrompt = pediatricRecordSystem;
            } else if (dept.contains("心内") || dept.contains("cardiology") || dept.contains("心血")) {
                systemPrompt = cardiologyRecordSystem;
            } else if (dept.contains("呼吸") || dept.contains("respiratory")) {
                systemPrompt = getProperty("ai.prompts.medical-record.respiratory.system");
            } else if (dept.contains("骨科") || dept.contains("orthopedics")) {
                systemPrompt = getProperty("ai.prompts.medical-record.orthopedics.system");
            } else if (dept.contains("耳鼻") || dept.contains("ent")) {
                systemPrompt = getProperty("ai.prompts.medical-record.ent.system");
            } else {
                systemPrompt = defaultRecordSystem;
            }
        } else {
            systemPrompt = defaultRecordSystem;
        }

        if (systemPrompt == null || systemPrompt.isBlank()) {
            systemPrompt = "你是一名经验丰富的临床医生，请根据输入的对话/关键词生成一份规范的病历。请严格按以下JSON格式返回：{\"chiefComplaint\":\"主诉\",\"presentIllness\":\"现病史\",\"pastHistory\":\"既往史\",\"physicalExamination\":\"体格检查\",\"diagnosis\":\"诊断\",\"treatment\":\"治疗意见\"}";
        }

        userPrompt = defaultRecordUser != null ? defaultRecordUser.replace("{inputText}", inputText) : "请根据以下信息生成一份完整的门（急）诊病历：\n" + inputText;

        Map<String, String> result = new HashMap<>();
        result.put("system", systemPrompt);
        result.put("user", userPrompt);
        return result;
    }

    @Override
    public Map<String, String> getPrescriptionReviewTemplate(String departmentName, String patientInfo, String drugsDesc) {
        String systemPrompt = defaultReviewSystem;
        String userPrompt = defaultReviewUser;

        if (systemPrompt == null || systemPrompt.isBlank()) {
            systemPrompt = "你是一名经验丰富的临床药师，请严格审核处方中的药品。请按以下JSON格式返回审核结果：{\"reviewResult\":\"pass/warning/reject\",\"reviewScore\":0-100,\"warnings\":[],\"suggestions\":\"\",\"drugInteractions\":[],\"allergyRisks\":[],\"dosageIssues\":[]}";
        }
        if (userPrompt == null || userPrompt.isBlank()) {
            userPrompt = "请审核以下处方：\n{patientInfo}\n药品列表：\n{drugsDesc}";
        }

        userPrompt = userPrompt.replace("{patientInfo}", patientInfo).replace("{drugsDesc}", drugsDesc);

        Map<String, String> result = new HashMap<>();
        result.put("system", systemPrompt);
        result.put("user", userPrompt);
        return result;
    }

    private String getProperty(String key) {
        try {
            var env = new org.springframework.core.env.StandardEnvironment();
            return env.getProperty(key);
        } catch (Exception e) {
            return null;
        }
    }
}
