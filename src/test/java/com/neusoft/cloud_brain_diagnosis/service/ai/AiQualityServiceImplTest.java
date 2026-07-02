package com.neusoft.cloud_brain_diagnosis.service.ai;

import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.common.util.AiApiUtil;
import com.neusoft.cloud_brain_diagnosis.entity.MedicalRecord;
import com.neusoft.cloud_brain_diagnosis.entity.QualityCheckDetail;
import com.neusoft.cloud_brain_diagnosis.entity.QualityCheckRecord;
import com.neusoft.cloud_brain_diagnosis.repository.MedicalRecordRepository;
import com.neusoft.cloud_brain_diagnosis.repository.QualityCheckDetailRepository;
import com.neusoft.cloud_brain_diagnosis.repository.QualityCheckRecordRepository;
import com.neusoft.cloud_brain_diagnosis.service.ai.impl.AiQualityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiQualityServiceImplTest {

    @Mock private QualityCheckRecordRepository checkRecordRepository;
    @Mock private QualityCheckDetailRepository checkDetailRepository;
    @Mock private MedicalRecordRepository medicalRecordRepository;
    @Mock private AiApiUtil aiApiUtil;

    private AiQualityServiceImpl qualityService;

    @BeforeEach
    void setUp() {
        qualityService = new AiQualityServiceImpl(
                checkRecordRepository, checkDetailRepository, medicalRecordRepository, aiApiUtil);
    }

    // ========== startQualityCheck() ==========

    @Test
    void startQualityCheck_ShouldCreateRecord_WithMedicalRecords() {
        MedicalRecord mr = new MedicalRecord();
        mr.setId(1L);
        mr.setChiefComplaint("头痛");
        mr.setPresentIllness("持续性疼痛");
        mr.setPastHistory("无");
        mr.setPhysicalExamination("正常");
        mr.setDiagnosis("偏头痛");
        mr.setTreatment("药物治疗");
        mr.setDoctorId(100L);
        mr.setDoctorName("张医生");

        when(medicalRecordRepository.findAll()).thenReturn(List.of(mr));
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"passCount\":1,\"avgScore\":85.5,\"problemSummary\":\"良好\",\"details\":[]}");

        Map<String, Object> result = qualityService.startQualityCheck("medical_record", 10);

        assertNotNull(result);
        assertEquals("medical_record", result.get("checkType"));
        assertEquals(1, result.get("totalCount"));

        ArgumentCaptor<QualityCheckRecord> recordCaptor = ArgumentCaptor.forClass(QualityCheckRecord.class);
        verify(checkRecordRepository).save(recordCaptor.capture());
        assertEquals("medical_record", recordCaptor.getValue().getCheckType());
    }

    @Test
    void startQualityCheck_ShouldUseDefaultSampleSize_WhenNull() {
        when(medicalRecordRepository.findAll()).thenReturn(Collections.emptyList());
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn("{}");

        Map<String, Object> result = qualityService.startQualityCheck("medical_record", null);

        assertNotNull(result);
        assertEquals(0, result.get("totalCount"));
    }

    @Test
    void startQualityCheck_ShouldLimitSampleSize_WhenExceeds() {
        MedicalRecord mr = new MedicalRecord();
        mr.setId(1L);
        mr.setChiefComplaint("头痛");
        mr.setPresentIllness("持续性疼痛");
        mr.setPastHistory("无");
        mr.setPhysicalExamination("正常");
        mr.setDiagnosis("偏头痛");
        mr.setTreatment("药物治疗");
        mr.setDoctorId(100L);
        mr.setDoctorName("张医生");

        when(medicalRecordRepository.findAll()).thenReturn(List.of(mr));
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn("{}");

        Map<String, Object> result = qualityService.startQualityCheck("medical_record", 5);

        assertEquals(1, result.get("totalCount"));
    }

    @Test
    void startQualityCheck_ShouldHandleAiParseError() {
        MedicalRecord mr = new MedicalRecord();
        mr.setId(1L);
        mr.setChiefComplaint("头痛");
        mr.setPresentIllness("持续性疼痛");
        mr.setPastHistory("无");
        mr.setPhysicalExamination("正常");
        mr.setDiagnosis("偏头痛");
        mr.setTreatment("药物治疗");
        mr.setDoctorId(100L);
        mr.setDoctorName("张医生");

        when(medicalRecordRepository.findAll()).thenReturn(List.of(mr));
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn("invalid json");

        Map<String, Object> result = qualityService.startQualityCheck("medical_record", 10);

        assertNotNull(result);
        verify(checkRecordRepository).save(any(QualityCheckRecord.class));
    }

    // ========== getCheckList() ==========

    @Test
    void getCheckList_ShouldReturnPage() {
        Page<QualityCheckRecord> page = new PageImpl<>(List.of(new QualityCheckRecord()));
        when(checkRecordRepository.findByOrderByCreateTimeDesc(any(Pageable.class))).thenReturn(page);

        Page<QualityCheckRecord> result = qualityService.getCheckList(1, 10);

        assertEquals(1, result.getContent().size());
        verify(checkRecordRepository).findByOrderByCreateTimeDesc(any(Pageable.class));
    }

    // ========== getCheckDetail() ==========

    @Test
    void getCheckDetail_ShouldReturnRecord_WhenExists() {
        QualityCheckRecord record = new QualityCheckRecord();
        record.setId(1L);
        when(checkRecordRepository.findById(1L)).thenReturn(Optional.of(record));

        QualityCheckRecord result = qualityService.getCheckDetail(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getCheckDetail_ShouldThrow_WhenNotFound() {
        when(checkRecordRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> qualityService.getCheckDetail(99L));
    }

    // ========== getCheckDetails() ==========

    @Test
    void getCheckDetails_ShouldReturnPage() {
        Page<QualityCheckDetail> page = new PageImpl<>(List.of(new QualityCheckDetail()));
        when(checkDetailRepository.findByRecordIdOrderByCreateTimeDesc(eq(1L), any(Pageable.class)))
                .thenReturn(page);

        Page<QualityCheckDetail> result = qualityService.getCheckDetails(1L, 1, 10);

        assertEquals(1, result.getContent().size());
    }

    // ========== getDoctorStats() ==========

    @Test
    void getDoctorStats_ShouldGroupByDoctor() {
        QualityCheckDetail detail1 = new QualityCheckDetail();
        detail1.setDoctorId(100L);
        detail1.setDoctorName("张医生");
        detail1.setScore(85);
        detail1.setStatus("pending");

        QualityCheckDetail detail2 = new QualityCheckDetail();
        detail2.setDoctorId(100L);
        detail2.setDoctorName("张医生");
        detail2.setScore(90);
        detail2.setStatus("pending");

        when(checkDetailRepository.findAll()).thenReturn(List.of(detail1, detail2));

        Map<String, Object> result = qualityService.getDoctorStats();

        assertNotNull(result);
        assertEquals(1, result.get("total"));
    }

    @Test
    void getDoctorStats_ShouldReturnEmpty_WhenNoRecords() {
        when(checkDetailRepository.findAll()).thenReturn(Collections.emptyList());

        Map<String, Object> result = qualityService.getDoctorStats();

        assertNotNull(result);
        assertEquals(0, result.get("total"));
    }

    // ========== getMyQualityList() ==========

    @Test
    void getMyQualityList_ShouldReturnPage() {
        Page<QualityCheckDetail> page = new PageImpl<>(List.of(new QualityCheckDetail()));
        when(checkDetailRepository.findByDoctorIdOrderByCreateTimeDesc(eq(100L), any(Pageable.class)))
                .thenReturn(page);

        Page<QualityCheckDetail> result = qualityService.getMyQualityList(100L, 1, 10);

        assertEquals(1, result.getContent().size());
    }

    // ========== rectify() ==========

    @Test
    void rectify_ShouldUpdateStatus() {
        QualityCheckDetail detail = new QualityCheckDetail();
        detail.setId(1L);
        detail.setStatus("pending");

        when(checkDetailRepository.findById(1L)).thenReturn(Optional.of(detail));
        when(checkDetailRepository.save(any())).thenReturn(detail);

        String result = qualityService.rectify(1L, "已整改", 100L);

        assertEquals("整改已提交", result);
        verify(checkDetailRepository).save(argThat(d -> "rectified".equals(d.getStatus())));
    }

    // ========== Additional coverage tests for startQualityCheck ==========

    @Test
    void startQualityCheck_ShouldSaveDetails_WithValidAiResponse() {
        MedicalRecord mr1 = new MedicalRecord();
        mr1.setId(1L);
        mr1.setChiefComplaint("Headache");
        mr1.setPresentIllness("Persistent pain");
        mr1.setPastHistory("None");
        mr1.setPhysicalExamination("Normal");
        mr1.setDiagnosis("Migraine");
        mr1.setTreatment("Medication");
        mr1.setDoctorId(100L);
        mr1.setDoctorName("Dr. Zhang");

        MedicalRecord mr2 = new MedicalRecord();
        mr2.setId(2L);
        mr2.setChiefComplaint("Fever");
        mr2.setPresentIllness("High temp");
        mr2.setPastHistory("None");
        mr2.setPhysicalExamination("Normal");
        mr2.setDiagnosis("Flu");
        mr2.setTreatment("Rest");
        mr2.setDoctorId(101L);
        mr2.setDoctorName("Dr. Li");

        String aiResponse = "{\"passCount\":2,\"avgScore\":85.0,\"problemSummary\":\"Good quality\",\"improvementSuggestions\":\"Continue\",\"details\":[{\"index\":1,\"score\":90,\"problems\":[],\"suggestions\":\"Keep it up\"},{\"index\":2,\"score\":80,\"problems\":[{\"level\":\"minor\",\"field\":\"Past History\",\"content\":\"Incomplete\"}],\"suggestions\":\"Improve\"}]}";

        when(medicalRecordRepository.findAll()).thenReturn(List.of(mr1, mr2));
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn(aiResponse);
        when(checkDetailRepository.save(any())).thenAnswer(inv -> {
            QualityCheckDetail d = inv.getArgument(0);
            d.setId(1L);
            return d;
        });

        Map<String, Object> result = qualityService.startQualityCheck("medical_record", 10);

        assertNotNull(result);
        assertEquals(2, result.get("totalCount"));
        assertEquals(2, result.get("passCount"));
        verify(checkDetailRepository, times(2)).save(any(QualityCheckDetail.class));
    }

    @Test
    void startQualityCheck_ShouldHandleEmptySamples_WhenCheckTypeNotMedicalRecord() {
        Map<String, Object> result = qualityService.startQualityCheck("prescription", 10);

        assertNotNull(result);
        assertEquals(0, result.get("totalCount"));
        verify(medicalRecordRepository, never()).findAll();
    }

    @Test
    void startQualityCheck_ShouldUseDefaultSampleSize_WhenSampleSizeIsNegative() {
        when(medicalRecordRepository.findAll()).thenReturn(Collections.emptyList());
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn("{}");

        Map<String, Object> result = qualityService.startQualityCheck("medical_record", -5);

        assertNotNull(result);
        assertEquals(0, result.get("totalCount"));
    }

    // ========== Additional coverage tests for getDoctorStats ==========

    @Test
    void getDoctorStats_ShouldCalculateStatsCorrectly_WithMultipleDoctors() {
        QualityCheckDetail detail1 = new QualityCheckDetail();
        detail1.setDoctorId(100L);
        detail1.setDoctorName("Dr. Zhang");
        detail1.setScore(85);
        detail1.setStatus("pending");

        QualityCheckDetail detail2 = new QualityCheckDetail();
        detail2.setDoctorId(101L);
        detail2.setDoctorName("Dr. Li");
        detail2.setScore(90);
        detail2.setStatus("completed");

        QualityCheckDetail detail3 = new QualityCheckDetail();
        detail3.setDoctorId(101L);
        detail3.setDoctorName("Dr. Li");
        detail3.setScore(88);
        detail3.setStatus("pending");

        when(checkDetailRepository.findAll()).thenReturn(List.of(detail1, detail2, detail3));

        Map<String, Object> result = qualityService.getDoctorStats();

        assertNotNull(result);
        assertEquals(2, result.get("total"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> stats = (List<Map<String, Object>>) result.get("stats");
        assertEquals(2, stats.size());
    }

    @Test
    void getDoctorStats_ShouldCalculatePendingCount() {
        QualityCheckDetail detail1 = new QualityCheckDetail();
        detail1.setDoctorId(100L);
        detail1.setDoctorName("Dr. Zhang");
        detail1.setScore(85);
        detail1.setStatus("pending");

        QualityCheckDetail detail2 = new QualityCheckDetail();
        detail2.setDoctorId(100L);
        detail2.setDoctorName("Dr. Zhang");
        detail2.setScore(90);
        detail2.setStatus("completed");

        QualityCheckDetail detail3 = new QualityCheckDetail();
        detail3.setDoctorId(100L);
        detail3.setDoctorName("Dr. Zhang");
        detail3.setScore(88);
        detail3.setStatus("pending");

        when(checkDetailRepository.findAll()).thenReturn(List.of(detail1, detail2, detail3));

        Map<String, Object> result = qualityService.getDoctorStats();

        assertNotNull(result);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> stats = (List<Map<String, Object>>) result.get("stats");
        Map<String, Object> zhangStats = stats.get(0);
        assertEquals(3, ((Number) zhangStats.get("totalCheck")).intValue());
        assertEquals(2, ((Number) zhangStats.get("pendingCount")).intValue());
    }

    @Test
    void getDoctorStats_ShouldHandleNullDoctorName() {
        QualityCheckDetail detail = new QualityCheckDetail();
        detail.setDoctorId(100L);
        detail.setDoctorName(null);
        detail.setScore(85);
        detail.setStatus("pending");

        when(checkDetailRepository.findAll()).thenReturn(List.of(detail));

        Map<String, Object> result = qualityService.getDoctorStats();

        assertNotNull(result);
        assertEquals(1, result.get("total"));
    }

    @Test
    void rectify_ShouldThrow_WhenNotFound() {
        when(checkDetailRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> qualityService.rectify(99L, "Remark", 100L));
    }
}
