package com.neusoft.cloud_brain_diagnosis.service.impl;

import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.entity.Doctor;
import com.neusoft.cloud_brain_diagnosis.entity.MedicalRecord;
import com.neusoft.cloud_brain_diagnosis.entity.Patient;
import com.neusoft.cloud_brain_diagnosis.entity.Registration;
import com.neusoft.cloud_brain_diagnosis.repository.DoctorRepository;
import com.neusoft.cloud_brain_diagnosis.repository.MedicalRecordRepository;
import com.neusoft.cloud_brain_diagnosis.repository.PatientRepository;
import com.neusoft.cloud_brain_diagnosis.repository.RegistrationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicalRecordServiceImplTest {

    @Mock
    private MedicalRecordRepository medicalRecordRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @InjectMocks
    private MedicalRecordServiceImpl medicalRecordService;

    private MedicalRecord testRecord;
    private Registration testRegistration;
    private Patient testPatient;
    private Doctor testDoctor;

    @BeforeEach
    void setUp() {
        testRegistration = new Registration();
        testRegistration.setId(100L);
        testRegistration.setDoctorId(1L);
        testRegistration.setPatientId(2L);
        testRegistration.setStatus("就诊中");

        testPatient = new Patient();
        testPatient.setId(2L);
        testPatient.setName("测试患者");

        testDoctor = new Doctor();
        testDoctor.setId(1L);
        testDoctor.setName("测试医生");

        testRecord = new MedicalRecord();
        testRecord.setId(50L);
        testRecord.setRegistrationId(100L);
        testRecord.setPatientId(2L);
        testRecord.setDoctorId(1L);
        testRecord.setChiefComplaint("头疼");
        testRecord.setDiagnosis("偏头痛");
    }

    @Test
    void saveRecord_updateExistingRecord_success() {
        when(medicalRecordRepository.findById(50L)).thenReturn(Optional.of(testRecord));
        when(registrationRepository.findById(100L)).thenReturn(Optional.of(testRegistration));
        when(medicalRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MedicalRecord update = new MedicalRecord();
        update.setId(50L);
        update.setChiefComplaint("Updated complaint");
        update.setDiagnosis("Updated diagnosis");

        MedicalRecord result = medicalRecordService.saveRecord(update, 1L);

        assertEquals("Updated complaint", result.getChiefComplaint());
        assertEquals("Updated diagnosis", result.getDiagnosis());
    }

    @Test
    void saveRecord_partialUpdate_keepsOtherFields() {
        testRecord.setChiefComplaint("Old complaint");
        testRecord.setPresentIllness("Old illness");
        testRecord.setPastHistory("Old history");

        when(medicalRecordRepository.findById(50L)).thenReturn(Optional.of(testRecord));
        when(registrationRepository.findById(100L)).thenReturn(Optional.of(testRegistration));
        when(medicalRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MedicalRecord update = new MedicalRecord();
        update.setId(50L);
        update.setChiefComplaint(null);
        update.setDiagnosis("New diagnosis");
        update.setPastHistory(null);

        MedicalRecord result = medicalRecordService.saveRecord(update, 1L);

        assertEquals("Old complaint", result.getChiefComplaint());
        assertEquals("New diagnosis", result.getDiagnosis());
        assertEquals("Old history", result.getPastHistory());
    }

    @Test
    void saveRecord_createNewRecord_success() {
        when(registrationRepository.findById(100L)).thenReturn(Optional.of(testRegistration));
        when(medicalRecordRepository.findByRegistrationId(100L)).thenReturn(Optional.empty());
        when(patientRepository.findById(2L)).thenReturn(Optional.of(testPatient));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(testDoctor));
        when(medicalRecordRepository.save(any())).thenAnswer(inv -> {
            MedicalRecord r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        MedicalRecord record = new MedicalRecord();
        record.setRegistrationId(100L);
        record.setChiefComplaint("头疼");
        record.setDiagnosis("偏头痛");

        MedicalRecord result = medicalRecordService.saveRecord(record, 1L);

        assertEquals(100L, result.getId());
        assertEquals("头疼", result.getChiefComplaint());
        assertEquals("测试患者", result.getPatientName());
        assertEquals("测试医生", result.getDoctorName());
    }

    @Test
    void saveRecord_nullRegistrationId_throwsException() {
        MedicalRecord record = new MedicalRecord();
        record.setChiefComplaint("头疼");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> medicalRecordService.saveRecord(record, 1L));
        assertEquals("挂号ID不能为空", ex.getMessage());
    }

    @Test
    void saveRecord_registrationNotFound_throwsException() {
        when(registrationRepository.findById(99L)).thenReturn(Optional.empty());

        MedicalRecord record = new MedicalRecord();
        record.setRegistrationId(99L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> medicalRecordService.saveRecord(record, 1L));
        assertEquals("挂号记录不存在", ex.getMessage());
    }

    @Test
    void saveRecord_patientNotFound_throwsException() {
        when(registrationRepository.findById(100L)).thenReturn(Optional.of(testRegistration));
        when(patientRepository.findById(2L)).thenReturn(Optional.empty());

        MedicalRecord record = new MedicalRecord();
        record.setRegistrationId(100L);
        record.setChiefComplaint("头疼");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> medicalRecordService.saveRecord(record, 1L));
        assertEquals("患者不存在", ex.getMessage());
    }

    @Test
    void saveRecord_doctorNotFound_throwsException() {
        when(registrationRepository.findById(100L)).thenReturn(Optional.of(testRegistration));
        when(patientRepository.findById(2L)).thenReturn(Optional.of(testPatient));
        when(doctorRepository.findById(1L)).thenReturn(Optional.empty());

        MedicalRecord record = new MedicalRecord();
        record.setRegistrationId(100L);
        record.setChiefComplaint("头疼");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> medicalRecordService.saveRecord(record, 1L));
        assertEquals("医生不存在", ex.getMessage());
    }

    @Test
    void saveRecord_permissionDenied_throwsException() {
        testRegistration.setDoctorId(99L);
        when(medicalRecordRepository.findById(50L)).thenReturn(Optional.of(testRecord));
        when(registrationRepository.findById(100L)).thenReturn(Optional.of(testRegistration));

        MedicalRecord update = new MedicalRecord();
        update.setId(50L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> medicalRecordService.saveRecord(update, 1L));
        assertEquals("无权操作其他医生的患者", ex.getMessage());
    }

    @Test
    void saveRecord_notInProgress_throwsException() {
        testRegistration.setStatus("已完成");
        when(medicalRecordRepository.findById(50L)).thenReturn(Optional.of(testRecord));
        when(registrationRepository.findById(100L)).thenReturn(Optional.of(testRegistration));

        MedicalRecord update = new MedicalRecord();
        update.setId(50L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> medicalRecordService.saveRecord(update, 1L));
        assertEquals("请先开始看诊，只有就诊中的挂号可以编辑病历", ex.getMessage());
    }

    @Test
    void saveRecord_sameRegistration_updatesExistingRecord() {
        MedicalRecord existingRecord = new MedicalRecord();
        existingRecord.setId(60L);
        existingRecord.setRegistrationId(100L);
        existingRecord.setChiefComplaint("Old complaint");

        when(registrationRepository.findById(100L)).thenReturn(Optional.of(testRegistration));
        when(medicalRecordRepository.findByRegistrationId(100L)).thenReturn(Optional.of(existingRecord));
        when(patientRepository.findById(2L)).thenReturn(Optional.of(testPatient));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(testDoctor));
        when(medicalRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MedicalRecord record = new MedicalRecord();
        record.setRegistrationId(100L);
        record.setChiefComplaint("New complaint");

        MedicalRecord result = medicalRecordService.saveRecord(record, 1L);

        assertEquals(60L, result.getId());
        assertEquals("New complaint", result.getChiefComplaint());
    }

    @Test
    void saveRecord_recordNotFound_throwsException() {
        when(medicalRecordRepository.findById(99L)).thenReturn(Optional.empty());

        MedicalRecord update = new MedicalRecord();
        update.setId(99L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> medicalRecordService.saveRecord(update, 1L));
        assertEquals("病历不存在", ex.getMessage());
    }

    @Test
    void getDetail_doctorAccess_success() {
        when(medicalRecordRepository.findById(50L)).thenReturn(Optional.of(testRecord));
        when(registrationRepository.findById(100L)).thenReturn(Optional.of(testRegistration));

        MedicalRecord result = medicalRecordService.getDetail(50L, 1L, "doctor");

        assertEquals(50L, result.getId());
    }

    @Test
    void getDetail_patientAccess_success() {
        testRegistration.setDoctorId(5L);
        testRegistration.setPatientId(2L);
        when(medicalRecordRepository.findById(50L)).thenReturn(Optional.of(testRecord));
        when(registrationRepository.findById(100L)).thenReturn(Optional.of(testRegistration));

        MedicalRecord result = medicalRecordService.getDetail(50L, 2L, "patient");

        assertEquals(50L, result.getId());
    }

    @Test
    void getDetail_adminAccess_success() {
        testRegistration.setDoctorId(5L);
        testRegistration.setPatientId(5L);
        when(medicalRecordRepository.findById(50L)).thenReturn(Optional.of(testRecord));
        when(registrationRepository.findById(100L)).thenReturn(Optional.of(testRegistration));

        MedicalRecord result = medicalRecordService.getDetail(50L, 1L, "admin");

        assertEquals(50L, result.getId());
    }

    @Test
    void getDetail_doctorCannotAccessOtherDoctorRecord_throwsException() {
        testRegistration.setDoctorId(99L);
        when(medicalRecordRepository.findById(50L)).thenReturn(Optional.of(testRecord));
        when(registrationRepository.findById(100L)).thenReturn(Optional.of(testRegistration));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> medicalRecordService.getDetail(50L, 1L, "doctor"));
        assertEquals("该患者不属于当前医生，无权查看病历", ex.getMessage());
    }

    @Test
    void getDetail_patientCannotAccessOtherRecord_throwsException() {
        testRegistration.setPatientId(99L);
        when(medicalRecordRepository.findById(50L)).thenReturn(Optional.of(testRecord));
        when(registrationRepository.findById(100L)).thenReturn(Optional.of(testRegistration));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> medicalRecordService.getDetail(50L, 1L, "patient"));
        assertEquals("无权查看其他患者的病历", ex.getMessage());
    }

    @Test
    void getDetail_invalidRole_throwsException() {
        testRegistration.setDoctorId(1L);
        testRegistration.setPatientId(2L);
        when(medicalRecordRepository.findById(50L)).thenReturn(Optional.of(testRecord));
        when(registrationRepository.findById(100L)).thenReturn(Optional.of(testRegistration));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> medicalRecordService.getDetail(50L, 1L, "invalid"));
        assertEquals("当前角色无权查看病历", ex.getMessage());
    }

    @Test
    void getDetail_recordNotFound_throwsException() {
        when(medicalRecordRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> medicalRecordService.getDetail(99L, 1L, "admin"));
    }

    @Test
    void getByRegistrationId_patientAccess_success() {
        testRegistration.setPatientId(1L);
        when(registrationRepository.findById(100L)).thenReturn(Optional.of(testRegistration));
        when(medicalRecordRepository.findByRegistrationId(100L)).thenReturn(Optional.of(testRecord));

        MedicalRecord result = medicalRecordService.getByRegistrationId(100L, 1L, "patient");

        assertNotNull(result);
        assertEquals(100L, result.getRegistrationId());
    }

    @Test
    void getByRegistrationId_doctorAccess_success() {
        testRegistration.setDoctorId(1L);
        when(registrationRepository.findById(100L)).thenReturn(Optional.of(testRegistration));
        when(medicalRecordRepository.findByRegistrationId(100L)).thenReturn(Optional.of(testRecord));

        MedicalRecord result = medicalRecordService.getByRegistrationId(100L, 1L, "doctor");

        assertNotNull(result);
        assertEquals(100L, result.getRegistrationId());
    }

    @Test
    void getByRegistrationId_notFound_returnsNull() {
        testRegistration.setDoctorId(1L);
        when(registrationRepository.findById(100L)).thenReturn(Optional.of(testRegistration));
        when(medicalRecordRepository.findByRegistrationId(100L)).thenReturn(Optional.empty());

        MedicalRecord result = medicalRecordService.getByRegistrationId(100L, 1L, "doctor");

        assertNull(result);
    }

    @Test
    void getPatientList_success() {
        Page<MedicalRecord> page = new PageImpl<>(Collections.singletonList(testRecord));
        when(medicalRecordRepository.findByPatientIdOrderByCreateTimeDesc(eq(2L), any(Pageable.class))).thenReturn(page);

        Page<MedicalRecord> result = medicalRecordService.getPatientList(2L, 1, 10);

        assertEquals(1, result.getContent().size());
    }

    @Test
    void getDoctorList_success() {
        Page<MedicalRecord> page = new PageImpl<>(Collections.singletonList(testRecord));
        when(medicalRecordRepository.findByDoctorIdOrderByCreateTimeDesc(eq(1L), any(Pageable.class))).thenReturn(page);

        Page<MedicalRecord> result = medicalRecordService.getDoctorList(1L, 1, 10);

        assertEquals(1, result.getContent().size());
    }
}
