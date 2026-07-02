package com.neusoft.cloud_brain_diagnosis.service.ai;

import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.common.util.AiApiUtil;
import com.neusoft.cloud_brain_diagnosis.entity.Doctor;
import com.neusoft.cloud_brain_diagnosis.entity.MedicalRecordAiGenerate;
import com.neusoft.cloud_brain_diagnosis.repository.DoctorRepository;
import com.neusoft.cloud_brain_diagnosis.repository.MedicalRecordAiGenerateRepository;
import com.neusoft.cloud_brain_diagnosis.service.ai.PromptTemplateService;
import com.neusoft.cloud_brain_diagnosis.service.ai.impl.AiMedicalRecordServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiMedicalRecordServiceImplTest {

    @Mock private MedicalRecordAiGenerateRepository generateRepository;
    @Mock private AiApiUtil aiApiUtil;
    @Mock private PromptTemplateService promptTemplateService;
    @Mock private DoctorRepository doctorRepository;

    private AiMedicalRecordServiceImpl medicalRecordService;

    @BeforeEach
    void setUp() {
        medicalRecordService = new AiMedicalRecordServiceImpl(
                generateRepository, aiApiUtil, promptTemplateService, doctorRepository);
    }

    // ========== generateRecord() ==========

    @Test
    void generateRecord_ShouldReturnParsedFields_WhenAiReturnsValidJson() {
        Doctor doctor = new Doctor();
        doctor.setId(10L);
        doctor.setDepartmentName("内科");

        when(doctorRepository.findById(10L)).thenReturn(Optional.of(doctor));
        when(promptTemplateService.getMedicalRecordTemplate(eq("内科"), anyString()))
                .thenReturn(Map.of("system", "sys", "user", "user"));
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"chiefComplaint\":\"头痛3天\",\"presentIllness\":\"轻微头痛\",\"pastHistory\":\"无\",\"physicalExamination\":\"神清\",\"diagnosis\":\"偏头痛\",\"treatment\":\"建议休息\"}");
        when(generateRepository.save(any())).thenAnswer(inv -> {
            MedicalRecordAiGenerate g = inv.getArgument(0);
            g.setId(100L);
            return g;
        });

        Map<String, Object> result = medicalRecordService.generateRecord(1L, "患者说头痛3天", "keyword", 10L);

        assertEquals("头痛3天", result.get("chiefComplaint"));
        assertEquals("轻微头痛", result.get("presentIllness"));
        assertEquals("偏头痛", result.get("diagnosis"));
        assertNotNull(result.get("id"));
    }

    @Test
    void generateRecord_ShouldHandleParseError_WhenResponseIsInvalid() {
        when(doctorRepository.findById(10L)).thenReturn(Optional.empty());
        when(promptTemplateService.getMedicalRecordTemplate(any(), any()))
                .thenReturn(Map.of("system", "sys", "user", "user"));
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn("非JSON响应");
        when(generateRepository.save(any())).thenAnswer(inv -> {
            MedicalRecordAiGenerate g = inv.getArgument(0);
            g.setId(100L);
            return g;
        });

        Map<String, Object> result = medicalRecordService.generateRecord(1L, "测试", "keyword", 10L);

        assertEquals("非JSON响应", result.get("presentIllness"));
        assertTrue(result.get("chiefComplaint").toString().isEmpty());
    }

    @Test
    void generateRecord_ShouldSaveAllFields() {
        when(doctorRepository.findById(10L)).thenReturn(Optional.empty());
        when(promptTemplateService.getMedicalRecordTemplate(any(), any()))
                .thenReturn(Map.of("system", "sys", "user", "user"));
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"chiefComplaint\":\"主诉\",\"presentIllness\":\"现病史\",\"pastHistory\":\"\",\"physicalExamination\":\"\",\"diagnosis\":\"诊断\",\"treatment\":\"\"}");
        when(generateRepository.save(any())).thenAnswer(inv -> {
            MedicalRecordAiGenerate g = inv.getArgument(0);
            g.setId(100L);
            return g;
        });

        medicalRecordService.generateRecord(1L, "测试", "dialogue", 10L);

        verify(generateRepository).save(argThat(g ->
                g.getPatientId().equals(1L) &&
                g.getDoctorId().equals(10L) &&
                "测试".equals(g.getInputText()) &&
                "dialogue".equals(g.getInputType())
        ));
    }

    // ========== getGenerateDetail() ==========

    @Test
    void getGenerateDetail_ShouldReturnRecord_WhenExists() {
        MedicalRecordAiGenerate gen = new MedicalRecordAiGenerate();
        gen.setId(1L);

        when(generateRepository.findById(1L)).thenReturn(Optional.of(gen));

        assertEquals(1L, medicalRecordService.getGenerateDetail(1L).getId());
    }

    @Test
    void getGenerateDetail_ShouldThrow_WhenNotFound() {
        when(generateRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> medicalRecordService.getGenerateDetail(99L));
    }

    // ========== Branch Coverage ==========

    @Test
    void generateRecord_ShouldHandleNullDoctorId() {
        when(promptTemplateService.getMedicalRecordTemplate(eq(null), any()))
                .thenReturn(Map.of("system", "sys", "user", "user"));
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"chiefComplaint\":\"主诉\",\"presentIllness\":\"现病史\"}");
        when(generateRepository.save(any())).thenAnswer(inv -> {
            MedicalRecordAiGenerate g = inv.getArgument(0);
            g.setId(100L);
            return g;
        });

        Map<String, Object> result = medicalRecordService.generateRecord(1L, "头痛", "keyword", null);

        assertNotNull(result.get("id"));
        assertEquals("", result.get("chiefComplaint"));
    }

    @Test
    void generateRecord_ShouldHandleDoctorRepositoryException() {
        when(doctorRepository.findById(10L)).thenThrow(new RuntimeException("DB error"));
        when(promptTemplateService.getMedicalRecordTemplate(any(), any()))
                .thenReturn(Map.of("system", "sys", "user", "user"));
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"chiefComplaint\":\"主诉\",\"presentIllness\":\"现病史\"}");
        when(generateRepository.save(any())).thenAnswer(inv -> {
            MedicalRecordAiGenerate g = inv.getArgument(0);
            g.setId(100L);
            return g;
        });

        // Should not throw, getDoctorDepartmentName catches exception and returns null
        Map<String, Object> result = medicalRecordService.generateRecord(1L, "头痛", "keyword", 10L);

        assertNotNull(result.get("id"));
    }

    @Test
    void generateRecord_ShouldHandleNullInputText() {
        when(promptTemplateService.getMedicalRecordTemplate(eq(null), eq(null)))
                .thenReturn(Map.of("system", "sys", "user", "user"));
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"chiefComplaint\":\"\",\"presentIllness\":\"\"}");
        when(generateRepository.save(any())).thenAnswer(inv -> {
            MedicalRecordAiGenerate g = inv.getArgument(0);
            g.setId(100L);
            return g;
        });

        Map<String, Object> result = medicalRecordService.generateRecord(1L, null, null, 10L);

        assertNotNull(result.get("id"));
    }
}
