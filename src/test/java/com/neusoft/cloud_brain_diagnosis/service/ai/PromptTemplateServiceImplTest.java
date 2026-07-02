package com.neusoft.cloud_brain_diagnosis.service.ai;

import com.neusoft.cloud_brain_diagnosis.service.ai.PromptTemplateService;
import com.neusoft.cloud_brain_diagnosis.service.ai.impl.PromptTemplateServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PromptTemplateServiceImplTest {

    private PromptTemplateServiceImpl templateService;

    @BeforeEach
    void setUp() {
        templateService = new PromptTemplateServiceImpl();
    }

    // ========== getMedicalRecordTemplate() ==========

    @Test
    void getMedicalRecordTemplate_ShouldReturnDefaultTemplate_WhenDepartmentIsNull() {
        ReflectionTestUtils.setField(templateService, "mrDefaultSystem", "");
        ReflectionTestUtils.setField(templateService, "mrDefaultUser", "");

        Map<String, String> result = templateService.getMedicalRecordTemplate(null, "患者主诉头痛");

        assertTrue(result.containsKey("system"));
        assertTrue(result.containsKey("user"));
        assertTrue(result.get("system").contains("病历"));
        assertTrue(result.get("user").contains("患者主诉头痛"));
    }

    @Test
    void getMedicalRecordTemplate_ShouldReturnDefaultTemplate_WhenDepartmentIsUnknown() {
        // When configured values are empty, the fallback template is used
        ReflectionTestUtils.setField(templateService, "mrDefaultSystem", "");
        ReflectionTestUtils.setField(templateService, "mrDefaultUser", "");

        Map<String, String> result = templateService.getMedicalRecordTemplate("未知科室", "测试输入");

        // The fallback user template contains {inputText} replaced with the input
        assertTrue(result.get("user").contains("测试输入"));
        assertTrue(result.get("system").contains("病历"));
    }

    @Test
    void getMedicalRecordTemplate_ShouldReplaceInputText() {
        ReflectionTestUtils.setField(templateService, "mrDefaultSystem", "");
        ReflectionTestUtils.setField(templateService, "mrDefaultUser", "");

        Map<String, String> result = templateService.getMedicalRecordTemplate(null, "头痛3天");

        assertTrue(result.get("user").contains("头痛3天"));
    }

    // ========== getPrescriptionReviewTemplate() ==========

    @Test
    void getPrescriptionReviewTemplate_ShouldReturnTemplate_WithPatientInfo() {
        ReflectionTestUtils.setField(templateService, "prDefaultSystem", "");
        ReflectionTestUtils.setField(templateService, "prDefaultUser", "");

        Map<String, String> result = templateService.getPrescriptionReviewTemplate(
                null, "患者信息：年龄45岁，性别男", "阿司匹林 100mg");

        assertTrue(result.containsKey("system"));
        assertTrue(result.containsKey("user"));
        assertTrue(result.get("system").contains("审核"));
        assertTrue(result.get("user").contains("患者信息"));
        assertTrue(result.get("user").contains("阿司匹林"));
    }

    @Test
    void getPrescriptionReviewTemplate_ShouldHandleNullPatientInfo() {
        ReflectionTestUtils.setField(templateService, "prDefaultSystem", "");
        ReflectionTestUtils.setField(templateService, "prDefaultUser", "");

        Map<String, String> result = templateService.getPrescriptionReviewTemplate(
                null, null, null);

        assertNotNull(result.get("user"));
    }

    @Test
    void getPrescriptionReviewTemplate_ShouldReplaceVariables() {
        ReflectionTestUtils.setField(templateService, "prDefaultSystem", "");
        ReflectionTestUtils.setField(templateService, "prDefaultUser", "");

        Map<String, String> result = templateService.getPrescriptionReviewTemplate(
                null, "年龄60岁", "布洛芬 200mg");

        assertTrue(result.get("user").contains("年龄60岁"));
        assertTrue(result.get("user").contains("布洛芬"));
    }

    // ========== resolveDeptKey() ==========

    @Test
    void getMedicalRecordTemplate_ShouldMapPediatricDepartment() {
        ReflectionTestUtils.setField(templateService, "mrPediatricSystem", "");
        ReflectionTestUtils.setField(templateService, "mrPediatricUser", "");
        ReflectionTestUtils.setField(templateService, "mrDefaultSystem", "");
        ReflectionTestUtils.setField(templateService, "mrDefaultUser", "");

        Map<String, String> result = templateService.getMedicalRecordTemplate("儿科", "测试");

        assertNotNull(result);
    }

    @Test
    void getMedicalRecordTemplate_ShouldMapCardiologyDepartment() {
        ReflectionTestUtils.setField(templateService, "mrCardiologySystem", "");
        ReflectionTestUtils.setField(templateService, "mrCardiologyUser", "");
        ReflectionTestUtils.setField(templateService, "mrDefaultSystem", "");
        ReflectionTestUtils.setField(templateService, "mrDefaultUser", "");

        Map<String, String> result = templateService.getMedicalRecordTemplate("心血管内科", "测试");

        assertNotNull(result);
    }

    @Test
    void getMedicalRecordTemplate_ShouldMapRespiratoryDepartment() {
        ReflectionTestUtils.setField(templateService, "mrRespiratorySystem", "");
        ReflectionTestUtils.setField(templateService, "mrRespiratoryUser", "");
        ReflectionTestUtils.setField(templateService, "mrDefaultSystem", "");
        ReflectionTestUtils.setField(templateService, "mrDefaultUser", "");

        Map<String, String> result = templateService.getMedicalRecordTemplate("呼吸内科", "测试");

        assertNotNull(result);
    }
}
