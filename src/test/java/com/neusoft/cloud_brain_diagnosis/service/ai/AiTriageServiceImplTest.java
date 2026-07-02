package com.neusoft.cloud_brain_diagnosis.service.ai;

import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.common.util.AiApiUtil;
import com.neusoft.cloud_brain_diagnosis.entity.Doctor;
import com.neusoft.cloud_brain_diagnosis.entity.TriageRecord;
import com.neusoft.cloud_brain_diagnosis.repository.DoctorRepository;
import com.neusoft.cloud_brain_diagnosis.repository.TriageRecordRepository;
import com.neusoft.cloud_brain_diagnosis.service.ai.impl.AiTriageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiTriageServiceImplTest {

    @Mock private TriageRecordRepository triageRecordRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private AiApiUtil aiApiUtil;

    private AiTriageServiceImpl triageService;

    @BeforeEach
    void setUp() {
        triageService = new AiTriageServiceImpl(triageRecordRepository, doctorRepository, aiApiUtil);
    }

    // ========== consult() ==========

    @Test
    void consult_ShouldReturnRecommendation_WithValidJson() {
        String aiResponse = "{\"recommendDepartment\":\"神经内科\",\"recommendDepartmentId\":8,\"recommendDoctorIds\":\"\",\"analysis\":\"头痛建议看神经内科\",\"confidence\":85}";
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn(aiResponse);
        when(triageRecordRepository.save(any())).thenAnswer(inv -> {
            TriageRecord r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        Map<String, Object> result = triageService.consult("头痛", 1L);

        assertEquals("神经内科", result.get("recommendDepartment"));
        assertEquals(8L, result.get("recommendDepartmentId"));
        assertEquals(85, result.get("confidence"));
        assertNotNull(result.get("id"));
    }

    @Test
    void consult_ShouldNormalizeDepartment_ForEyeSymptoms() {
        String aiResponse = "{\"recommendDepartment\":\"内科\",\"recommendDepartmentId\":1,\"recommendDoctorIds\":\"\",\"analysis\":\"建议\",\"confidence\":80}";
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn(aiResponse);
        when(triageRecordRepository.save(any())).thenAnswer(inv -> {
            TriageRecord r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        Map<String, Object> result = triageService.consult("眼睛红肿", 1L);

        assertEquals("眼科", result.get("recommendDepartment"));
        assertEquals(4L, result.get("recommendDepartmentId"));
    }

    @Test
    void consult_ShouldNormalizeDepartment_ForStomachSymptoms() {
        String aiResponse = "{\"recommendDepartment\":\"全科\",\"recommendDepartmentId\":1,\"recommendDoctorIds\":\"\",\"analysis\":\"建议\",\"confidence\":80}";
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn(aiResponse);
        when(triageRecordRepository.save(any())).thenAnswer(inv -> {
            TriageRecord r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        Map<String, Object> result = triageService.consult("胃痛腹胀", 1L);

        assertEquals("消化内科", result.get("recommendDepartment"));
        assertEquals(11L, result.get("recommendDepartmentId"));
    }

    @Test
    void consult_ShouldMatchDoctorsByDepartment_WhenNoSpecificDoctorIds() {
        Doctor doc = new Doctor();
        doc.setId(20L);
        doc.setName("王医生");
        doc.setTitle("副主任医师");
        doc.setSpecialty("神经内科");

        String aiResponse = "{\"recommendDepartment\":\"神经内科\",\"recommendDepartmentId\":8,\"recommendDoctorIds\":\"\",\"analysis\":\"建议\",\"confidence\":80}";
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn(aiResponse);
        when(doctorRepository.findByDepartmentId(8L)).thenReturn(List.of(doc));
        when(triageRecordRepository.save(any())).thenAnswer(inv -> {
            TriageRecord r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        Map<String, Object> result = triageService.consult("头痛头晕", 1L);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> doctors = (List<Map<String, Object>>) result.get("recommendDoctors");
        assertFalse(doctors.isEmpty());
        assertEquals("王医生", doctors.get(0).get("name"));
    }

    @Test
    void consult_ShouldParseError_WithFallbackToRawResponse() {
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn("非JSON响应");
        when(triageRecordRepository.save(any())).thenAnswer(inv -> {
            TriageRecord r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        Map<String, Object> result = triageService.consult("头痛", 1L);

        assertNotNull(result.get("analysis"));
    }

    // ========== getDetail() ==========

    @Test
    void getDetail_ShouldReturnRecord_WhenExists() {
        TriageRecord record = new TriageRecord();
        record.setId(1L);

        when(triageRecordRepository.findById(1L)).thenReturn(Optional.of(record));

        assertEquals(1L, triageService.getDetail(1L).getId());
    }

    @Test
    void getDetail_ShouldThrow_WhenNotFound() {
        when(triageRecordRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> triageService.getDetail(99L));
    }

    // ========== Additional coverage tests ==========

    @Test
    void consult_ShouldHandleHighConfidence() {
        String aiResponse = "{\"recommendDepartment\":\"神经内科\",\"recommendDepartmentId\":8,\"recommendDoctorIds\":\"\",\"analysis\":\"头痛建议看神经内科\",\"confidence\":95}";
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn(aiResponse);
        when(triageRecordRepository.save(any())).thenAnswer(inv -> {
            TriageRecord r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        Map<String, Object> result = triageService.consult("剧烈头痛", 1L);

        assertEquals(95, result.get("confidence"));
    }

    @Test
    void consult_ShouldMatchSpecificDoctors_WhenDoctorIdsProvided() {
        Doctor doc = new Doctor();
        doc.setId(50L);
        doc.setName("李主任");
        doc.setTitle("主任医师");
        doc.setSpecialty("神经内科");

        String aiResponse = "{\"recommendDepartment\":\"神经内科\",\"recommendDepartmentId\":8,\"recommendDoctorIds\":\"50,51\",\"analysis\":\"建议\",\"confidence\":80}";
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn(aiResponse);
        when(doctorRepository.findById(50L)).thenReturn(Optional.of(doc));
        when(doctorRepository.findById(51L)).thenReturn(Optional.empty());
        when(triageRecordRepository.save(any())).thenAnswer(inv -> {
            TriageRecord r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        Map<String, Object> result = triageService.consult("头痛", 1L);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> doctors = (List<Map<String, Object>>) result.get("recommendDoctors");
        assertFalse(doctors.isEmpty());
        assertEquals("李主任", doctors.get(0).get("name"));
    }

    @Test
    void consult_ShouldHandleInvalidJsonResponse() {
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn("{invalid}");
        when(triageRecordRepository.save(any())).thenAnswer(inv -> {
            TriageRecord r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        Map<String, Object> result = triageService.consult("头痛", 1L);

        assertNotNull(result.get("analysis"));
    }

    // ========== Department Normalization Branches ==========

    @Test
    void consult_ShouldNormalizeToDental_ForTeethSymptoms() {
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"recommendDepartment\":\"内科\",\"recommendDepartmentId\":1,\"recommendDoctorIds\":\"\",\"analysis\":\"建议\",\"confidence\":80}");
        when(triageRecordRepository.save(any())).thenAnswer(inv -> {
            TriageRecord r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        Map<String, Object> result = triageService.consult("牙痛口腔溃疡", 1L);

        assertEquals("口腔科", result.get("recommendDepartment"));
        assertEquals(6L, result.get("recommendDepartmentId"));
    }

    @Test
    void consult_ShouldNormalizeToOtolaryngology_ForENTSymptoms() {
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"recommendDepartment\":\"内科\",\"recommendDepartmentId\":1,\"recommendDoctorIds\":\"\",\"analysis\":\"建议\",\"confidence\":80}");
        when(triageRecordRepository.save(any())).thenAnswer(inv -> {
            TriageRecord r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        Map<String, Object> result = triageService.consult("鼻塞嗓子痛", 1L);

        assertEquals("耳鼻喉科", result.get("recommendDepartment"));
        assertEquals(5L, result.get("recommendDepartmentId"));
    }

    @Test
    void consult_ShouldNormalizeToRespiratory_ForBreathingSymptoms() {
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"recommendDepartment\":\"内科\",\"recommendDepartmentId\":1,\"recommendDoctorIds\":\"\",\"analysis\":\"建议\",\"confidence\":80}");
        when(triageRecordRepository.save(any())).thenAnswer(inv -> {
            TriageRecord r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        Map<String, Object> result = triageService.consult("咳嗽胸闷气短", 1L);

        assertEquals("呼吸内科", result.get("recommendDepartment"));
        assertEquals(10L, result.get("recommendDepartmentId"));
    }

    @Test
    void consult_ShouldNormalizeToOrthopedics_ForBoneSymptoms() {
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"recommendDepartment\":\"内科\",\"recommendDepartmentId\":1,\"recommendDoctorIds\":\"\",\"analysis\":\"建议\",\"confidence\":80}");
        when(triageRecordRepository.save(any())).thenAnswer(inv -> {
            TriageRecord r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        Map<String, Object> result = triageService.consult("腰痛关节疼痛", 1L);

        assertEquals("骨科", result.get("recommendDepartment"));
        assertEquals(12L, result.get("recommendDepartmentId"));
    }

    @Test
    void consult_ShouldNormalizeToDermatology_ForSkinSymptoms() {
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"recommendDepartment\":\"内科\",\"recommendDepartmentId\":1,\"recommendDoctorIds\":\"\",\"analysis\":\"建议\",\"confidence\":80}");
        when(triageRecordRepository.save(any())).thenAnswer(inv -> {
            TriageRecord r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        Map<String, Object> result = triageService.consult("皮肤瘙痒红疹", 1L);

        assertEquals("皮肤科", result.get("recommendDepartment"));
        assertEquals(7L, result.get("recommendDepartmentId"));
    }

    @Test
    void consult_ShouldNormalizeToNeurology_ForNeurologicalSymptoms() {
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"recommendDepartment\":\"内科\",\"recommendDepartmentId\":1,\"recommendDoctorIds\":\"\",\"analysis\":\"建议\",\"confidence\":80}");
        when(triageRecordRepository.save(any())).thenAnswer(inv -> {
            TriageRecord r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        Map<String, Object> result = triageService.consult("头痛头晕失眠", 1L);

        assertEquals("神经内科", result.get("recommendDepartment"));
        assertEquals(8L, result.get("recommendDepartmentId"));
    }

    @Test
    void consult_ShouldNormalizeToCardiology_ForHeartSymptoms() {
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"recommendDepartment\":\"内科\",\"recommendDepartmentId\":1,\"recommendDoctorIds\":\"\",\"analysis\":\"建议\",\"confidence\":80}");
        when(triageRecordRepository.save(any())).thenAnswer(inv -> {
            TriageRecord r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        Map<String, Object> result = triageService.consult("心慌胸痛血压高", 1L);

        assertEquals("心血管内科", result.get("recommendDepartment"));
        assertEquals(9L, result.get("recommendDepartmentId"));
    }

    @Test
    void consult_ShouldNormalizeToPediatrics_ForChildSymptoms() {
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"recommendDepartment\":\"内科\",\"recommendDepartmentId\":1,\"recommendDoctorIds\":\"\",\"analysis\":\"建议\",\"confidence\":80}");
        when(triageRecordRepository.save(any())).thenAnswer(inv -> {
            TriageRecord r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        Map<String, Object> result = triageService.consult("孩子发烧", 1L);

        assertEquals("儿科", result.get("recommendDepartment"));
        assertEquals(2L, result.get("recommendDepartmentId"));
    }

    @Test
    void consult_ShouldNormalizeToEmergency_ForUrgentSymptoms() {
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"recommendDepartment\":\"内科\",\"recommendDepartmentId\":1,\"recommendDoctorIds\":\"\",\"analysis\":\"建议\",\"confidence\":80}");
        when(triageRecordRepository.save(any())).thenAnswer(inv -> {
            TriageRecord r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        Map<String, Object> result = triageService.consult("高热急症", 1L);

        assertEquals("急诊科", result.get("recommendDepartment"));
        assertEquals(14L, result.get("recommendDepartmentId"));
    }

    @Test
    void consult_ShouldNormalizeToDefault_WhenNoMatchAndPositiveId() {
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"recommendDepartment\":\"其他科\",\"recommendDepartmentId\":3,\"recommendDoctorIds\":\"\",\"analysis\":\"建议\",\"confidence\":80}");
        when(triageRecordRepository.save(any())).thenAnswer(inv -> {
            TriageRecord r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        Map<String, Object> result = triageService.consult("一般不适", 1L);

        assertEquals("其他科", result.get("recommendDepartment"));
        assertEquals(3L, result.get("recommendDepartmentId"));
    }

    @Test
    void consult_ShouldNormalizeToInternal_WhenNoMatchAndNegativeId() {
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"recommendDepartment\":\"其他科\",\"recommendDepartmentId\":-1,\"recommendDoctorIds\":\"\",\"analysis\":\"建议\",\"confidence\":80}");
        when(triageRecordRepository.save(any())).thenAnswer(inv -> {
            TriageRecord r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        Map<String, Object> result = triageService.consult("一般不适", 1L);

        assertEquals("内科", result.get("recommendDepartment"));
        assertEquals(20L, result.get("recommendDepartmentId"));
    }

    @Test
    void consult_ShouldHandleNullChiefComplaint() {
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"recommendDepartment\":\"内科\",\"recommendDepartmentId\":1,\"recommendDoctorIds\":\"\",\"analysis\":\"建议\",\"confidence\":80}");
        when(triageRecordRepository.save(any())).thenAnswer(inv -> {
            TriageRecord r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        Map<String, Object> result = triageService.consult(null, 1L);

        assertNotNull(result.get("recommendDepartment"));
    }

    @Test
    void consult_ShouldHandleNullDepartmentName() {
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"recommendDepartment\":null,\"recommendDepartmentId\":5,\"recommendDoctorIds\":\"\",\"analysis\":\"建议\",\"confidence\":80}");
        when(triageRecordRepository.save(any())).thenAnswer(inv -> {
            TriageRecord r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        Map<String, Object> result = triageService.consult("症状", 1L);

        assertEquals("内科", result.get("recommendDepartment"));
    }
}
