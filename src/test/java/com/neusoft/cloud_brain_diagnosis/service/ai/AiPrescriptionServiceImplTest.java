package com.neusoft.cloud_brain_diagnosis.service.ai;

import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.common.util.AiApiUtil;
import com.neusoft.cloud_brain_diagnosis.entity.Doctor;
import com.neusoft.cloud_brain_diagnosis.entity.PrescriptionAiReview;
import com.neusoft.cloud_brain_diagnosis.repository.DoctorRepository;
import com.neusoft.cloud_brain_diagnosis.repository.PrescriptionAiReviewRepository;
import com.neusoft.cloud_brain_diagnosis.repository.PrescriptionRepository;
import com.neusoft.cloud_brain_diagnosis.service.NotificationService;
import com.neusoft.cloud_brain_diagnosis.service.ai.PromptTemplateService;
import com.neusoft.cloud_brain_diagnosis.service.ai.impl.AiPrescriptionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class AiPrescriptionServiceImplTest {

    @Mock private PrescriptionAiReviewRepository reviewRepository;
    @Mock private PrescriptionRepository prescriptionRepository;
    @Mock private AiApiUtil aiApiUtil;
    @Mock private PromptTemplateService promptTemplateService;
    @Mock private DoctorRepository doctorRepository;
    @Mock private NotificationService notificationService;

    private AiPrescriptionServiceImpl prescriptionService;

    @BeforeEach
    void setUp() {
        prescriptionService = new AiPrescriptionServiceImpl(
                reviewRepository, prescriptionRepository, aiApiUtil,
                promptTemplateService, doctorRepository, notificationService);
    }

    // ========== checkPrescription() ==========

    @Test
    void checkPrescription_ShouldReturnReviewResult_WithValidResponse() {
        Doctor doctor = new Doctor();
        doctor.setId(10L);
        doctor.setDepartmentName("心内科");

        when(doctorRepository.findById(10L)).thenReturn(Optional.of(doctor));
        when(promptTemplateService.getPrescriptionReviewTemplate(anyString(), anyString(), anyString()))
                .thenReturn(Map.of("system", "sys", "user", "user"));
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"reviewResult\":\"pass\",\"reviewScore\":95,\"warnings\":[],\"suggestions\":\"处方合理\",\"drugInteractions\":[],\"allergyRisks\":[],\"dosageIssues\":[]}");
        when(reviewRepository.save(any())).thenAnswer(inv -> {
            PrescriptionAiReview r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        Map<String, Object> request = new HashMap<>();
        request.put("patientId", 1L);
        request.put("patientAge", 45);
        request.put("patientGender", "男");
        request.put("drugs", List.of(Map.of("name", "阿司匹林", "specification", "100mg", "dosage", "1片/次")));

        Map<String, Object> result = prescriptionService.checkPrescription(request, 10L);

        assertEquals("pass", result.get("reviewResult"));
        assertEquals(95, result.get("reviewScore"));
        assertEquals("处方合理", result.get("suggestions"));
    }

    @Test
    void checkPrescription_ShouldNotifyHighRisk_WhenRejectResult() {
        Doctor doctor = new Doctor();
        doctor.setId(10L);

        when(doctorRepository.findById(10L)).thenReturn(Optional.of(doctor));
        when(promptTemplateService.getPrescriptionReviewTemplate(any(), any(), any()))
                .thenReturn(Map.of("system", "sys", "user", "user"));
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"reviewResult\":\"reject\",\"reviewScore\":30,\"warnings\":[{\"level\":\"high\",\"content\":\"剂量过大\"}],\"suggestions\":\"降低剂量\",\"drugInteractions\":[],\"allergyRisks\":[],\"dosageIssues\":[]}");
        when(reviewRepository.save(any())).thenAnswer(inv -> {
            PrescriptionAiReview r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        Map<String, Object> request = new HashMap<>();
        request.put("patientId", 1L);
        request.put("patientAge", 60);
        request.put("patientGender", "女");
        request.put("drugs", List.of());

        prescriptionService.checkPrescription(request, 10L);

        verify(notificationService).notifyHighRiskMedication(eq(10L), eq(100L), anyList(), anyString());
    }

    @Test
    void checkPrescription_ShouldNotifyMediumRisk_WhenWarningAndLowScore() {
        Doctor doctor = new Doctor();
        doctor.setId(10L);

        when(doctorRepository.findById(10L)).thenReturn(Optional.of(doctor));
        when(promptTemplateService.getPrescriptionReviewTemplate(any(), any(), any()))
                .thenReturn(Map.of("system", "sys", "user", "user"));
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"reviewResult\":\"warning\",\"reviewScore\":70,\"warnings\":[],\"suggestions\":\"建议复核\",\"drugInteractions\":[],\"allergyRisks\":[],\"dosageIssues\":[]}");
        when(reviewRepository.save(any())).thenAnswer(inv -> {
            PrescriptionAiReview r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        Map<String, Object> request = new HashMap<>();
        request.put("patientId", 1L);
        request.put("patientAge", 30);
        request.put("patientGender", "男");
        request.put("drugs", List.of());

        prescriptionService.checkPrescription(request, 10L);

        verify(notificationService).notifyMediumRiskMedication(eq(10L), eq(100L), anyList(), anyString());
    }

    @Test
    void checkPrescription_ShouldHandleNullDoctorId() {
        when(promptTemplateService.getPrescriptionReviewTemplate(any(), any(), any()))
                .thenReturn(Map.of("system", "sys", "user", "user"));
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"reviewResult\":\"pass\",\"reviewScore\":100,\"warnings\":[],\"suggestions\":\"\",\"drugInteractions\":[],\"allergyRisks\":[],\"dosageIssues\":[]}");
        when(reviewRepository.save(any())).thenAnswer(inv -> {
            PrescriptionAiReview r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        Map<String, Object> request = new HashMap<>();
        request.put("patientId", 1L);
        request.put("patientAge", 25);
        request.put("patientGender", "男");
        request.put("drugs", List.of());

        Map<String, Object> result = prescriptionService.checkPrescription(request, null);

        assertEquals("pass", result.get("reviewResult"));
    }

    // ========== Null & Edge Case Branches ==========

    @Test
    void checkPrescription_ShouldHandleNullPatientId() {
        when(promptTemplateService.getPrescriptionReviewTemplate(any(), any(), any()))
                .thenReturn(Map.of("system", "sys", "user", "user"));
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"reviewResult\":\"pass\",\"reviewScore\":100,\"warnings\":[],\"suggestions\":\"\",\"drugInteractions\":[],\"allergyRisks\":[],\"dosageIssues\":[]}");
        when(reviewRepository.save(any())).thenAnswer(inv -> {
            PrescriptionAiReview r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        Map<String, Object> request = new HashMap<>();
        request.put("patientAge", 30);
        request.put("patientGender", "男");
        request.put("drugs", List.of());

        Map<String, Object> result = prescriptionService.checkPrescription(request, 10L);

        assertNotNull(result.get("reviewResult"));
    }

    @Test
    void checkPrescription_ShouldHandleNullPatientAge() {
        when(promptTemplateService.getPrescriptionReviewTemplate(any(), any(), any()))
                .thenReturn(Map.of("system", "sys", "user", "user"));
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"reviewResult\":\"pass\",\"reviewScore\":100,\"warnings\":[],\"suggestions\":\"\",\"drugInteractions\":[],\"allergyRisks\":[],\"dosageIssues\":[]}");
        when(reviewRepository.save(any())).thenAnswer(inv -> {
            PrescriptionAiReview r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        Map<String, Object> request = new HashMap<>();
        request.put("patientId", 1L);
        request.put("patientGender", "男");
        request.put("drugs", List.of());

        Map<String, Object> result = prescriptionService.checkPrescription(request, 10L);

        assertNotNull(result.get("reviewResult"));
    }

    @Test
    void checkPrescription_ShouldHandleNullDrugs() {
        Doctor doctor = new Doctor();
        doctor.setId(10L);

        when(doctorRepository.findById(10L)).thenReturn(Optional.of(doctor));
        when(promptTemplateService.getPrescriptionReviewTemplate(any(), any(), any()))
                .thenReturn(Map.of("system", "sys", "user", "user"));
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"reviewResult\":\"pass\",\"reviewScore\":100,\"warnings\":[],\"suggestions\":\"\",\"drugInteractions\":[],\"allergyRisks\":[],\"dosageIssues\":[]}");
        when(reviewRepository.save(any())).thenAnswer(inv -> {
            PrescriptionAiReview r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        Map<String, Object> request = new HashMap<>();
        request.put("patientId", 1L);
        request.put("patientAge", 30);
        request.put("patientGender", "男");
        request.put("drugs", null);

        Map<String, Object> result = prescriptionService.checkPrescription(request, 10L);

        assertNotNull(result.get("reviewResult"));
    }

    @Test
    void checkPrescription_ShouldReturnEmptyLists_WhenWarningsEmpty() {
        Doctor doctor = new Doctor();
        doctor.setId(10L);

        when(doctorRepository.findById(10L)).thenReturn(Optional.of(doctor));
        when(promptTemplateService.getPrescriptionReviewTemplate(any(), any(), any()))
                .thenReturn(Map.of("system", "sys", "user", "user"));
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"reviewResult\":\"pass\",\"reviewScore\":100,\"warnings\":[],\"drugInteractions\":[],\"allergyRisks\":[],\"dosageIssues\":[],\"suggestions\":\"\"}");
        when(reviewRepository.save(any())).thenAnswer(inv -> {
            PrescriptionAiReview r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        Map<String, Object> request = new HashMap<>();
        request.put("patientId", 1L);
        request.put("patientAge", 30);
        request.put("patientGender", "男");
        request.put("drugs", List.of());

        Map<String, Object> result = prescriptionService.checkPrescription(request, 10L);

        assertNotNull(result.get("warnings"));
        assertNotNull(result.get("drugInteractions"));
        assertNotNull(result.get("allergyRisks"));
        assertNotNull(result.get("dosageIssues"));
    }

    @Test
    void checkPrescription_ShouldNotifyHighRisk_WhenScoreBelow60() {
        Doctor doctor = new Doctor();
        doctor.setId(10L);

        when(doctorRepository.findById(10L)).thenReturn(Optional.of(doctor));
        when(promptTemplateService.getPrescriptionReviewTemplate(any(), any(), any()))
                .thenReturn(Map.of("system", "sys", "user", "user"));
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"reviewResult\":\"pass\",\"reviewScore\":50,\"warnings\":[{\"level\":\"high\",\"content\":\"风险\"}],\"suggestions\":\"\",\"drugInteractions\":[],\"allergyRisks\":[],\"dosageIssues\":[]}");
        when(reviewRepository.save(any())).thenAnswer(inv -> {
            PrescriptionAiReview r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        Map<String, Object> request = new HashMap<>();
        request.put("patientId", 1L);
        request.put("patientAge", 30);
        request.put("patientGender", "男");
        request.put("drugs", List.of());

        prescriptionService.checkPrescription(request, 10L);

        verify(notificationService).notifyHighRiskMedication(eq(10L), eq(100L), anyList(), anyString());
    }

    @Test
    void checkPrescription_ShouldNotNotify_WhenPassAndHighScore() {
        Doctor doctor = new Doctor();
        doctor.setId(10L);

        when(doctorRepository.findById(10L)).thenReturn(Optional.of(doctor));
        when(promptTemplateService.getPrescriptionReviewTemplate(any(), any(), any()))
                .thenReturn(Map.of("system", "sys", "user", "user"));
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"reviewResult\":\"pass\",\"reviewScore\":90,\"warnings\":[],\"suggestions\":\"\",\"drugInteractions\":[],\"allergyRisks\":[],\"dosageIssues\":[]}");
        when(reviewRepository.save(any())).thenAnswer(inv -> {
            PrescriptionAiReview r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        Map<String, Object> request = new HashMap<>();
        request.put("patientId", 1L);
        request.put("patientAge", 30);
        request.put("patientGender", "男");
        request.put("drugs", List.of());

        prescriptionService.checkPrescription(request, 10L);

        verify(notificationService, never()).notifyHighRiskMedication(any(), any(), any(), any());
        verify(notificationService, never()).notifyMediumRiskMedication(any(), any(), any(), any());
    }

    @Test
    void checkPrescription_ShouldUseDefaultScore_WhenAiReturnsInvalidJson() {
        Doctor doctor = new Doctor();
        doctor.setId(10L);

        when(doctorRepository.findById(10L)).thenReturn(Optional.of(doctor));
        when(promptTemplateService.getPrescriptionReviewTemplate(any(), any(), any()))
                .thenReturn(Map.of("system", "sys", "user", "user"));
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn("invalid json");
        when(reviewRepository.save(any())).thenAnswer(inv -> {
            PrescriptionAiReview r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        Map<String, Object> request = new HashMap<>();
        request.put("patientId", 1L);
        request.put("patientAge", 30);
        request.put("patientGender", "男");
        request.put("drugs", List.of());

        Map<String, Object> result = prescriptionService.checkPrescription(request, 10L);

        assertEquals(80, result.get("reviewScore"));
        assertEquals("warning", result.get("reviewResult"));
    }

    @Test
    void checkPrescription_ShouldHandleNullWarningsArray() {
        Doctor doctor = new Doctor();
        doctor.setId(10L);

        when(doctorRepository.findById(10L)).thenReturn(Optional.of(doctor));
        when(promptTemplateService.getPrescriptionReviewTemplate(any(), any(), any()))
                .thenReturn(Map.of("system", "sys", "user", "user"));
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"reviewResult\":\"pass\",\"reviewScore\":90,\"suggestions\":\"\",\"drugInteractions\":null,\"allergyRisks\":null,\"dosageIssues\":null}");
        when(reviewRepository.save(any())).thenAnswer(inv -> {
            PrescriptionAiReview r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        Map<String, Object> request = new HashMap<>();
        request.put("patientId", 1L);
        request.put("patientAge", 30);
        request.put("patientGender", "男");
        request.put("drugs", List.of());

        Map<String, Object> result = prescriptionService.checkPrescription(request, 10L);

        assertNotNull(result.get("warnings"));
    }

    @Test
    void checkPrescription_ShouldHandleDoctorRepositoryException() {
        when(doctorRepository.findById(10L)).thenThrow(new RuntimeException("DB error"));
        when(promptTemplateService.getPrescriptionReviewTemplate(any(), any(), any()))
                .thenReturn(Map.of("system", "sys", "user", "user"));
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"reviewResult\":\"pass\",\"reviewScore\":100,\"warnings\":[],\"suggestions\":\"\",\"drugInteractions\":[],\"allergyRisks\":[],\"dosageIssues\":[]}");
        when(reviewRepository.save(any())).thenAnswer(inv -> {
            PrescriptionAiReview r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        Map<String, Object> request = new HashMap<>();
        request.put("patientId", 1L);
        request.put("patientAge", 30);
        request.put("patientGender", "男");
        request.put("drugs", List.of());

        Map<String, Object> result = prescriptionService.checkPrescription(request, 10L);

        assertNotNull(result.get("reviewResult"));
    }

    // ========== getReviewList() ==========

    @Test
    void getReviewList_ShouldReturnAll_WhenDoctorIdIsNull() {
        when(reviewRepository.findByOrderByReviewTimeDesc(any())).thenReturn(Page.empty());

        var result = prescriptionService.getReviewList(null, 1, 10);

        assertNotNull(result);
        verify(reviewRepository).findByOrderByReviewTimeDesc(any());
    }

    @Test
    void getReviewList_ShouldFilterByDoctorId_WhenProvided() {
        when(reviewRepository.findByDoctorIdOrderByReviewTimeDesc(eq(10L), any())).thenReturn(Page.empty());

        var result = prescriptionService.getReviewList(10L, 1, 10);

        assertNotNull(result);
        verify(reviewRepository).findByDoctorIdOrderByReviewTimeDesc(eq(10L), any());
    }

    // ========== getReviewDetail() ==========

    @Test
    void getReviewDetail_ShouldReturnRecord_WhenExists() {
        PrescriptionAiReview review = new PrescriptionAiReview();
        review.setId(1L);

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        assertEquals(1L, prescriptionService.getReviewDetail(1L).getId());
    }

    @Test
    void getReviewDetail_ShouldThrow_WhenNotFound() {
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> prescriptionService.getReviewDetail(99L));
    }
}
