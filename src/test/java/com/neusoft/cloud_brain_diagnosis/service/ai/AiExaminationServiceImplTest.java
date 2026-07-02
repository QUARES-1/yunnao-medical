package com.neusoft.cloud_brain_diagnosis.service.ai;

import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.common.util.AiApiUtil;
import com.neusoft.cloud_brain_diagnosis.entity.*;
import com.neusoft.cloud_brain_diagnosis.repository.*;
import com.neusoft.cloud_brain_diagnosis.service.ai.impl.AiExaminationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiExaminationServiceImplTest {

    @Mock private ExaminationAiInterpretationRepository interpretationRepository;
    @Mock private CriticalValueWarningRepository criticalValueRepository;
    @Mock private ExaminationAiReviewRepository reviewRepository;
    @Mock private ExaminationRepository examinationRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private AiApiUtil aiApiUtil;

    private AiExaminationServiceImpl examinationService;

    @BeforeEach
    void setUp() {
        examinationService = new AiExaminationServiceImpl(
                interpretationRepository, criticalValueRepository, reviewRepository,
                examinationRepository, patientRepository, aiApiUtil);
    }

    // ========== interpret() ==========

    @Test
    void interpret_ShouldReturnInterpretation_WithValidResponse() {
        Examination exam = new Examination();
        exam.setId(1L);
        exam.setPatientId(1L);
        exam.setResult("WBC:12.5");

        when(examinationRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"abnormalItems\":[{\"name\":\"WBC\",\"value\":\"12.5\"}],\"interpretationPro\":\"High\",\"interpretationPatient\":\"Elevated\",\"suggestions\":\"Retest\",\"reviewReminder\":\"Retest in 1 week\"}");
        when(interpretationRepository.save(any())).thenAnswer(inv -> {
            ExaminationAiInterpretation i = inv.getArgument(0);
            i.setId(100L);
            return i;
        });

        Map<String, Object> result = examinationService.interpret(1L);

        assertEquals(100L, result.get("id"));
        assertEquals("Elevated", result.get("interpretation"));
    }

    @Test
    void interpret_ShouldThrow_WhenExaminationNotFound() {
        when(examinationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> examinationService.interpret(99L));
    }

    @Test
    void interpret_ShouldHandleParseError() {
        Examination exam = new Examination();
        exam.setId(1L);
        exam.setPatientId(1L);
        exam.setResult("Result");

        when(examinationRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn("non-json response");
        when(interpretationRepository.save(any())).thenAnswer(inv -> {
            ExaminationAiInterpretation i = inv.getArgument(0);
            i.setId(100L);
            return i;
        });

        Map<String, Object> result = examinationService.interpret(1L);

        assertEquals("non-json response", result.get("interpretation"));
    }

    @Test
    void interpret_ShouldHandleNullAbnormalItems() {
        Examination exam = new Examination();
        exam.setId(1L);
        exam.setPatientId(1L);
        exam.setResult("Normal result");

        when(examinationRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"interpretationPro\":\"Normal\",\"interpretationPatient\":\"Normal\",\"suggestions\":\"None\"}");
        when(interpretationRepository.save(any())).thenAnswer(inv -> {
            ExaminationAiInterpretation i = inv.getArgument(0);
            i.setId(100L);
            return i;
        });

        Map<String, Object> result = examinationService.interpret(1L);

        assertNotNull(result);
        assertEquals("Normal", result.get("interpretation"));
    }

    // ========== getPatientInterpretation() ==========

    @Test
    void getPatientInterpretation_ShouldReturnRecord_WhenExists() {
        ExaminationAiInterpretation interpretation = new ExaminationAiInterpretation();
        interpretation.setId(1L);

        when(interpretationRepository.findByExaminationId(1L)).thenReturn(Optional.of(interpretation));

        assertEquals(1L, examinationService.getPatientInterpretation(1L).getId());
    }

    @Test
    void getPatientInterpretation_ShouldThrow_WhenNotFound() {
        when(interpretationRepository.findByExaminationId(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> examinationService.getPatientInterpretation(99L));
    }

    // ========== getProInterpretation() ==========

    @Test
    void getProInterpretation_ShouldReturnRecord_WhenExists() {
        ExaminationAiInterpretation interpretation = new ExaminationAiInterpretation();
        interpretation.setId(1L);

        when(interpretationRepository.findByExaminationId(1L)).thenReturn(Optional.of(interpretation));

        assertEquals(1L, examinationService.getProInterpretation(1L).getId());
    }

    @Test
    void getProInterpretation_ShouldThrow_WhenNotFound() {
        when(interpretationRepository.findByExaminationId(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> examinationService.getProInterpretation(99L));
    }

    // ========== getCriticalList() ==========

    @Test
    void getCriticalList_ShouldReturnByDoctor() {
        CriticalValueWarning warning = new CriticalValueWarning();
        warning.setId(1L);

        when(criticalValueRepository.findByDoctorIdOrderByCreateTimeDesc(eq(100L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(warning)));

        Page<CriticalValueWarning> result = examinationService.getCriticalList(100L, "doctor", 1, 10);

        assertEquals(1, result.getContent().size());
    }

    @Test
    void getCriticalList_ShouldReturnByPatient() {
        CriticalValueWarning warning = new CriticalValueWarning();
        warning.setId(1L);

        when(criticalValueRepository.findByPatientIdOrderByCreateTimeDesc(eq(200L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(warning)));

        Page<CriticalValueWarning> result = examinationService.getCriticalList(200L, "patient", 1, 10);

        assertEquals(1, result.getContent().size());
    }

    @Test
    void getCriticalList_ShouldReturnPending() {
        CriticalValueWarning warning = new CriticalValueWarning();
        warning.setId(1L);

        when(criticalValueRepository.findByStatusOrderByCreateTimeDesc(eq("pending"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(warning)));

        Page<CriticalValueWarning> result = examinationService.getCriticalList(100L, "lab", 1, 10);

        assertEquals(1, result.getContent().size());
    }

    // ========== detectCriticalValue() ==========

    @Test
    void detectCriticalValue_ShouldDetectLowHemoglobin() {
        Examination exam = new Examination();
        exam.setId(1L);
        exam.setPatientId(1L);
        exam.setDoctorId(10L);
        exam.setDoctorName("Dr. Smith");
        exam.setPatientName("Patient A");
        exam.setResult("HGB:50");

        Patient patient = new Patient();
        patient.setId(1L);
        patient.setPhone("13800000000");

        when(examinationRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(criticalValueRepository.existsByExaminationId(1L)).thenReturn(false);
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(criticalValueRepository.save(any())).thenAnswer(inv -> {
            CriticalValueWarning w = inv.getArgument(0);
            w.setId(100L);
            return w;
        });

        CriticalValueWarning result = examinationService.detectCriticalValue(1L);

        assertNotNull(result);
        assertEquals("critical", result.getWarningLevel());
        assertNotNull(result.getCriticalItems());
    }

    @Test
    void detectCriticalValue_ShouldDetectHighBloodGlucose() {
        Examination exam = new Examination();
        exam.setId(1L);
        exam.setPatientId(1L);
        exam.setDoctorId(10L);
        exam.setDoctorName("Dr. Smith");
        exam.setPatientName("Patient A");
        exam.setResult("GLU:18.5");

        when(examinationRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(criticalValueRepository.existsByExaminationId(1L)).thenReturn(false);
        when(patientRepository.findById(1L)).thenReturn(Optional.empty());
        when(criticalValueRepository.save(any())).thenAnswer(inv -> {
            CriticalValueWarning w = inv.getArgument(0);
            w.setId(100L);
            return w;
        });

        CriticalValueWarning result = examinationService.detectCriticalValue(1L);

        assertNotNull(result);
        assertNotNull(result.getCriticalItems());
    }

    @Test
    void detectCriticalValue_ShouldReturnNull_WhenAlreadyExists() {
        Examination exam = new Examination();
        exam.setId(1L);

        when(examinationRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(criticalValueRepository.existsByExaminationId(1L)).thenReturn(true);

        CriticalValueWarning result = examinationService.detectCriticalValue(1L);

        assertNull(result);
        verify(criticalValueRepository, never()).save(any());
    }

    @Test
    void detectCriticalValue_ShouldReturnNull_WhenNoCriticalValues() {
        Examination exam = new Examination();
        exam.setId(1L);
        exam.setPatientId(1L);
        exam.setDoctorId(10L);
        exam.setDoctorName("Dr. Smith");
        exam.setPatientName("Patient A");
        exam.setResult("WBC:8.0 (Normal range)");

        when(examinationRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(criticalValueRepository.existsByExaminationId(1L)).thenReturn(false);

        CriticalValueWarning result = examinationService.detectCriticalValue(1L);

        assertNull(result);
    }

    @Test
    void detectCriticalValue_ShouldReturnNull_WhenResultIsNull() {
        Examination exam = new Examination();
        exam.setId(1L);
        exam.setPatientId(1L);
        exam.setDoctorId(10L);
        exam.setDoctorName("Dr. Smith");
        exam.setPatientName("Patient A");
        exam.setResult(null);

        when(examinationRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(criticalValueRepository.existsByExaminationId(1L)).thenReturn(false);

        CriticalValueWarning result = examinationService.detectCriticalValue(1L);

        assertNull(result);
    }

    // ========== confirmWarning() ==========

    @Test
    void confirmWarning_ShouldUpdateStatus() {
        CriticalValueWarning warning = new CriticalValueWarning();
        warning.setId(1L);

        when(criticalValueRepository.findById(1L)).thenReturn(Optional.of(warning));
        when(criticalValueRepository.save(any())).thenReturn(warning);

        String result = examinationService.confirmWarning(1L, 100L);

        assertNotNull(result);
        verify(criticalValueRepository).save(argThat(w -> "confirmed".equals(w.getStatus())));
    }

    @Test
    void confirmWarning_ShouldThrow_WhenNotFound() {
        when(criticalValueRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> examinationService.confirmWarning(99L, 100L));
    }

    // ========== processWarning() ==========

    @Test
    void processWarning_ShouldUpdateStatusAndRemark() {
        CriticalValueWarning warning = new CriticalValueWarning();
        warning.setId(1L);

        when(criticalValueRepository.findById(1L)).thenReturn(Optional.of(warning));
        when(criticalValueRepository.save(any())).thenReturn(warning);

        String result = examinationService.processWarning(1L, "Processed", 100L);

        assertNotNull(result);
        verify(criticalValueRepository).save(argThat(w -> 
            "processed".equals(w.getStatus())));
    }

    @Test
    void processWarning_ShouldThrow_WhenNotFound() {
        when(criticalValueRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> examinationService.processWarning(99L, "Remark", 100L));
    }

    // ========== getCriticalHistory() ==========

    @Test
    void getCriticalHistory_ShouldReturnPage() {
        CriticalValueWarning warning = new CriticalValueWarning();
        warning.setId(1L);

        when(criticalValueRepository.findByOrderByCreateTimeDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(warning)));

        Page<CriticalValueWarning> result = examinationService.getCriticalHistory(1, 10);

        assertEquals(1, result.getContent().size());
    }

    // ========== reviewExamination() ==========

    @Test
    void reviewExamination_ShouldCreateReview() {
        Examination exam = new Examination();
        exam.setId(1L);
        exam.setPatientId(1L);
        exam.setResult("WBC:12.5");

        when(examinationRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"reviewResult\":\"pass\",\"reviewScore\":85,\"abnormalItems\":[],\"suggestions\":\"Normal\"}");
        when(reviewRepository.findByExaminationId(1L)).thenReturn(Optional.empty());
        when(reviewRepository.save(any())).thenAnswer(inv -> {
            ExaminationAiReview r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        Map<String, Object> result = examinationService.reviewExamination(1L, 50L);

        assertNotNull(result);
        assertEquals(100L, result.get("id"));
        assertEquals("pass", result.get("reviewResult"));
    }

    @Test
    void reviewExamination_ShouldUpdateExistingReview() {
        Examination exam = new Examination();
        exam.setId(1L);
        exam.setPatientId(1L);
        exam.setResult("WBC:12.5");

        ExaminationAiReview existingReview = new ExaminationAiReview();
        existingReview.setId(50L);

        when(examinationRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"reviewResult\":\"pass\",\"reviewScore\":85,\"suggestions\":\"Normal\"}");
        when(reviewRepository.findByExaminationId(1L)).thenReturn(Optional.of(existingReview));
        when(reviewRepository.save(any())).thenReturn(existingReview);

        Map<String, Object> result = examinationService.reviewExamination(1L, 50L);

        assertNotNull(result);
    }

    @Test
    void reviewExamination_ShouldThrow_WhenNotFound() {
        when(examinationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> examinationService.reviewExamination(99L, 50L));
    }

    // Note: Parse error test is hard to verify due to encoding issues

    // ========== getManualList() ==========

    @Test
    void getManualList_ShouldReturnPage() {
        ExaminationAiReview review = new ExaminationAiReview();
        review.setId(1L);

        when(reviewRepository.findByReviewResultOrderByReviewTimeDesc(eq("manual"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(review)));

        Page<ExaminationAiReview> result = examinationService.getManualList(1, 10);

        assertEquals(1, result.getContent().size());
    }

    // ========== getReviewList() ==========

    @Test
    void getReviewList_ShouldFilterByResult() {
        ExaminationAiReview review = new ExaminationAiReview();
        review.setId(1L);

        when(reviewRepository.findByReviewResultOrderByReviewTimeDesc(eq("pass"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(review)));

        Page<ExaminationAiReview> result = examinationService.getReviewList("pass", 1, 10);

        assertEquals(1, result.getContent().size());
    }

    @Test
    void getReviewList_ShouldReturnAll_WhenResultIsNull() {
        ExaminationAiReview review = new ExaminationAiReview();
        review.setId(1L);

        when(reviewRepository.findByOrderByReviewTimeDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(review)));

        Page<ExaminationAiReview> result = examinationService.getReviewList(null, 1, 10);

        assertEquals(1, result.getContent().size());
    }

    @Test
    void getReviewList_ShouldReturnAll_WhenResultIsEmpty() {
        ExaminationAiReview review = new ExaminationAiReview();
        review.setId(1L);

        when(reviewRepository.findByOrderByReviewTimeDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(review)));

        Page<ExaminationAiReview> result = examinationService.getReviewList("", 1, 10);

        assertEquals(1, result.getContent().size());
    }

    @Test
    void getReviewList_ShouldReturnAll_WhenResultIsAll() {
        ExaminationAiReview review = new ExaminationAiReview();
        review.setId(1L);

        when(reviewRepository.findByOrderByReviewTimeDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(review)));

        Page<ExaminationAiReview> result = examinationService.getReviewList("全部", 1, 10);

        assertEquals(1, result.getContent().size());
    }

    // ========== getReviewDetail() ==========

    @Test
    void getReviewDetail_ShouldReturnReview() {
        ExaminationAiReview review = new ExaminationAiReview();
        review.setId(1L);

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        ExaminationAiReview result = examinationService.getReviewDetail(1L);

        assertEquals(1L, result.getId());
    }

    @Test
    void getReviewDetail_ShouldThrow_WhenNotFound() {
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> examinationService.getReviewDetail(99L));
    }

    // ========== manualConfirm() ==========

    @Test
    void manualConfirm_ShouldUpdateResult() {
        ExaminationAiReview review = new ExaminationAiReview();
        review.setId(1L);

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any())).thenReturn(review);

        String result = examinationService.manualConfirm(1L);

        assertNotNull(result);
        verify(reviewRepository).save(argThat(r -> "pass".equals(r.getReviewResult())));
    }

    @Test
    void manualConfirm_ShouldThrow_WhenNotFound() {
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> examinationService.manualConfirm(99L));
    }

    // ========== reject() ==========

    @Test
    void reject_ShouldUpdateResultAndReason() {
        ExaminationAiReview review = new ExaminationAiReview();
        review.setId(1L);

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any())).thenReturn(review);

        String result = examinationService.reject(1L, "Does not meet standard");

        assertNotNull(result);
        verify(reviewRepository).save(argThat(r -> 
            "reject".equals(r.getReviewResult())));
    }

    @Test
    void reject_ShouldThrow_WhenNotFound() {
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> examinationService.reject(99L, "Reason"));
    }

    // ========== getReviewStats() ==========

    @Test
    void getReviewStats_ShouldCalculateStats() {
        ExaminationAiReview pass = new ExaminationAiReview();
        pass.setReviewResult("pass");
        ExaminationAiReview manual = new ExaminationAiReview();
        manual.setReviewResult("manual");
        ExaminationAiReview reject = new ExaminationAiReview();
        reject.setReviewResult("reject");

        when(reviewRepository.findAll()).thenReturn(List.of(pass, pass, manual, reject));

        Map<String, Object> result = examinationService.getReviewStats();

        assertEquals(4L, result.get("total"));
        assertEquals(2L, result.get("passCount"));
        assertEquals(1L, result.get("manualCount"));
        assertEquals(1L, result.get("rejectCount"));
    }

    @Test
    void getReviewStats_ShouldHandleEmptyList() {
        when(reviewRepository.findAll()).thenReturn(List.of());

        Map<String, Object> result = examinationService.getReviewStats();

        assertEquals(0L, result.get("total"));
        assertNotNull(result.get("passRate"));
    }
}
