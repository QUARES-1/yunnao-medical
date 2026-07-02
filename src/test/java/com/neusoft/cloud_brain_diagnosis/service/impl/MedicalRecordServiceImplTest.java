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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * MedicalRecordServiceImpl 白盒单元测试
 * 覆盖：新增、修改、详情、列表、权限验证
 */
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

    private MedicalRecordServiceImpl medicalRecordService;

    @BeforeEach
    void setUp() {
        medicalRecordService = new MedicalRecordServiceImpl(
                medicalRecordRepository, registrationRepository, patientRepository, doctorRepository);
    }

    // ========== saveRecord - 修改病历 ==========

    @Test
    void saveRecord_ShouldUpdateExistingRecord() {
        MedicalRecord existing = new MedicalRecord();
        existing.setId(1L);
        existing.setRegistrationId(10L);
        existing.setChiefComplaint("旧主诉");

        Registration registration = new Registration();
        registration.setId(10L);
        registration.setDoctorId(1L);
        registration.setPatientId(2L);
        registration.setStatus("就诊中");

        when(medicalRecordRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(registrationRepository.findById(10L)).thenReturn(Optional.of(registration));
        when(medicalRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MedicalRecord update = new MedicalRecord();
        update.setId(1L);
        update.setChiefComplaint("新主诉");
        update.setDiagnosis("新诊断");

        MedicalRecord result = medicalRecordService.saveRecord(update, 1L);

        assertEquals("新主诉", result.getChiefComplaint());
        assertEquals("新诊断", result.getDiagnosis());
    }

    @Test
    void saveRecord_ShouldOnlyUpdateNonNullFields() {
        MedicalRecord existing = new MedicalRecord();
        existing.setId(1L);
        existing.setRegistrationId(10L);
        existing.setChiefComplaint("旧主诉");
        existing.setDiagnosis("旧诊断");

        Registration registration = new Registration();
        registration.setId(10L);
        registration.setDoctorId(1L);
        registration.setPatientId(2L);
        registration.setStatus("就诊中");

        when(medicalRecordRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(registrationRepository.findById(10L)).thenReturn(Optional.of(registration));
        when(medicalRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MedicalRecord update = new MedicalRecord();
        update.setId(1L);
        update.setChiefComplaint(null);
        update.setDiagnosis("只更新诊断");

        MedicalRecord result = medicalRecordService.saveRecord(update, 1L);

        assertEquals("旧主诉", result.getChiefComplaint());
        assertEquals("只更新诊断", result.getDiagnosis());
    }

    @Test
    void saveRecord_ShouldThrow_WhenRecordNotFound() {
        when(medicalRecordRepository.findById(99L)).thenReturn(Optional.empty());

        MedicalRecord update = new MedicalRecord();
        update.setId(99L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> medicalRecordService.saveRecord(update, 1L));
        assertEquals("病历不存在", ex.getMessage());
    }

    // ========== saveRecord - 新增病历 ==========

    @Test
    void saveRecord_ShouldCreateNewRecord_WhenIdIsNull() {
        Registration registration = new Registration();
        registration.setId(10L);
        registration.setDoctorId(1L);
        registration.setPatientId(2L);
        registration.setDepartmentId(5L);
        registration.setStatus("就诊中");

        Patient patient = new Patient();
        patient.setId(2L);
        patient.setName("张三");

        Doctor doctor = new Doctor();
        doctor.setId(1L);
        doctor.setName("李医生");

        when(registrationRepository.findById(10L)).thenReturn(Optional.of(registration));
        when(medicalRecordRepository.findByRegistrationId(10L)).thenReturn(Optional.empty());
        when(patientRepository.findById(2L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(medicalRecordRepository.save(any())).thenAnswer(inv -> {
            MedicalRecord r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        MedicalRecord record = new MedicalRecord();
        record.setRegistrationId(10L);
        record.setChiefComplaint("头痛");
        record.setDiagnosis("偏头痛");

        MedicalRecord result = medicalRecordService.saveRecord(record, 1L);

        assertEquals(100L, result.getId());
        assertEquals("头痛", result.getChiefComplaint());
        assertEquals("张三", result.getPatientName());
        assertEquals("李医生", result.getDoctorName());
        assertEquals(5L, result.getDepartmentId());
    }

    @Test
    void saveRecord_ShouldUpdateExistingRecord_WhenSameRegistration() {
        Registration registration = new Registration();
        registration.setId(10L);
        registration.setDoctorId(1L);
        registration.setPatientId(2L);
        registration.setDepartmentId(5L);
        registration.setStatus("就诊中");

        Patient patient = new Patient();
        patient.setId(2L);
        patient.setName("张三");

        Doctor doctor = new Doctor();
        doctor.setId(1L);
        doctor.setName("李医生");

        MedicalRecord existingRecord = new MedicalRecord();
        existingRecord.setId(50L);
        existingRecord.setRegistrationId(10L);

        when(registrationRepository.findById(10L)).thenReturn(Optional.of(registration));
        when(medicalRecordRepository.findByRegistrationId(10L)).thenReturn(Optional.of(existingRecord));
        when(patientRepository.findById(2L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(medicalRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MedicalRecord record = new MedicalRecord();
        record.setRegistrationId(10L);
        record.setChiefComplaint("更新主诉");

        MedicalRecord result = medicalRecordService.saveRecord(record, 1L);

        assertEquals(50L, result.getId());
        assertEquals("更新主诉", result.getChiefComplaint());
    }

    @Test
    void saveRecord_ShouldThrow_WhenRegistrationIdIsNull() {
        MedicalRecord record = new MedicalRecord();
        record.setChiefComplaint("头痛");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> medicalRecordService.saveRecord(record, 1L));
        assertEquals("挂号ID不能为空", ex.getMessage());
    }

    @Test
    void saveRecord_ShouldThrow_WhenRegistrationNotFound() {
        when(registrationRepository.findById(99L)).thenReturn(Optional.empty());

        MedicalRecord record = new MedicalRecord();
        record.setRegistrationId(99L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> medicalRecordService.saveRecord(record, 1L));
        assertEquals("挂号记录不存在", ex.getMessage());
    }

    @Test
    void saveRecord_ShouldThrow_WhenDoctorNotOperate() {
        Registration registration = new Registration();
        registration.setId(10L);
        registration.setDoctorId(99L);
        registration.setStatus("就诊中");

        when(registrationRepository.findById(10L)).thenReturn(Optional.of(registration));

        MedicalRecord record = new MedicalRecord();
        record.setRegistrationId(10L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> medicalRecordService.saveRecord(record, 1L));
        assertEquals("无权操作其他医生的患者", ex.getMessage());
    }

    @Test
    void saveRecord_ShouldThrow_WhenRegistrationNotInProgress() {
        Registration registration = new Registration();
        registration.setId(10L);
        registration.setDoctorId(1L);
        registration.setStatus("待就诊");

        when(registrationRepository.findById(10L)).thenReturn(Optional.of(registration));

        MedicalRecord record = new MedicalRecord();
        record.setRegistrationId(10L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> medicalRecordService.saveRecord(record, 1L));
        assertEquals("请先开始看诊，只有就诊中的挂号可以编辑病历", ex.getMessage());
    }

    // ========== getDetail ==========

    @Test
    void getDetail_ShouldReturnRecord_ForDoctor() {
        MedicalRecord record = new MedicalRecord();
        record.setId(1L);
        record.setRegistrationId(10L);

        Registration registration = new Registration();
        registration.setId(10L);
        registration.setDoctorId(1L);
        registration.setPatientId(2L);

        when(medicalRecordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(registrationRepository.findById(10L)).thenReturn(Optional.of(registration));

        MedicalRecord result = medicalRecordService.getDetail(1L, 1L, "doctor");

        assertEquals(1L, result.getId());
    }

    @Test
    void getDetail_ShouldReturnRecord_ForPatient() {
        MedicalRecord record = new MedicalRecord();
        record.setId(1L);
        record.setRegistrationId(10L);

        Registration registration = new Registration();
        registration.setId(10L);
        registration.setDoctorId(5L);
        registration.setPatientId(1L);

        when(medicalRecordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(registrationRepository.findById(10L)).thenReturn(Optional.of(registration));

        MedicalRecord result = medicalRecordService.getDetail(1L, 1L, "patient");

        assertEquals(1L, result.getId());
    }

    @Test
    void getDetail_ShouldThrow_WhenDoctorCannotAccess() {
        MedicalRecord record = new MedicalRecord();
        record.setId(1L);
        record.setRegistrationId(10L);

        Registration registration = new Registration();
        registration.setId(10L);
        registration.setDoctorId(99L);
        registration.setPatientId(2L);

        when(medicalRecordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(registrationRepository.findById(10L)).thenReturn(Optional.of(registration));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> medicalRecordService.getDetail(1L, 1L, "doctor"));
        assertEquals("该患者不属于当前医生，无权查看病历", ex.getMessage());
    }

    @Test
    void getDetail_ShouldThrow_WhenPatientCannotAccess() {
        MedicalRecord record = new MedicalRecord();
        record.setId(1L);
        record.setRegistrationId(10L);

        Registration registration = new Registration();
        registration.setId(10L);
        registration.setDoctorId(5L);
        registration.setPatientId(99L);

        when(medicalRecordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(registrationRepository.findById(10L)).thenReturn(Optional.of(registration));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> medicalRecordService.getDetail(1L, 1L, "patient"));
        assertEquals("无权查看其他患者的病历", ex.getMessage());
    }

    @Test
    void getDetail_ShouldThrow_WhenInvalidRole() {
        MedicalRecord record = new MedicalRecord();
        record.setId(1L);
        record.setRegistrationId(10L);

        Registration registration = new Registration();
        registration.setId(10L);
        registration.setDoctorId(1L);
        registration.setPatientId(2L);

        when(medicalRecordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(registrationRepository.findById(10L)).thenReturn(Optional.of(registration));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> medicalRecordService.getDetail(1L, 1L, "invalid"));
        assertEquals("当前角色无权查看病历", ex.getMessage());
    }

    @Test
    void getDetail_ShouldThrow_WhenNotFound() {
        when(medicalRecordRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> medicalRecordService.getDetail(99L, 1L, "admin"));
        assertEquals("病历不存在", ex.getMessage());
    }

    // ========== getPatientList / getDoctorList ==========

    @Test
    void getPatientList_ShouldReturnPage() {
        MedicalRecord record = new MedicalRecord();
        record.setId(1L);

        Page<MedicalRecord> page = new PageImpl<>(List.of(record));
        when(medicalRecordRepository.findByPatientIdOrderByCreateTimeDesc(eq(1L), any(Pageable.class))).thenReturn(page);

        Page<MedicalRecord> result = medicalRecordService.getPatientList(1L, 1, 10);

        assertEquals(1, result.getContent().size());
    }

    @Test
    void getDoctorList_ShouldReturnPage() {
        MedicalRecord record = new MedicalRecord();
        record.setId(1L);

        Page<MedicalRecord> page = new PageImpl<>(List.of(record));
        when(medicalRecordRepository.findByDoctorIdOrderByCreateTimeDesc(eq(1L), any(Pageable.class))).thenReturn(page);

        Page<MedicalRecord> result = medicalRecordService.getDoctorList(1L, 1, 10);

        assertEquals(1, result.getContent().size());
    }

    @Test
    void getByRegistrationId_ShouldReturnRecord() {
        MedicalRecord record = new MedicalRecord();
        record.setId(1L);

        Registration registration = new Registration();
        registration.setId(10L);
        registration.setDoctorId(1L);
        registration.setPatientId(2L);

        when(medicalRecordRepository.findByRegistrationId(10L)).thenReturn(Optional.of(record));
        when(registrationRepository.findById(10L)).thenReturn(Optional.of(registration));

        MedicalRecord result = medicalRecordService.getByRegistrationId(10L, 1L, "doctor");

        assertEquals(1L, result.getId());
    }

    @Test
    void getByRegistrationId_ShouldReturnNull_WhenNotFound() {
        Registration registration = new Registration();
        registration.setId(10L);
        registration.setDoctorId(1L);
        registration.setPatientId(2L);

        when(medicalRecordRepository.findByRegistrationId(10L)).thenReturn(Optional.empty());
        when(registrationRepository.findById(10L)).thenReturn(Optional.of(registration));

        MedicalRecord result = medicalRecordService.getByRegistrationId(10L, 1L, "doctor");

        assertNull(result);
    }
}
