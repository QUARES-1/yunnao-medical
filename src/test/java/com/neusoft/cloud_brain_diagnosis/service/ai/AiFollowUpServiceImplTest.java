package com.neusoft.cloud_brain_diagnosis.service.ai;

import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.common.util.AiApiUtil;
import com.neusoft.cloud_brain_diagnosis.entity.FollowUpPlan;
import com.neusoft.cloud_brain_diagnosis.entity.FollowUpRecord;
import com.neusoft.cloud_brain_diagnosis.repository.FollowUpPlanRepository;
import com.neusoft.cloud_brain_diagnosis.repository.FollowUpRecordRepository;
import com.neusoft.cloud_brain_diagnosis.service.ai.impl.AiFollowUpServiceImpl;
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
class AiFollowUpServiceImplTest {

    @Mock private FollowUpPlanRepository planRepository;
    @Mock private FollowUpRecordRepository recordRepository;
    @Mock private AiApiUtil aiApiUtil;

    private AiFollowUpServiceImpl followUpService;

    @BeforeEach
    void setUp() {
        followUpService = new AiFollowUpServiceImpl(planRepository, recordRepository, aiApiUtil);
    }

    // ========== createPlan() ==========

    @Test
    void createPlan_ShouldCreatePlanWithRecords() {
        when(planRepository.save(any())).thenAnswer(inv -> {
            FollowUpPlan p = inv.getArgument(0);
            p.setId(100L);
            return p;
        });
        when(recordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> request = new HashMap<>();
        request.put("patientId", 1L);
        request.put("registrationId", 10L);
        request.put("disease", "感冒");
        request.put("planType", "定期复查");
        request.put("totalTimes", 3);

        FollowUpPlan result = followUpService.createPlan(request, 10L);

        assertEquals(100L, result.getId());
        assertEquals("ongoing", result.getStatus());
        assertEquals(3, result.getTotalTimes());
        verify(recordRepository, times(3)).save(any(FollowUpRecord.class));
    }

    @Test
    void createPlan_ShouldUseDefaultTimes_WhenNotProvided() {
        when(planRepository.save(any())).thenAnswer(inv -> {
            FollowUpPlan p = inv.getArgument(0);
            p.setId(100L);
            return p;
        });
        when(recordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> request = new HashMap<>();
        request.put("patientId", 1L);

        FollowUpPlan result = followUpService.createPlan(request, 10L);

        assertEquals(3, result.getTotalTimes());
        verify(recordRepository, times(3)).save(any(FollowUpRecord.class));
    }

    // ========== submitRecord() ==========

    @Test
    void submitRecord_ShouldCompleteRecord_WithNormalAnalysis() {
        FollowUpRecord record = new FollowUpRecord();
        record.setId(1L);
        record.setPatientId(1L);
        record.setPlanId(100L);
        record.setQuestionnaireJson("[{\"key\":\"recovery\"}]");

        FollowUpPlan plan = new FollowUpPlan();
        plan.setId(100L);
        plan.setTotalTimes(3);
        plan.setCompletedTimes(0);

        when(recordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"analysis\":\"恢复良好\",\"abnormal\":false}");
        when(recordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(planRepository.findById(100L)).thenReturn(Optional.of(plan));
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = followUpService.submitRecord(1L, "{\"recovery\":\"明显好转\"}", 1L);

        assertEquals("提交成功", result);
        verify(recordRepository).save(argThat(r -> "completed".equals(r.getStatus())));
    }

    @Test
    void submitRecord_ShouldDetectAbnormal_WhenDangerSignalFound() {
        FollowUpRecord record = new FollowUpRecord();
        record.setId(1L);
        record.setPatientId(1L);
        record.setPlanId(100L);
        record.setQuestionnaireJson("[]");

        FollowUpPlan plan = new FollowUpPlan();
        plan.setId(100L);
        plan.setTotalTimes(3);
        plan.setCompletedTimes(0);

        when(recordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"analysis\":\"无异常\",\"abnormal\":false}");
        when(recordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(planRepository.findById(100L)).thenReturn(Optional.of(plan));
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        followUpService.submitRecord(1L, "{\"recovery\":\"症状加重\"}", 1L);

        verify(recordRepository).save(argThat(r -> "abnormal".equals(r.getStatus()) && r.getAbnormalFlag() == 1));
    }

    @Test
    void submitRecord_ShouldThrow_WhenRecordNotFound() {
        when(recordRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> followUpService.submitRecord(99L, "{}", 1L));
    }

    @Test
    void submitRecord_ShouldThrow_WhenPatientMismatch() {
        FollowUpRecord record = new FollowUpRecord();
        record.setId(1L);
        record.setPatientId(2L);

        when(recordRepository.findById(1L)).thenReturn(Optional.of(record));

        assertThrows(BusinessException.class,
                () -> followUpService.submitRecord(1L, "{}", 1L));
    }

    @Test
    void submitRecord_ShouldCompletePlan_WhenAllRecordsSubmitted() {
        FollowUpRecord record = new FollowUpRecord();
        record.setId(1L);
        record.setPatientId(1L);
        record.setPlanId(100L);
        record.setQuestionnaireJson("[]");

        FollowUpPlan plan = new FollowUpPlan();
        plan.setId(100L);
        plan.setTotalTimes(3);
        plan.setCompletedTimes(2);

        when(recordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"analysis\":\"正常\",\"abnormal\":false}");
        when(recordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(planRepository.findById(100L)).thenReturn(Optional.of(plan));
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        followUpService.submitRecord(1L, "{}", 1L);

        verify(planRepository).save(argThat(p -> "completed".equals(p.getStatus())));
    }

    // ========== Additional coverage tests ==========

    @Test
    void createPlan_ShouldHandleNullPlanType() {
        when(planRepository.save(any())).thenAnswer(inv -> {
            FollowUpPlan p = inv.getArgument(0);
            p.setId(100L);
            return p;
        });
        when(recordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> request = new HashMap<>();
        request.put("patientId", 1L);
        request.put("planType", null);

        FollowUpPlan result = followUpService.createPlan(request, 10L);

        assertEquals(100L, result.getId());
        assertEquals(3, result.getTotalTimes());
    }

    @Test
    void submitRecord_ShouldHandleEmptyQuestionnaire() {
        FollowUpRecord record = new FollowUpRecord();
        record.setId(1L);
        record.setPatientId(1L);
        record.setPlanId(100L);
        record.setQuestionnaireJson(null);

        FollowUpPlan plan = new FollowUpPlan();
        plan.setId(100L);
        plan.setTotalTimes(3);
        plan.setCompletedTimes(0);

        when(recordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"analysis\":\"正常\",\"abnormal\":false}");
        when(recordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(planRepository.findById(100L)).thenReturn(Optional.of(plan));
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = followUpService.submitRecord(1L, null, 1L);

        assertEquals("提交成功", result);
    }

    @Test
    void submitRecord_ShouldHandleAiParseError() {
        FollowUpRecord record = new FollowUpRecord();
        record.setId(1L);
        record.setPatientId(1L);
        record.setPlanId(100L);
        record.setQuestionnaireJson("[]");

        FollowUpPlan plan = new FollowUpPlan();
        plan.setId(100L);
        plan.setTotalTimes(3);
        plan.setCompletedTimes(0);

        when(recordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("non-JSON response");
        when(recordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(planRepository.findById(100L)).thenReturn(Optional.of(plan));
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = followUpService.submitRecord(1L, "{}", 1L);

        assertNotNull(result);
    }

    // ========== getPatientPlans() ==========

    @Test
    void getPatientPlans_ShouldReturnPage() {
        FollowUpPlan plan = new FollowUpPlan();
        plan.setId(1L);

        when(planRepository.findByPatientIdOrderByCreateTimeDesc(eq(1L), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(plan)));

        var result = followUpService.getPatientPlans(1L, 1, 10);

        assertEquals(1, result.getContent().size());
    }

    // ========== getPendingRecords() ==========

    @Test
    void getPendingRecords_ShouldReturnPage() {
        FollowUpRecord record = new FollowUpRecord();
        record.setId(1L);

        when(recordRepository.findByPatientIdAndStatus(eq(1L), eq("pending"), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(record)));

        var result = followUpService.getPendingRecords(1L, 1, 10);

        assertEquals(1, result.getContent().size());
    }

    // ========== getDetail() ==========

    @Test
    void getDetail_ShouldReturnDetail() {
        FollowUpRecord record = new FollowUpRecord();
        record.setId(1L);
        record.setPlanId(100L);
        record.setPatientId(1L);
        record.setFollowUpTime(java.time.LocalDateTime.now());
        record.setStatus("pending");
        record.setAbnormalFlag(0);
        record.setQuestionnaireJson("[{\"key\":\"test\"}]");
        record.setAnswerJson("{\"key\":\"value\"}");

        FollowUpPlan plan = new FollowUpPlan();
        plan.setId(100L);
        plan.setDisease("Flu");
        plan.setPlanType("Weekly");

        when(recordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(planRepository.findById(100L)).thenReturn(Optional.of(plan));

        Map<String, Object> result = followUpService.getDetail(1L);

        assertEquals(1L, result.get("id"));
        assertEquals("Flu", result.get("disease"));
        assertNotNull(result.get("questionnaire"));
        assertNotNull(result.get("answers"));
    }

    @Test
    void getDetail_ShouldHandleNullPlan() {
        FollowUpRecord record = new FollowUpRecord();
        record.setId(1L);
        record.setPlanId(100L);
        record.setPatientId(1L);
        record.setFollowUpTime(java.time.LocalDateTime.now());
        record.setStatus("pending");
        record.setAbnormalFlag(0);

        when(recordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(planRepository.findById(100L)).thenReturn(Optional.empty());

        Map<String, Object> result = followUpService.getDetail(1L);

        assertEquals(1L, result.get("id"));
        assertNull(result.get("disease"));
    }

    @Test
    void getDetail_ShouldThrow_WhenRecordNotFound() {
        when(recordRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> followUpService.getDetail(99L));
    }

    @Test
    void getDetail_ShouldHandleInvalidJson() {
        FollowUpRecord record = new FollowUpRecord();
        record.setId(1L);
        record.setPlanId(100L);
        record.setPatientId(1L);
        record.setFollowUpTime(java.time.LocalDateTime.now());
        record.setStatus("pending");
        record.setAbnormalFlag(0);
        record.setQuestionnaireJson("invalid json");
        record.setAnswerJson("also invalid");

        when(recordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(planRepository.findById(100L)).thenReturn(Optional.empty());

        Map<String, Object> result = followUpService.getDetail(1L);

        assertEquals(1L, result.get("id"));
        assertNull(result.get("questionnaire"));
        assertNull(result.get("answers"));
    }

    // ========== getDoctorList() ==========

    @Test
    void getDoctorList_ShouldReturnPage() {
        FollowUpPlan plan = new FollowUpPlan();
        plan.setId(1L);

        when(planRepository.findByDoctorIdOrderByCreateTimeDesc(eq(10L), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(plan)));

        var result = followUpService.getDoctorList(10L, 1, 10);

        assertEquals(1, result.getContent().size());
    }

    // ========== doctorReply() ==========

    @Test
    void doctorReply_ShouldUpdateRemark() {
        FollowUpRecord record = new FollowUpRecord();
        record.setId(1L);

        when(recordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(recordRepository.save(any())).thenReturn(record);

        String result = followUpService.doctorReply(1L, "Patient should rest more", 10L);

        assertNotNull(result);
        verify(recordRepository).save(argThat(r -> "Patient should rest more".equals(r.getDoctorRemark())));
    }

    @Test
    void doctorReply_ShouldThrow_WhenRecordNotFound() {
        when(recordRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> followUpService.doctorReply(99L, "Remark", 10L));
    }

    // ========== containsDangerSignal() branches ==========

    @Test
    void submitRecord_ShouldDetectHighFever() {
        FollowUpRecord record = new FollowUpRecord();
        record.setId(1L);
        record.setPatientId(1L);
        record.setPlanId(100L);
        record.setQuestionnaireJson("[]");

        FollowUpPlan plan = new FollowUpPlan();
        plan.setId(100L);
        plan.setTotalTimes(3);
        plan.setCompletedTimes(0);

        when(recordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"analysis\":\"Normal\",\"abnormal\":false}");
        when(recordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(planRepository.findById(100L)).thenReturn(Optional.of(plan));
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        followUpService.submitRecord(1L, "{\"temp\":\"持续高热39.5度\"}", 1L);

        verify(recordRepository, atLeastOnce()).save(argThat(r -> r.getAbnormalFlag() == 1));
    }

    @Test
    void submitRecord_ShouldDetectChestPain() {
        FollowUpRecord record = new FollowUpRecord();
        record.setId(1L);
        record.setPatientId(1L);
        record.setPlanId(100L);
        record.setQuestionnaireJson("[]");

        FollowUpPlan plan = new FollowUpPlan();
        plan.setId(100L);
        plan.setTotalTimes(3);
        plan.setCompletedTimes(0);

        when(recordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"analysis\":\"Normal\",\"abnormal\":false}");
        when(recordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(planRepository.findById(100L)).thenReturn(Optional.of(plan));
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        followUpService.submitRecord(1L, "{\"symptoms\":\"胸痛胸闷\"}", 1L);

        verify(recordRepository, atLeastOnce()).save(argThat(r -> r.getAbnormalFlag() == 1));
    }

    @Test
    void submitRecord_ShouldDetectWoundIssue() {
        FollowUpRecord record = new FollowUpRecord();
        record.setId(1L);
        record.setPatientId(1L);
        record.setPlanId(100L);
        record.setQuestionnaireJson("[]");

        FollowUpPlan plan = new FollowUpPlan();
        plan.setId(100L);
        plan.setTotalTimes(3);
        plan.setCompletedTimes(0);

        when(recordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"analysis\":\"Normal\",\"abnormal\":false}");
        when(recordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(planRepository.findById(100L)).thenReturn(Optional.of(plan));
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        followUpService.submitRecord(1L, "{\"wound\":\"伤口红肿流脓\"}", 1L);

        verify(recordRepository, atLeastOnce()).save(argThat(r -> r.getAbnormalFlag() == 1));
    }

    @Test
    void submitRecord_ShouldDetectStoppedMedicine() {
        FollowUpRecord record = new FollowUpRecord();
        record.setId(1L);
        record.setPatientId(1L);
        record.setPlanId(100L);
        record.setQuestionnaireJson("[]");

        FollowUpPlan plan = new FollowUpPlan();
        plan.setId(100L);
        plan.setTotalTimes(3);
        plan.setCompletedTimes(0);

        when(recordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"analysis\":\"Normal\",\"abnormal\":false}");
        when(recordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(planRepository.findById(100L)).thenReturn(Optional.of(plan));
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        followUpService.submitRecord(1L, "{\"medicine\":\"已自行停药\"}", 1L);

        verify(recordRepository, atLeastOnce()).save(argThat(r -> r.getAbnormalFlag() == 1));
    }

    @Test
    void submitRecord_ShouldDetectHighTemperature() {
        FollowUpRecord record = new FollowUpRecord();
        record.setId(1L);
        record.setPatientId(1L);
        record.setPlanId(100L);
        record.setQuestionnaireJson("[]");

        FollowUpPlan plan = new FollowUpPlan();
        plan.setId(100L);
        plan.setTotalTimes(3);
        plan.setCompletedTimes(0);

        when(recordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"analysis\":\"Normal\",\"abnormal\":false}");
        when(recordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(planRepository.findById(100L)).thenReturn(Optional.of(plan));
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Use keyword "40℃" which is in the danger signal list
        followUpService.submitRecord(1L, "{\"temp\":\"40℃\"}", 1L);

        verify(recordRepository, atLeastOnce()).save(argThat(r -> r.getAbnormalFlag() == 1));
    }

    @Test
    void submitRecord_ShouldHandleNullAnswerJson() {
        FollowUpRecord record = new FollowUpRecord();
        record.setId(1L);
        record.setPatientId(1L);
        record.setPlanId(100L);
        record.setQuestionnaireJson("[]");

        FollowUpPlan plan = new FollowUpPlan();
        plan.setId(100L);
        plan.setTotalTimes(3);
        plan.setCompletedTimes(0);

        when(recordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"analysis\":\"Normal\",\"abnormal\":false}");
        when(recordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(planRepository.findById(100L)).thenReturn(Optional.of(plan));
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = followUpService.submitRecord(1L, null, 1L);

        assertNotNull(result);
    }

    @Test
    void submitRecord_ShouldHandleNullPlan() {
        FollowUpRecord record = new FollowUpRecord();
        record.setId(1L);
        record.setPatientId(1L);
        record.setPlanId(100L);
        record.setQuestionnaireJson("[]");

        when(recordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"analysis\":\"Normal\",\"abnormal\":false}");
        when(recordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(planRepository.findById(100L)).thenReturn(Optional.empty());

        String result = followUpService.submitRecord(1L, "{}", 1L);

        assertNotNull(result);
    }

    // ========== Branch Coverage ==========

    @Test
    void createPlan_ShouldHandleNullPatientId() {
        when(planRepository.save(any())).thenAnswer(inv -> {
            FollowUpPlan p = inv.getArgument(0);
            p.setId(100L);
            return p;
        });
        when(recordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> request = new HashMap<>();
        request.put("disease", "感冒");

        FollowUpPlan result = followUpService.createPlan(request, 10L);

        assertEquals(100L, result.getId());
        assertNull(result.getPatientId());
        assertEquals(10L, result.getDoctorId());
    }

    @Test
    void createPlan_ShouldHandleNullRegistrationId() {
        when(planRepository.save(any())).thenAnswer(inv -> {
            FollowUpPlan p = inv.getArgument(0);
            p.setId(100L);
            return p;
        });
        when(recordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> request = new HashMap<>();
        request.put("patientId", 1L);

        FollowUpPlan result = followUpService.createPlan(request, 10L);

        assertEquals(100L, result.getId());
        assertNull(result.getRegistrationId());
    }

    @Test
    void createPlan_ShouldHandleNullPlanType() {
        when(planRepository.save(any())).thenAnswer(inv -> {
            FollowUpPlan p = inv.getArgument(0);
            p.setId(100L);
            return p;
        });
        when(recordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> request = new HashMap<>();
        request.put("patientId", 1L);
        request.put("planType", null);

        FollowUpPlan result = followUpService.createPlan(request, 10L);

        assertEquals(100L, result.getId());
        assertNull(result.getPlanType());
    }

    @Test
    void submitRecord_ShouldThrow_WhenPatientIdMismatch() {
        FollowUpRecord record = new FollowUpRecord();
        record.setId(1L);
        record.setPatientId(1L);

        when(recordRepository.findById(1L)).thenReturn(Optional.of(record));

        assertThrows(BusinessException.class,
                () -> followUpService.submitRecord(1L, "{}", 99L));
    }

    @Test
    void submitRecord_ShouldHandleAbnormal_FromDangerSignal() {
        FollowUpRecord record = new FollowUpRecord();
        record.setId(1L);
        record.setPatientId(1L);
        record.setPlanId(100L);
        record.setQuestionnaireJson("[]");

        FollowUpPlan plan = new FollowUpPlan();
        plan.setId(100L);
        plan.setTotalTimes(3);
        plan.setCompletedTimes(1);

        when(recordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"analysis\":\"Normal\",\"abnormal\":false}");
        when(recordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(planRepository.findById(100L)).thenReturn(Optional.of(plan));
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // "40℃" is a danger signal -> abnormal = true
        followUpService.submitRecord(1L, "{\"temp\":\"40℃\"}", 1L);

        verify(recordRepository, atLeastOnce()).save(argThat(r -> r.getAbnormalFlag() != null && r.getAbnormalFlag() == 1));
    }
}
