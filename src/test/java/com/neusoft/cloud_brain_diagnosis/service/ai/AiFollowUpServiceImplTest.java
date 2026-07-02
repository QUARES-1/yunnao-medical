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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
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
        request.put("disease", "Cold");
        request.put("planType", "Regular review");
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

    @Test
    void createPlan_ShouldHandleNullPatientId() {
        when(planRepository.save(any())).thenAnswer(inv -> {
            FollowUpPlan p = inv.getArgument(0);
            p.setId(100L);
            return p;
        });
        when(recordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> request = new HashMap<>();
        request.put("disease", "Cold");

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

    // ========== getPatientPlans / getPendingRecords ==========

    @Test
    void getPatientPlans_ShouldReturnPage() {
        FollowUpPlan plan = new FollowUpPlan();
        plan.setId(1L);

        Page<FollowUpPlan> page = new PageImpl<>(List.of(plan));
        when(planRepository.findByPatientIdOrderByCreateTimeDesc(eq(1L), any(Pageable.class))).thenReturn(page);

        Page<FollowUpPlan> result = followUpService.getPatientPlans(1L, 1, 10);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getPendingRecords_ShouldReturnPage() {
        FollowUpRecord record = new FollowUpRecord();
        record.setId(1L);

        Page<FollowUpRecord> page = new PageImpl<>(List.of(record));
        when(recordRepository.findByPatientIdAndStatus(eq(1L), eq("pending"), any(Pageable.class))).thenReturn(page);

        Page<FollowUpRecord> result = followUpService.getPendingRecords(1L, 1, 10);
        assertEquals(1, result.getContent().size());
    }

    // ========== submitRecord() ==========

    @Test
    void submitRecord_ShouldSucceed() {
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

        String result = followUpService.submitRecord(1L, "{}", 1L);

        assertEquals("提交成功", result);
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
                .thenReturn("{\"analysis\":\"Normal\",\"abnormal\":false}");
        when(recordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(planRepository.findById(100L)).thenReturn(Optional.of(plan));
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = followUpService.submitRecord(1L, null, 1L);

        assertEquals("提交成功", result);
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
    void submitRecord_ShouldThrow_WhenRecordNotFound() {
        when(recordRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> followUpService.submitRecord(99L, "{}", 1L));
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
        plan.setCompletedTimes(1);

        when(recordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn("invalid json");
        when(recordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(planRepository.findById(100L)).thenReturn(Optional.of(plan));
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = followUpService.submitRecord(1L, "{}", 1L);

        assertNotNull(result);
    }

    @Test
    void submitRecord_ShouldHandleAbnormal_FromAiResponse() {
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
                .thenReturn("{\"analysis\":\"Abnormal\",\"abnormal\":true}");
        when(recordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(planRepository.findById(100L)).thenReturn(Optional.of(plan));
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        followUpService.submitRecord(1L, "{\"temp\":\"38\"}", 1L);

        verify(recordRepository, atLeastOnce()).save(argThat(r -> r.getAbnormalFlag() != null && r.getAbnormalFlag() == 1));
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

        followUpService.submitRecord(1L, "{\"temp\":\"40C\"}", 1L);

        verify(recordRepository, atLeastOnce()).save(argThat(r -> r.getAbnormalFlag() != null && r.getAbnormalFlag() == 1));
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
}
