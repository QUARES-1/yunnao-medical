package com.neusoft.cloud_brain_diagnosis.service.ai;

import com.neusoft.cloud_brain_diagnosis.common.context.UserContext;
import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.common.util.AiApiUtil;
import com.neusoft.cloud_brain_diagnosis.entity.*;
import com.neusoft.cloud_brain_diagnosis.repository.*;
import com.neusoft.cloud_brain_diagnosis.service.ai.impl.AiMedicationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiMedicationServiceImplTest {

    @Mock private MedicationGuideRepository guideRepository;
    @Mock private PrescriptionRepository prescriptionRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private MedicalRecordRepository medicalRecordRepository;
    @Mock private AiApiUtil aiApiUtil;

    private AiMedicationServiceImpl medicationService;

    @BeforeEach
    void setUp() {
        medicationService = new AiMedicationServiceImpl(
                guideRepository, prescriptionRepository, patientRepository,
                medicalRecordRepository, aiApiUtil);
    }

    // ========== generateGuide() - Parameter Validation ==========

    @Test
    void generateGuide_ShouldThrow_WhenPrescriptionIdIsNull() {
        assertThrows(BusinessException.class, () -> medicationService.generateGuide(null));
    }

    @Test
    void generateGuide_ShouldThrow_WhenPrescriptionNotFound() {
        when(prescriptionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> medicationService.generateGuide(99L));
    }

    @Test
    void generateGuide_ShouldThrow_WhenNoMedications() {
        Prescription prescription = createPrescription(1L, 100L, "Patient A", null);

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(medicalRecordRepository.findByRegistrationId(any())).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> medicationService.generateGuide(1L));
    }

    @Test
    void generateGuide_ShouldThrow_WhenEmptyMedications() {
        Prescription prescription = createPrescription(1L, 100L, "Patient A", "[]");

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(medicalRecordRepository.findByRegistrationId(any())).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> medicationService.generateGuide(1L));
    }

    // ========== generateGuide() - Main Flow ==========

    @Test
    void generateGuide_ShouldSucceed_WithValidPrescription() {
        Prescription prescription = createPrescription(1L, 100L, "Patient A", "[{\"name\":\"阿奇霉素片\",\"specification\":\"0.25g\",\"quantity\":6}]");
        Patient patient = createPatient(100L, 30, "M", "None");

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(patientRepository.findById(100L)).thenReturn(Optional.of(patient));
        when(medicalRecordRepository.findByRegistrationId(any())).thenReturn(Optional.empty());
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn(createValidAiResponse());
        when(guideRepository.save(any())).thenAnswer(inv -> {
            MedicationGuide g = inv.getArgument(0);
            g.setId(10L);
            g.setCreateTime(LocalDateTime.now());
            return g;
        });

        Map<String, Object> result = medicationService.generateGuide(1L);

        assertNotNull(result);
        assertEquals(10L, result.get("id"));
        assertEquals(1L, result.get("prescriptionId"));
        verify(guideRepository).deleteByPrescriptionId(1L);
    }

    @Test
    void generateGuide_ShouldSucceed_WhenPatientIsNull() {
        Prescription prescription = createPrescription(1L, null, "Patient A", "[{\"name\":\"阿奇霉素片\"}]");

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(medicalRecordRepository.findByRegistrationId(any())).thenReturn(Optional.empty());
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn(createValidAiResponse());
        when(guideRepository.save(any())).thenAnswer(inv -> {
            MedicationGuide g = inv.getArgument(0);
            g.setId(10L);
            g.setCreateTime(LocalDateTime.now());
            return g;
        });

        Map<String, Object> result = medicationService.generateGuide(1L);

        assertNotNull(result);
        assertNull(result.get("patientId"));
    }

    @Test
    void generateGuide_ShouldUseFallback_WhenAiFails() {
        Prescription prescription = createPrescription(1L, 100L, "Patient A", "[{\"name\":\"阿奇霉素片\"}]");
        Patient patient = createPatient(100L, 30, "M", "None");

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(patientRepository.findById(100L)).thenReturn(Optional.of(patient));
        when(medicalRecordRepository.findByRegistrationId(any())).thenReturn(Optional.empty());
        when(aiApiUtil.callAi(anyString(), anyString())).thenThrow(new RuntimeException("AI Error"));
        when(guideRepository.save(any())).thenAnswer(inv -> {
            MedicationGuide g = inv.getArgument(0);
            g.setId(10L);
            g.setCreateTime(LocalDateTime.now());
            return g;
        });

        Map<String, Object> result = medicationService.generateGuide(1L);

        assertNotNull(result);
        assertFalse((Boolean) result.get("aiGenerated"));
        assertEquals("安全规则兜底", result.get("source"));
    }

    @Test
    void generateGuide_ShouldUseFallback_WhenAiResponseIsEmpty() {
        Prescription prescription = createPrescription(1L, 100L, "Patient A", "[{\"name\":\"阿奇霉素片\"}]");
        Patient patient = createPatient(100L, 30, "M", "None");

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(patientRepository.findById(100L)).thenReturn(Optional.of(patient));
        when(medicalRecordRepository.findByRegistrationId(any())).thenReturn(Optional.empty());
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn("");
        when(guideRepository.save(any())).thenAnswer(inv -> {
            MedicationGuide g = inv.getArgument(0);
            g.setId(10L);
            g.setCreateTime(LocalDateTime.now());
            return g;
        });

        Map<String, Object> result = medicationService.generateGuide(1L);

        assertNotNull(result);
        assertFalse((Boolean) result.get("aiGenerated"));
    }

    @Test
    void generateGuide_ShouldUseFallback_WhenAiHasNoMedications() {
        Prescription prescription = createPrescription(1L, 100L, "Patient A", "[{\"name\":\"阿奇霉素片\"}]");
        Patient patient = createPatient(100L, 30, "M", "None");

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(patientRepository.findById(100L)).thenReturn(Optional.of(patient));
        when(medicalRecordRepository.findByRegistrationId(any())).thenReturn(Optional.empty());
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn("{\"summary\":\"test\"}");
        when(guideRepository.save(any())).thenAnswer(inv -> {
            MedicationGuide g = inv.getArgument(0);
            g.setId(10L);
            g.setCreateTime(LocalDateTime.now());
            return g;
        });

        Map<String, Object> result = medicationService.generateGuide(1L);

        assertNotNull(result);
        assertFalse((Boolean) result.get("aiGenerated"));
    }

    @Test
    void generateGuide_ShouldUseFallback_WhenAiNameNotMatch() {
        Prescription prescription = createPrescription(1L, 100L, "Patient A", "[{\"name\":\"阿奇霉素片\"}]");
        Patient patient = createPatient(100L, 30, "M", "None");

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(patientRepository.findById(100L)).thenReturn(Optional.of(patient));
        when(medicalRecordRepository.findByRegistrationId(any())).thenReturn(Optional.empty());
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn(
                "{\"medications\":[{\"name\":\"Totally Different Drug\"}]}");
        when(guideRepository.save(any())).thenAnswer(inv -> {
            MedicationGuide g = inv.getArgument(0);
            g.setId(10L);
            g.setCreateTime(LocalDateTime.now());
            return g;
        });

        Map<String, Object> result = medicationService.generateGuide(1L);

        assertNotNull(result);
        assertFalse((Boolean) result.get("aiGenerated"));
    }

    // ========== getGuide() ==========

    @Test
    void getGuide_ShouldThrow_WhenPrescriptionNotFound() {
        when(prescriptionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> medicationService.getGuide(99L));
    }

    @Test
    void getGuide_ShouldGenerateNew_WhenNoGuidesExist() {
        Prescription prescription = createPrescription(1L, 100L, "Patient A", "[{\"name\":\"阿奇霉素片\"}]");
        Patient patient = createPatient(100L, 30, "M", "None");

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(guideRepository.findAllByPrescriptionIdOrderByIdDesc(1L)).thenReturn(List.of());
        when(patientRepository.findById(100L)).thenReturn(Optional.of(patient));
        when(medicalRecordRepository.findByRegistrationId(any())).thenReturn(Optional.empty());
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn(createValidAiResponse());
        when(guideRepository.save(any())).thenAnswer(inv -> {
            MedicationGuide g = inv.getArgument(0);
            g.setId(10L);
            g.setCreateTime(LocalDateTime.now());
            return g;
        });

        Map<String, Object> result = medicationService.getGuide(1L);

        assertNotNull(result);
        verify(guideRepository).save(any());
    }

    @Test
    void getGuide_ShouldGenerateNew_WhenMultipleGuidesExist() {
        Prescription prescription = createPrescription(1L, 100L, "Patient A", "[{\"name\":\"阿奇霉素片\"}]");
        MedicationGuide guide1 = createMedicationGuide(10L, 1L, 100L);
        MedicationGuide guide2 = createMedicationGuide(11L, 1L, 100L);
        Patient patient = createPatient(100L, 30, "M", "None");

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(guideRepository.findAllByPrescriptionIdOrderByIdDesc(1L))
                .thenReturn(List.of(guide1, guide2));
        when(patientRepository.findById(100L)).thenReturn(Optional.of(patient));
        when(medicalRecordRepository.findByRegistrationId(any())).thenReturn(Optional.empty());
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn(createValidAiResponse());
        when(guideRepository.save(any())).thenAnswer(inv -> {
            MedicationGuide g = inv.getArgument(0);
            g.setId(12L);
            g.setCreateTime(LocalDateTime.now());
            return g;
        });

        Map<String, Object> result = medicationService.getGuide(1L);

        assertNotNull(result);
    }

    @Test
    void getGuide_ShouldGenerateNew_WhenDrugsChanged() {
        Prescription prescription = createPrescription(1L, 100L, "Patient A", "[{\"name\":\"阿奇霉素片\"}]");
        MedicationGuide guide = createMedicationGuide(10L, 1L, 100L);
        guide.setDrugsJson("[{\"name\":\"Different Drug\"}]");
        Patient patient = createPatient(100L, 30, "M", "None");

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(guideRepository.findAllByPrescriptionIdOrderByIdDesc(1L))
                .thenReturn(List.of(guide));
        when(patientRepository.findById(100L)).thenReturn(Optional.of(patient));
        when(medicalRecordRepository.findByRegistrationId(any())).thenReturn(Optional.empty());
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn(createValidAiResponse());
        when(guideRepository.save(any())).thenAnswer(inv -> {
            MedicationGuide g = inv.getArgument(0);
            g.setId(12L);
            g.setCreateTime(LocalDateTime.now());
            return g;
        });

        Map<String, Object> result = medicationService.getGuide(1L);

        assertNotNull(result);
    }

    @Test
    void getGuide_ShouldGenerateNew_WhenPatientIdChanged() {
        Prescription prescription = createPrescription(1L, 100L, "Patient A", "[{\"name\":\"阿奇霉素片\"}]");
        MedicationGuide guide = createMedicationGuide(10L, 1L, 200L);
        guide.setDrugsJson("[{\"name\":\"阿奇霉素片\"}]");
        Patient patient = createPatient(100L, 30, "M", "None");

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(guideRepository.findAllByPrescriptionIdOrderByIdDesc(1L))
                .thenReturn(List.of(guide));
        when(patientRepository.findById(100L)).thenReturn(Optional.of(patient));
        when(medicalRecordRepository.findByRegistrationId(any())).thenReturn(Optional.empty());
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn(createValidAiResponse());
        when(guideRepository.save(any())).thenAnswer(inv -> {
            MedicationGuide g = inv.getArgument(0);
            g.setId(12L);
            g.setCreateTime(LocalDateTime.now());
            return g;
        });

        Map<String, Object> result = medicationService.getGuide(1L);

        assertNotNull(result);
    }

    @Test
    void getGuide_ShouldReturnExistingGuide_WhenValid() {
        Prescription prescription = createPrescription(1L, 100L, "Patient A", "[{\"name\":\"阿奇霉素片\"}]");
        MedicationGuide guide = createMedicationGuide(10L, 1L, 100L);
        guide.setRawResponse(createValidAiResponse());

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(guideRepository.findAllByPrescriptionIdOrderByIdDesc(1L))
                .thenReturn(List.of(guide));
        when(patientRepository.findById(100L)).thenReturn(Optional.empty());
        when(medicalRecordRepository.findByRegistrationId(any())).thenReturn(Optional.empty());

        Map<String, Object> result = medicationService.getGuide(1L);

        assertNotNull(result);
        assertEquals(10L, result.get("id"));
        verify(guideRepository, never()).save(any());
    }

    @Test
    void getGuide_ShouldUseFallback_WhenAiPayloadParseFails() {
        Prescription prescription = createPrescription(1L, 100L, "Patient A", "[{\"name\":\"阿奇霉素片\"}]");
        MedicationGuide guide = createMedicationGuide(10L, 1L, 100L);
        guide.setRawResponse("invalid json");

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(guideRepository.findAllByPrescriptionIdOrderByIdDesc(1L))
                .thenReturn(List.of(guide));
        when(patientRepository.findById(100L)).thenReturn(Optional.empty());
        when(medicalRecordRepository.findByRegistrationId(any())).thenReturn(Optional.empty());

        Map<String, Object> result = medicationService.getGuide(1L);

        assertNotNull(result);
        assertFalse((Boolean) result.get("aiGenerated"));
    }

    @Test
    void getGuide_ShouldHandlePatientNull() {
        Prescription prescription = createPrescription(1L, null, "Patient A", "[{\"name\":\"阿奇霉素片\"}]");
        MedicationGuide guide = createMedicationGuide(10L, 1L, null);
        guide.setDrugsJson("[{\"name\":\"阿奇霉素片\"}]");
        guide.setPatientAge(30);
        guide.setPatientGender("M");
        guide.setRawResponse(createValidAiResponse());

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(guideRepository.findAllByPrescriptionIdOrderByIdDesc(1L))
                .thenReturn(List.of(guide));
        when(medicalRecordRepository.findByRegistrationId(any())).thenReturn(Optional.empty());

        Map<String, Object> result = medicationService.getGuide(1L);

        assertNotNull(result);
        assertEquals(30, result.get("patientAge"));
    }

    // ========== markPrinted() ==========

    @Test
    void markPrinted_ShouldIncrementPrintCount() {
        MedicationGuide guide = new MedicationGuide();
        guide.setId(10L);
        guide.setPatientId(100L);
        guide.setPrintCount(2);

        when(guideRepository.findById(10L)).thenReturn(Optional.of(guide));
        when(guideRepository.save(any())).thenReturn(guide);

        String result = medicationService.markPrinted(10L);

        assertEquals("打印记录已保存", result);
        verify(guideRepository).save(argThat(g -> g.getPrintCount() == 3));
    }

    @Test
    void markPrinted_ShouldStartFromZero_WhenPrintCountIsNull() {
        MedicationGuide guide = new MedicationGuide();
        guide.setId(10L);
        guide.setPatientId(100L);
        guide.setPrintCount(null);

        when(guideRepository.findById(10L)).thenReturn(Optional.of(guide));
        when(guideRepository.save(any())).thenReturn(guide);

        medicationService.markPrinted(10L);

        verify(guideRepository).save(argThat(g -> g.getPrintCount() == 1));
    }

    @Test
    void markPrinted_ShouldThrow_WhenGuideNotFound() {
        when(guideRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> medicationService.markPrinted(99L));
    }

    @Test
    void markPrinted_ShouldThrow_WhenPatientPrintsOthersGuide() {
        MedicationGuide guide = new MedicationGuide();
        guide.setId(10L);
        guide.setPatientId(100L);

        UserContext.setUserId(200L);
        UserContext.setRole(RoleEnum.PATIENT.getCode());

        when(guideRepository.findById(10L)).thenReturn(Optional.of(guide));

        assertThrows(BusinessException.class, () -> medicationService.markPrinted(10L));

        UserContext.clear();
    }

    @Test
    void markPrinted_ShouldSucceed_WhenDoctorPrintsAnyGuide() {
        MedicationGuide guide = new MedicationGuide();
        guide.setId(10L);
        guide.setPatientId(100L);
        guide.setPrintCount(0);

        UserContext.setUserId(200L);
        UserContext.setRole(RoleEnum.DOCTOR.getCode());

        when(guideRepository.findById(10L)).thenReturn(Optional.of(guide));
        when(guideRepository.save(any())).thenReturn(guide);

        String result = medicationService.markPrinted(10L);

        assertEquals("打印记录已保存", result);

        UserContext.clear();
    }

    // ========== parseMedications() ==========

    @Test
    void generateGuide_ShouldHandleInvalidJson() {
        Prescription prescription = createPrescription(1L, 100L, "Patient A", "invalid json");
        Patient patient = createPatient(100L, 30, "M", "None");

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(patientRepository.findById(100L)).thenReturn(Optional.of(patient));
        when(medicalRecordRepository.findByRegistrationId(any())).thenReturn(Optional.empty());
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn(createValidAiResponse());
        when(guideRepository.save(any())).thenAnswer(inv -> {
            MedicationGuide g = inv.getArgument(0);
            g.setId(10L);
            g.setCreateTime(LocalDateTime.now());
            return g;
        });

        Map<String, Object> result = medicationService.generateGuide(1L);

        assertNotNull(result);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> meds = (List<Map<String, Object>>) result.get("medications");
        assertEquals(1, meds.size());
        assertEquals("处方药品", meds.get(0).get("name"));
    }

    // ========== adviceFor() ==========

    @Test
    void generateGuide_ShouldReturnAdviceForAzithromycin() {
        testAdviceForDrug("阿奇霉素片");
    }

    @Test
    void generateGuide_ShouldReturnAdviceForAmbroxol() {
        testAdviceForDrug("氨溴索口服液");
    }

    @Test
    void generateGuide_ShouldReturnAdviceForParacetamol() {
        testAdviceForDrug("对乙酰氨基酚片");
    }

    @Test
    void generateGuide_ShouldReturnAdviceForCephalosporin() {
        testAdviceForDrug("头孢克肟胶囊");
    }

    @Test
    void generateGuide_ShouldReturnAdviceForAmoxicillin() {
        testAdviceForDrug("阿莫西林胶囊");
    }

    @Test
    void generateGuide_ShouldReturnAdviceForIbuprofen() {
        testAdviceForDrug("布洛芬缓释胶囊");
    }

    @Test
    void generateGuide_ShouldReturnAdviceForAspirin() {
        testAdviceForDrug("阿司匹林肠溶片");
    }

    @Test
    void generateGuide_ShouldReturnAdviceForLevofloxacin() {
        testAdviceForDrug("左氧氟沙星片");
    }

    @Test
    void generateGuide_ShouldReturnAdviceForOmeprazole() {
        testAdviceForDrug("奥美拉唑肠溶胶囊");
    }

    @Test
    void generateGuide_ShouldReturnAdviceForDomperidone() {
        testAdviceForDrug("多潘立酮片");
    }

    @Test
    void generateGuide_ShouldReturnAdviceForAtorvastatin() {
        testAdviceForDrug("阿托伐他汀钙片");
    }

    @Test
    void generateGuide_ShouldReturnDefaultAdvice_ForUnknownDrug() {
        testAdviceForDrug("维生素C片");
    }

    // ========== resolveDiagnosis() ==========

    @Test
    void generateGuide_ShouldResolveDiagnosisFromMedicalRecord() {
        Prescription prescription = createPrescription(1L, 100L, "Patient A", "[{\"name\":\"阿奇霉素片\"}]");
        prescription.setRegistrationId(200L);
        MedicalRecord record = new MedicalRecord();
        record.setId(50L);
        record.setPatientId(100L);
        record.setDiagnosis("Upper respiratory infection");
        Patient patient = createPatient(100L, 30, "M", "None");

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(patientRepository.findById(100L)).thenReturn(Optional.of(patient));
        when(medicalRecordRepository.findByRegistrationId(200L)).thenReturn(Optional.of(record));
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn(createValidAiResponse());
        when(guideRepository.save(any())).thenAnswer(inv -> {
            MedicationGuide g = inv.getArgument(0);
            g.setId(10L);
            g.setCreateTime(LocalDateTime.now());
            return g;
        });

        Map<String, Object> result = medicationService.generateGuide(1L);

        assertEquals("Upper respiratory infection", result.get("diagnosis"));
    }

    @Test
    void generateGuide_ShouldReturnDefaultDiagnosis_WhenRecordNotFound() {
        Prescription prescription = createPrescription(1L, 100L, "Patient A", "[{\"name\":\"维生素C片\"}]");
        prescription.setRegistrationId(200L);
        Patient patient = createPatient(100L, 30, "M", "None");

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(patientRepository.findById(100L)).thenReturn(Optional.of(patient));
        when(medicalRecordRepository.findByRegistrationId(200L)).thenReturn(Optional.empty());
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn(createValidAiResponse());
        when(guideRepository.save(any())).thenAnswer(inv -> {
            MedicationGuide g = inv.getArgument(0);
            g.setId(10L);
            g.setCreateTime(LocalDateTime.now());
            return g;
        });

        Map<String, Object> result = medicationService.generateGuide(1L);

        assertEquals("处方相关疾病", result.get("diagnosis"));
    }

    @Test
    void generateGuide_ShouldReturnDefaultDiagnosis_WhenRecordDiagnosisBlank() {
        Prescription prescription = createPrescription(1L, 100L, "Patient A", "[{\"name\":\"维生素B族片\"}]");
        prescription.setRegistrationId(200L);
        MedicalRecord record = new MedicalRecord();
        record.setId(50L);
        record.setPatientId(100L);
        record.setDiagnosis(null);
        Patient patient = createPatient(100L, 30, "M", "None");

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(patientRepository.findById(100L)).thenReturn(Optional.of(patient));
        when(medicalRecordRepository.findByRegistrationId(200L)).thenReturn(Optional.of(record));
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn(createValidAiResponse());
        when(guideRepository.save(any())).thenAnswer(inv -> {
            MedicationGuide g = inv.getArgument(0);
            g.setId(10L);
            g.setCreateTime(LocalDateTime.now());
            return g;
        });

        Map<String, Object> result = medicationService.generateGuide(1L);

        assertEquals("处方相关疾病", result.get("diagnosis"));
    }

    @Test
    void generateGuide_ShouldInferRespiratoryDiagnosis_ForRespiratoryDrugs() {
        Prescription prescription = createPrescription(1L, 100L, "Patient A", "[{\"name\":\"氨溴索口服液\"}]");

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(medicalRecordRepository.findByRegistrationId(any())).thenReturn(Optional.empty());
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn(createValidAiResponse());
        when(guideRepository.save(any())).thenAnswer(inv -> {
            MedicationGuide g = inv.getArgument(0);
            g.setId(10L);
            g.setCreateTime(LocalDateTime.now());
            return g;
        });

        Map<String, Object> result = medicationService.generateGuide(1L);

        assertEquals("呼吸道感染伴咳嗽、咳痰", result.get("diagnosis"));
    }

    @Test
    void generateGuide_ShouldInferDigestiveDiagnosis_ForDigestiveDrugs() {
        Prescription prescription = createPrescription(1L, 100L, "Patient A", "[{\"name\":\"奥美拉唑胶囊\"}]");

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(medicalRecordRepository.findByRegistrationId(any())).thenReturn(Optional.empty());
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn(createValidAiResponse());
        when(guideRepository.save(any())).thenAnswer(inv -> {
            MedicationGuide g = inv.getArgument(0);
            g.setId(10L);
            g.setCreateTime(LocalDateTime.now());
            return g;
        });

        Map<String, Object> result = medicationService.generateGuide(1L);

        assertEquals("消化系统不适", result.get("diagnosis"));
    }

    @Test
    void generateGuide_ShouldInferCardiovascularDiagnosis_ForCardiovascularDrugs() {
        Prescription prescription = createPrescription(1L, 100L, "Patient A", "[{\"name\":\"氨氯地平片\"}]");

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(medicalRecordRepository.findByRegistrationId(any())).thenReturn(Optional.empty());
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn(createValidAiResponse());
        when(guideRepository.save(any())).thenAnswer(inv -> {
            MedicationGuide g = inv.getArgument(0);
            g.setId(10L);
            g.setCreateTime(LocalDateTime.now());
            return g;
        });

        Map<String, Object> result = medicationService.generateGuide(1L);

        assertEquals("心血管慢性病", result.get("diagnosis"));
    }

    // ========== assertPatientOwnsPrescription() ==========

    @Test
    void generateGuide_ShouldThrow_WhenPatientAccessOthersPrescription() {
        Prescription prescription = createPrescription(1L, 100L, "Patient A", "[{\"name\":\"阿奇霉素片\"}]");
        UserContext.setUserId(200L);
        UserContext.setRole(RoleEnum.PATIENT.getCode());

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));

        assertThrows(BusinessException.class, () -> medicationService.generateGuide(1L));

        UserContext.clear();
    }

    @Test
    void generateGuide_ShouldSucceed_WhenDoctorAccessAnyPrescription() {
        Prescription prescription = createPrescription(1L, 100L, "Patient A", "[{\"name\":\"阿奇霉素片\"}]");
        UserContext.setUserId(200L);
        UserContext.setRole(RoleEnum.DOCTOR.getCode());
        Patient patient = createPatient(100L, 30, "M", "None");

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(patientRepository.findById(100L)).thenReturn(Optional.of(patient));
        when(medicalRecordRepository.findByRegistrationId(any())).thenReturn(Optional.empty());
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn(createValidAiResponse());
        when(guideRepository.save(any())).thenAnswer(inv -> {
            MedicationGuide g = inv.getArgument(0);
            g.setId(10L);
            g.setCreateTime(LocalDateTime.now());
            return g;
        });

        Map<String, Object> result = medicationService.generateGuide(1L);

        assertNotNull(result);

        UserContext.clear();
    }

    // ========== Helper Methods ==========

    // ========== Edge Cases & Exception Boundaries ==========

    @Test
    void generateGuide_ShouldUseFallback_WhenAiReturnsBlankName() {
        Prescription prescription = createPrescription(1L, 100L, "Patient A", "[{\"name\":\"阿奇霉素片\"}]");
        Patient patient = createPatient(100L, 30, "M", "None");

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(patientRepository.findById(100L)).thenReturn(Optional.of(patient));
        when(medicalRecordRepository.findByRegistrationId(any())).thenReturn(Optional.empty());
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"medications\":[{\"name\":\"\"}]}");
        when(guideRepository.save(any())).thenAnswer(inv -> {
            MedicationGuide g = inv.getArgument(0);
            g.setId(10L);
            g.setCreateTime(LocalDateTime.now());
            return g;
        });

        Map<String, Object> result = medicationService.generateGuide(1L);

        assertNotNull(result);
        assertFalse((Boolean) result.get("aiGenerated"));
    }

    @Test
    void generateGuide_ShouldHandleAiResponseWithNullFields() {
        Prescription prescription = createPrescription(1L, 100L, "Patient A", "[{\"name\":\"阿奇霉素片\"}]");
        Patient patient = createPatient(100L, 30, "M", "None");

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(patientRepository.findById(100L)).thenReturn(Optional.of(patient));
        when(medicalRecordRepository.findByRegistrationId(any())).thenReturn(Optional.empty());
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"medications\":[{\"name\":\"阿奇霉素片\"}],\"generalAdvice\":null,\"followUpAdvice\":null,\"guideContent\":null}");
        when(guideRepository.save(any())).thenAnswer(inv -> {
            MedicationGuide g = inv.getArgument(0);
            g.setId(10L);
            g.setCreateTime(LocalDateTime.now());
            return g;
        });

        Map<String, Object> result = medicationService.generateGuide(1L);

        assertNotNull(result);
        assertTrue((Boolean) result.get("aiGenerated"));
        assertNotNull(result.get("generalAdvice"));
        assertNotNull(result.get("followUpAdvice"));
        assertNotNull(result.get("guideContent"));
    }

    @Test
    void generateGuide_ShouldHandleAiResponseWithEmptyGeneralAdvice() {
        Prescription prescription = createPrescription(1L, 100L, "Patient A", "[{\"name\":\"阿奇霉素片\"}]");
        Patient patient = createPatient(100L, 30, "M", "None");

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(patientRepository.findById(100L)).thenReturn(Optional.of(patient));
        when(medicalRecordRepository.findByRegistrationId(any())).thenReturn(Optional.empty());
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"medications\":[{\"name\":\"阿奇霉素片\",\"takingTime\":\"每日1次\",\"dietRestrictions\":\"避免饮酒\",\"adverseReactions\":\"恶心\",\"precautions\":\"遵医嘱\",\"missedDose\":\"补服\"}],\"generalAdvice\":[]}");
        when(guideRepository.save(any())).thenAnswer(inv -> {
            MedicationGuide g = inv.getArgument(0);
            g.setId(10L);
            g.setCreateTime(LocalDateTime.now());
            return g;
        });

        Map<String, Object> result = medicationService.generateGuide(1L);

        assertNotNull(result);
        assertTrue((Boolean) result.get("aiGenerated"));
        @SuppressWarnings("unchecked")
        List<String> advice = (List<String>) result.get("generalAdvice");
        assertEquals(3, advice.size());
    }

    @Test
    void generateGuide_ShouldMatchAiNameWithContains() {
        Prescription prescription = createPrescription(1L, 100L, "Patient A", "[{\"name\":\"阿奇霉素\"}]");
        Patient patient = createPatient(100L, 30, "M", "None");

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(patientRepository.findById(100L)).thenReturn(Optional.of(patient));
        when(medicalRecordRepository.findByRegistrationId(any())).thenReturn(Optional.empty());
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"medications\":[{\"name\":\"阿奇霉素片\",\"takingTime\":\"每日1次\",\"dietRestrictions\":\"避免饮酒\",\"adverseReactions\":\"恶心\",\"precautions\":\"遵医嘱\",\"missedDose\":\"补服\"}]}");
        when(guideRepository.save(any())).thenAnswer(inv -> {
            MedicationGuide g = inv.getArgument(0);
            g.setId(10L);
            g.setCreateTime(LocalDateTime.now());
            return g;
        });

        Map<String, Object> result = medicationService.generateGuide(1L);

        assertNotNull(result);
        assertTrue((Boolean) result.get("aiGenerated"));
    }

    @Test
    void generateGuide_ShouldMatchAiNameReversed() {
        Prescription prescription = createPrescription(1L, 100L, "Patient A", "[{\"name\":\"阿奇霉素片\"}]");
        Patient patient = createPatient(100L, 30, "M", "None");

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(patientRepository.findById(100L)).thenReturn(Optional.of(patient));
        when(medicalRecordRepository.findByRegistrationId(any())).thenReturn(Optional.empty());
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"medications\":[{\"name\":\"阿奇霉素\",\"takingTime\":\"每日1次\",\"dietRestrictions\":\"避免饮酒\",\"adverseReactions\":\"恶心\",\"precautions\":\"遵医嘱\",\"missedDose\":\"补服\"}]}");
        when(guideRepository.save(any())).thenAnswer(inv -> {
            MedicationGuide g = inv.getArgument(0);
            g.setId(10L);
            g.setCreateTime(LocalDateTime.now());
            return g;
        });

        Map<String, Object> result = medicationService.generateGuide(1L);

        assertNotNull(result);
        assertTrue((Boolean) result.get("aiGenerated"));
    }

    @Test
    void generateGuide_ShouldHandleAiResponseWithMarkdownCodeBlock() {
        Prescription prescription = createPrescription(1L, 100L, "Patient A", "[{\"name\":\"阿奇霉素片\"}]");
        Patient patient = createPatient(100L, 30, "M", "None");

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(patientRepository.findById(100L)).thenReturn(Optional.of(patient));
        when(medicalRecordRepository.findByRegistrationId(any())).thenReturn(Optional.empty());
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("```json\n" + createValidAiResponse() + "\n```");
        when(guideRepository.save(any())).thenAnswer(inv -> {
            MedicationGuide g = inv.getArgument(0);
            g.setId(10L);
            g.setCreateTime(LocalDateTime.now());
            return g;
        });

        Map<String, Object> result = medicationService.generateGuide(1L);

        assertNotNull(result);
        assertTrue((Boolean) result.get("aiGenerated"));
    }

    @Test
    void generateGuide_ShouldHandleAiResponseWithPlainCodeBlock() {
        Prescription prescription = createPrescription(1L, 100L, "Patient A", "[{\"name\":\"阿奇霉素片\"}]");
        Patient patient = createPatient(100L, 30, "M", "None");

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(patientRepository.findById(100L)).thenReturn(Optional.of(patient));
        when(medicalRecordRepository.findByRegistrationId(any())).thenReturn(Optional.empty());
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("```\n" + createValidAiResponse() + "\n```");
        when(guideRepository.save(any())).thenAnswer(inv -> {
            MedicationGuide g = inv.getArgument(0);
            g.setId(10L);
            g.setCreateTime(LocalDateTime.now());
            return g;
        });

        Map<String, Object> result = medicationService.generateGuide(1L);

        assertNotNull(result);
        assertTrue((Boolean) result.get("aiGenerated"));
    }

    @Test
    void generateGuide_ShouldHandleNullPatientName() {
        Prescription prescription = createPrescription(1L, 100L, null, "[{\"name\":\"阿奇霉素片\"}]");
        Patient patient = createPatient(100L, 30, "M", "None");

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(patientRepository.findById(100L)).thenReturn(Optional.of(patient));
        when(medicalRecordRepository.findByRegistrationId(any())).thenReturn(Optional.empty());
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn(createValidAiResponse());
        when(guideRepository.save(any())).thenAnswer(inv -> {
            MedicationGuide g = inv.getArgument(0);
            g.setId(10L);
            g.setCreateTime(LocalDateTime.now());
            return g;
        });

        Map<String, Object> result = medicationService.generateGuide(1L);

        assertNotNull(result);
    }

    @Test
    void generateGuide_ShouldHandlePatientWithNullAge() {
        Prescription prescription = createPrescription(1L, 100L, "Patient A", "[{\"name\":\"复合维生素B片\"}]");
        Patient patient = createPatient(100L, null, "M", "None");

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(patientRepository.findById(100L)).thenReturn(Optional.of(patient));
        when(medicalRecordRepository.findByRegistrationId(any())).thenReturn(Optional.empty());
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn(createValidAiResponse());
        when(guideRepository.save(any())).thenAnswer(inv -> {
            MedicationGuide g = inv.getArgument(0);
            g.setId(10L);
            g.setCreateTime(LocalDateTime.now());
            return g;
        });

        Map<String, Object> result = medicationService.generateGuide(1L);

        assertNotNull(result);
        assertNull(result.get("patientAge"));
    }

    @Test
    void generateGuide_ShouldHandlePatientWithNullGender() {
        Prescription prescription = createPrescription(1L, 100L, "Patient A", "[{\"name\":\"复合维生素B片\"}]");
        Patient patient = createPatient(100L, 30, null, "None");

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(patientRepository.findById(100L)).thenReturn(Optional.of(patient));
        when(medicalRecordRepository.findByRegistrationId(any())).thenReturn(Optional.empty());
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn(createValidAiResponse());
        when(guideRepository.save(any())).thenAnswer(inv -> {
            MedicationGuide g = inv.getArgument(0);
            g.setId(10L);
            g.setCreateTime(LocalDateTime.now());
            return g;
        });

        Map<String, Object> result = medicationService.generateGuide(1L);

        assertNotNull(result);
        assertNull(result.get("patientGender"));
    }

    @Test
    void generateGuide_ShouldHandlePatientWithNullAllergyHistory() {
        Prescription prescription = createPrescription(1L, 100L, "Patient A", "[{\"name\":\"阿奇霉素片\"}]");
        Patient patient = createPatient(100L, 30, "M", null);

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(patientRepository.findById(100L)).thenReturn(Optional.of(patient));
        when(medicalRecordRepository.findByRegistrationId(any())).thenReturn(Optional.empty());
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn(createValidAiResponse());
        when(guideRepository.save(any())).thenAnswer(inv -> {
            MedicationGuide g = inv.getArgument(0);
            g.setId(10L);
            g.setCreateTime(LocalDateTime.now());
            return g;
        });

        Map<String, Object> result = medicationService.generateGuide(1L);

        assertNotNull(result);
        assertEquals("无已知药物过敏史", result.get("allergyHistory"));
    }

    @Test
    void generateGuide_ShouldHandleDrugsWithNullSpec() {
        Prescription prescription = createPrescription(1L, 100L, "Patient A", "[{\"name\":\"阿奇霉素片\",\"specification\":null}]");
        Patient patient = createPatient(100L, 30, "M", "None");

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(patientRepository.findById(100L)).thenReturn(Optional.of(patient));
        when(medicalRecordRepository.findByRegistrationId(any())).thenReturn(Optional.empty());
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn(createValidAiResponse());
        when(guideRepository.save(any())).thenAnswer(inv -> {
            MedicationGuide g = inv.getArgument(0);
            g.setId(10L);
            g.setCreateTime(LocalDateTime.now());
            return g;
        });

        Map<String, Object> result = medicationService.generateGuide(1L);

        assertNotNull(result);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> meds = (List<Map<String, Object>>) result.get("medications");
        assertEquals("规格未填写", meds.get(0).get("specification"));
    }

    @Test
    void generateGuide_ShouldHandleDrugsWithNullUnit() {
        Prescription prescription = createPrescription(1L, 100L, "Patient A", "[{\"name\":\"阿奇霉素片\",\"unit\":null}]");
        Patient patient = createPatient(100L, 30, "M", "None");

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(patientRepository.findById(100L)).thenReturn(Optional.of(patient));
        when(medicalRecordRepository.findByRegistrationId(any())).thenReturn(Optional.empty());
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn(createValidAiResponse());
        when(guideRepository.save(any())).thenAnswer(inv -> {
            MedicationGuide g = inv.getArgument(0);
            g.setId(10L);
            g.setCreateTime(LocalDateTime.now());
            return g;
        });

        Map<String, Object> result = medicationService.generateGuide(1L);

        assertNotNull(result);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> meds = (List<Map<String, Object>>) result.get("medications");
        assertEquals("盒", meds.get(0).get("unit"));
    }

    @Test
    void generateGuide_ShouldHandleDrugsWithNullUsage() {
        Prescription prescription = createPrescription(1L, 100L, "Patient A", "[{\"name\":\"阿奇霉素片\",\"usage\":null}]");
        Patient patient = createPatient(100L, 30, "M", "None");

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(patientRepository.findById(100L)).thenReturn(Optional.of(patient));
        when(medicalRecordRepository.findByRegistrationId(any())).thenReturn(Optional.empty());
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn(createValidAiResponse());
        when(guideRepository.save(any())).thenAnswer(inv -> {
            MedicationGuide g = inv.getArgument(0);
            g.setId(10L);
            g.setCreateTime(LocalDateTime.now());
            return g;
        });

        Map<String, Object> result = medicationService.generateGuide(1L);

        assertNotNull(result);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> meds = (List<Map<String, Object>>) result.get("medications");
        assertEquals("请按医嘱使用", meds.get(0).get("usage"));
    }

    @Test
    void generateGuide_ShouldIgnoreMedicalRecordWithDifferentPatientId() {
        Prescription prescription = createPrescription(1L, 100L, "Patient A", "[{\"name\":\"复合维生素B片\"}]");
        prescription.setRegistrationId(200L);
        MedicalRecord record = new MedicalRecord();
        record.setId(50L);
        record.setPatientId(999L);
        record.setDiagnosis("Some diagnosis");
        Patient patient = createPatient(100L, 30, "M", "None");

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(patientRepository.findById(100L)).thenReturn(Optional.of(patient));
        when(medicalRecordRepository.findByRegistrationId(200L)).thenReturn(Optional.of(record));
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn(createValidAiResponse());
        when(guideRepository.save(any())).thenAnswer(inv -> {
            MedicationGuide g = inv.getArgument(0);
            g.setId(10L);
            g.setCreateTime(LocalDateTime.now());
            return g;
        });

        Map<String, Object> result = medicationService.generateGuide(1L);

        assertNotNull(result);
        assertEquals("处方相关疾病", result.get("diagnosis"));
    }

    @Test
    void getGuide_ShouldReturnZeroPrintCount_WhenNull() {
        Prescription prescription = createPrescription(1L, 100L, "Patient A", "[{\"name\":\"阿奇霉素片\"}]");
        MedicationGuide guide = createMedicationGuide(10L, 1L, 100L);
        guide.setRawResponse(createValidAiResponse());
        guide.setPrintCount(null);

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(guideRepository.findAllByPrescriptionIdOrderByIdDesc(1L))
                .thenReturn(List.of(guide));
        when(patientRepository.findById(100L)).thenReturn(Optional.empty());
        when(medicalRecordRepository.findByRegistrationId(any())).thenReturn(Optional.empty());

        Map<String, Object> result = medicationService.getGuide(1L);

        assertNotNull(result);
        assertEquals(0, result.get("printCount"));
    }

    @Test
    void generateGuide_ShouldUseDefaultFollowUpAdvice_WhenNull() {
        Prescription prescription = createPrescription(1L, 100L, "Patient A", "[{\"name\":\"阿奇霉素片\"}]");
        Patient patient = createPatient(100L, 30, "M", "None");

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(patientRepository.findById(100L)).thenReturn(Optional.of(patient));
        when(medicalRecordRepository.findByRegistrationId(any())).thenReturn(Optional.empty());
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"medications\":[{\"name\":\"阿奇霉素片\",\"takingTime\":\"每日1次\",\"dietRestrictions\":\"避免饮酒\",\"adverseReactions\":\"恶心\",\"precautions\":\"遵医嘱\",\"missedDose\":\"补服\"}],\"followUpAdvice\":null}");
        when(guideRepository.save(any())).thenAnswer(inv -> {
            MedicationGuide g = inv.getArgument(0);
            g.setId(10L);
            g.setCreateTime(LocalDateTime.now());
            return g;
        });

        Map<String, Object> result = medicationService.generateGuide(1L);

        assertNotNull(result);
        assertEquals("症状持续加重或出现明显不适时，请及时复诊。", result.get("followUpAdvice"));
    }

    @Test
    void generateGuide_ShouldMatchMultipleMedications() {
        Prescription prescription = createPrescription(1L, 100L, "Patient A",
                "[{\"name\":\"阿奇霉素片\"},{\"name\":\"布洛芬片\"}]");
        Patient patient = createPatient(100L, 30, "M", "None");

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(patientRepository.findById(100L)).thenReturn(Optional.of(patient));
        when(medicalRecordRepository.findByRegistrationId(any())).thenReturn(Optional.empty());
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"medications\":[{\"name\":\"阿奇霉素片\",\"takingTime\":\"每日1次\",\"dietRestrictions\":\"避免饮酒\",\"adverseReactions\":\"恶心\",\"precautions\":\"遵医嘱\",\"missedDose\":\"补服\"},{\"name\":\"布洛芬片\",\"takingTime\":\"每日3次\",\"dietRestrictions\":\"饭后服用\",\"adverseReactions\":\"胃痛\",\"precautions\":\"不要超量\",\"missedDose\":\"不补\"}]}");
        when(guideRepository.save(any())).thenAnswer(inv -> {
            MedicationGuide g = inv.getArgument(0);
            g.setId(10L);
            g.setCreateTime(LocalDateTime.now());
            return g;
        });

        Map<String, Object> result = medicationService.generateGuide(1L);

        assertNotNull(result);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> meds = (List<Map<String, Object>>) result.get("medications");
        assertEquals(2, meds.size());
        assertTrue((Boolean) result.get("aiGenerated"));
    }

    private void testAdviceForDrug(String drugName) {
        Prescription prescription = createPrescription(1L, 100L, "Patient A", "[{\"name\":\"" + drugName + "\"}]");
        Patient patient = createPatient(100L, 30, "M", "None");

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(patientRepository.findById(100L)).thenReturn(Optional.of(patient));
        when(medicalRecordRepository.findByRegistrationId(any())).thenReturn(Optional.empty());
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn(createValidAiResponse());
        when(guideRepository.save(any())).thenAnswer(inv -> {
            MedicationGuide g = inv.getArgument(0);
            g.setId(10L);
            g.setCreateTime(LocalDateTime.now());
            return g;
        });

        Map<String, Object> result = medicationService.generateGuide(1L);

        assertNotNull(result);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> meds = (List<Map<String, Object>>) result.get("medications");
        assertNotNull(meds);
        assertFalse(meds.isEmpty());
        assertNotNull(meds.get(0).get("takingTime"));
        assertNotNull(meds.get(0).get("dietRestrictions"));
        assertNotNull(meds.get(0).get("adverseReactions"));
        assertNotNull(meds.get(0).get("precautions"));
    }

    private Prescription createPrescription(Long id, Long patientId, String patientName, String drugs) {
        Prescription p = new Prescription();
        p.setId(id);
        p.setPatientId(patientId);
        p.setPatientName(patientName);
        p.setDrugs(drugs);
        p.setStatus("pending");
        p.setDoctorName("Dr. Smith");
        return p;
    }

    private Patient createPatient(Long id, Integer age, String gender, String allergyHistory) {
        Patient p = new Patient();
        p.setId(id);
        p.setAge(age);
        p.setGender(gender);
        p.setAllergyHistory(allergyHistory);
        return p;
    }

    private MedicationGuide createMedicationGuide(Long id, Long prescriptionId, Long patientId) {
        MedicationGuide g = new MedicationGuide();
        g.setId(id);
        g.setPrescriptionId(prescriptionId);
        g.setPatientId(patientId);
        g.setDrugsJson("[{\"name\":\"阿奇霉素片\"}]");
        g.setCreateTime(LocalDateTime.now());
        return g;
    }

    private String createValidAiResponse() {
        return """
                {
                  "summary": "Medication guide summary",
                  "medications": [
                    {
                      "name": "阿奇霉素片",
                      "takingTime": "每日固定时间服用",
                      "dietRestrictions": "避免饮酒",
                      "adverseReactions": "可能出现恶心、腹痛",
                      "precautions": "对大环内酯类过敏者慎用",
                      "missedDose": "想起后尽快补服"
                    }
                  ],
                  "generalAdvice": ["按时服药", "注意饮食", "定期复诊"],
                  "followUpAdvice": "症状持续请复诊",
                  "guideContent": "详细用药指导内容"
                }
                """;
    }
}
