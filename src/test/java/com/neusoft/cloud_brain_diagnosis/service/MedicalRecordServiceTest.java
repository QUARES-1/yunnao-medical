package com.neusoft.cloud_brain_diagnosis.service;

import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.entity.*;
import com.neusoft.cloud_brain_diagnosis.repository.*;
import com.neusoft.cloud_brain_diagnosis.service.impl.MedicalRecordServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MedicalRecordService 单元测试
 * 覆盖：新增/修改病历、病历查询（按ID/挂号ID/患者/医生）
 */
@ExtendWith(MockitoExtension.class)
class MedicalRecordServiceTest {

    @Mock private MedicalRecordRepository medicalRecordRepository;
    @Mock private RegistrationRepository registrationRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private DoctorRepository doctorRepository;

    private MedicalRecordServiceImpl medicalRecordService;

    @BeforeEach
    void setUp() {
        medicalRecordService = new MedicalRecordServiceImpl(
                medicalRecordRepository, registrationRepository, patientRepository, doctorRepository);
    }

    // ========== 保存病历（新增） ==========

    @Test
    void saveRecord_ShouldCreateNewRecord() {
        Registration reg = new Registration();
        reg.setId(100L);
        reg.setPatientId(1L);
        reg.setDoctorId(10L);
        reg.setDepartmentId(50L);

        Patient patient = new Patient();
        patient.setId(1L);
        patient.setName("患者张三");

        Doctor doctor = new Doctor();
        doctor.setId(10L);
        doctor.setName("医生李四");

        MedicalRecord input = new MedicalRecord();
        input.setRegistrationId(100L);
        input.setChiefComplaint("头痛");
        input.setDiagnosis("感冒");

        when(registrationRepository.findById(100L)).thenReturn(Optional.of(reg));
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(10L)).thenReturn(Optional.of(doctor));
        when(medicalRecordRepository.findByRegistrationId(100L)).thenReturn(Optional.empty());
        when(medicalRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MedicalRecord result = medicalRecordService.saveRecord(input);
        assertEquals("患者张三", result.getPatientName());
        assertEquals("医生李四", result.getDoctorName());
        assertEquals("头痛", result.getChiefComplaint());
        assertEquals("感冒", result.getDiagnosis());
    }

    @Test
    void saveRecord_ShouldThrow_WhenRegistrationIdIsNull() {
        MedicalRecord input = new MedicalRecord();
        input.setId(null);
        assertThrows(BusinessException.class, () -> medicalRecordService.saveRecord(input));
    }

    @Test
    void saveRecord_ShouldThrow_WhenRegistrationNotFound() {
        MedicalRecord input = new MedicalRecord();
        input.setRegistrationId(999L);

        when(registrationRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> medicalRecordService.saveRecord(input));
    }

    // ========== 保存病历（修改已有） ==========

    @Test
    void saveRecord_ShouldUpdateExisting_WhenIdProvided() {
        MedicalRecord existing = new MedicalRecord();
        existing.setId(1L);
        existing.setChiefComplaint("旧主诉");
        existing.setDiagnosis("旧诊断");

        MedicalRecord input = new MedicalRecord();
        input.setId(1L);
        input.setChiefComplaint("新主诉");
        input.setDiagnosis(null); // 不更新

        when(medicalRecordRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(medicalRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MedicalRecord result = medicalRecordService.saveRecord(input);
        assertEquals("新主诉", result.getChiefComplaint());
        assertEquals("旧诊断", result.getDiagnosis()); // 保持不变
    }

    @Test
    void saveRecord_ShouldUpdateFromRegistration_WhenRecordExistsForSameRegistration() {
        Registration reg = new Registration();
        reg.setId(100L);
        reg.setPatientId(1L);
        reg.setDoctorId(10L);
        reg.setDepartmentId(50L);

        Patient patient = new Patient();
        patient.setId(1L);
        patient.setName("患者");

        Doctor doctor = new Doctor();
        doctor.setId(10L);
        doctor.setName("医生");

        MedicalRecord existing = new MedicalRecord();
        existing.setRegistrationId(100L);
        existing.setChiefComplaint("旧主诉");

        MedicalRecord input = new MedicalRecord();
        input.setRegistrationId(100L);
        input.setChiefComplaint("新主诉");

        when(registrationRepository.findById(100L)).thenReturn(Optional.of(reg));
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(10L)).thenReturn(Optional.of(doctor));
        when(medicalRecordRepository.findByRegistrationId(100L)).thenReturn(Optional.of(existing));
        when(medicalRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MedicalRecord result = medicalRecordService.saveRecord(input);
        assertEquals("新主诉", result.getChiefComplaint());
    }

    // ========== 查询 ==========

    @Test
    void getDetail_ShouldReturnRecord() {
        MedicalRecord record = new MedicalRecord();
        record.setId(1L);
        when(medicalRecordRepository.findById(1L)).thenReturn(Optional.of(record));

        assertEquals(1L, medicalRecordService.getDetail(1L).getId());
    }

    @Test
    void getDetail_ShouldThrow_WhenNotFound() {
        when(medicalRecordRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> medicalRecordService.getDetail(99L));
    }

    @Test
    void getByRegistrationId_ShouldReturnRecord_WhenExists() {
        MedicalRecord record = new MedicalRecord();
        record.setRegistrationId(100L);
        when(medicalRecordRepository.findByRegistrationId(100L)).thenReturn(Optional.of(record));

        MedicalRecord result = medicalRecordService.getByRegistrationId(100L);
        assertNotNull(result);
        assertEquals(100L, result.getRegistrationId());
    }

    @Test
    void getByRegistrationId_ShouldReturnNull_WhenNotExists() {
        when(medicalRecordRepository.findByRegistrationId(999L)).thenReturn(Optional.empty());
        assertNull(medicalRecordService.getByRegistrationId(999L));
    }

    @Test
    void getPatientList_ShouldReturnPage() {
        Page<MedicalRecord> page = new PageImpl<>(List.of(new MedicalRecord()));
        when(medicalRecordRepository.findByPatientIdOrderByCreateTimeDesc(eq(1L), any(Pageable.class)))
                .thenReturn(page);
        assertEquals(1, medicalRecordService.getPatientList(1L, 1, 10).getContent().size());
    }

    @Test
    void getDoctorList_ShouldReturnPage() {
        Page<MedicalRecord> page = new PageImpl<>(List.of(new MedicalRecord()));
        when(medicalRecordRepository.findByDoctorIdOrderByCreateTimeDesc(eq(10L), any(Pageable.class)))
                .thenReturn(page);
        assertEquals(1, medicalRecordService.getDoctorList(10L, 1, 10).getContent().size());
    }
}
