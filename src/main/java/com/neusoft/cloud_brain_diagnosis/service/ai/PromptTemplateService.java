package com.neusoft.cloud_brain_diagnosis.service.ai;

import java.util.Map;

/**
 * 可配置的 AI 提示词模板服务
 * <p>
 * 针对不同科室（儿科、心内科等）优化病历生成与处方审核的提示词质量。
 */
public interface PromptTemplateService {

    /**
     * 获取病历生成模板
     *
     * @param departmentName 科室名称，如 "儿科"、"心血管内科"；为 null 或空时返回默认模板
     * @param inputText      用户输入的原始文本
     * @return systemPrompt + userPrompt 的键值对
     */
    Map<String, String> getMedicalRecordTemplate(String departmentName, String inputText);

    /**
     * 获取处方审核模板
     *
     * @param departmentName 科室名称；为 null 或空时返回默认模板
     * @param patientInfo    患者信息描述
     * @param drugsDesc      药品列表描述
     * @return systemPrompt + userPrompt 的键值对
     */
    Map<String, String> getPrescriptionReviewTemplate(String departmentName, String patientInfo, String drugsDesc);
}
