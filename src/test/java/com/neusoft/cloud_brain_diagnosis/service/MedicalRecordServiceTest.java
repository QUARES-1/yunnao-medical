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
 * MedicalRecordService 白盒单元测试
 * 覆盖：新增/修改病历、病历查询（按ID/挂号ID/患者/医生）、权限控制
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
        reg.setStatus("就诊中");

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

        MedicalRecord result = medicalRecordService.saveRecord(input, 10L);
        assertEquals("患者张三", result.getPatientName());
        assertEquals("医生李四", result.getDoctorName());
        assertEquals("头痛", result.getChiefComplaint());
        assertEquals("感冒", result.getDiagnosis());
    }

    @Test
    void saveRecord_ShouldThrow_WhenRegistrationIdIsNull() {
        MedicalRecord input = new MedicalRecord();
        input.setId(null);
        input.setRegistrationId(null);
        assertThrows(BusinessException.class, () -> medicalRecordService.saveRecord(input, 10L));
    }

    @Test
    void saveRecord_ShouldThrow_WhenRegistrationNotFound() {
        MedicalRecord input = new MedicalRecord();
        input.setRegistrationId(999L);

        when(registrationRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> medicalRecordService.saveRecord(input, 10L));
    }

    @Test
    void saveRecord_ShouldThrow_WhenDoctorNotOperate() {
        Registration reg = new Registration();
        reg.setId(100L);
        reg.setPatientId(1L);
        reg.setDoctorId(20L); // 其他医生的挂号
        reg.setStatus("就诊中");

        MedicalRecord input = new MedicalRecord();
        input.setRegistrationId(100L);

        when(registrationRepository.findById(100L)).thenReturn(Optional.of(reg));
        assertThrows(BusinessException.class, () -> medicalRecordService.saveRecord(input, 10L));
    }

    @Test
    void saveRecord_ShouldThrow_WhenRegistrationNotConsulting() {
        Registration reg = new Registration();
        reg.setId(100L);
        reg.setPatientId(1L);
        reg.setDoctorId(10L);
        reg.setStatus("待就诊"); // 未开始看诊

        MedicalRecord input = new MedicalRecord();
        input.setRegistrationId(100L);

        when(registrationRepository.findById(100L)).thenReturn(Optional.of(reg));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> medicalRecordService.saveRecord(input, 10L));
        assertTrue(ex.getMessage().contains("请先开始看诊"));
    }

    // ========== 保存病历（修改已有） ==========

    @Test
    void saveRecord_ShouldUpdateExisting_WhenIdProvided() {
        Registration reg = new Registration();
        reg.setId(100L);
        reg.setPatientId(1L);
        reg.setDoctorId(10L);
        reg.setStatus("就诊中");

        MedicalRecord existing = new MedicalRecord();
        existing.setId(1L);
        existing.setRegistrationId(100L);
        existing.setChiefComplaint("旧主诉");
        existing.setDiagnosis("旧诊断");

        MedicalRecord input = new MedicalRecord();
        input.setId(1L);
        input.setChiefComplaint("新主诉");
        input.setDiagnosis(null); // 不更新

        when(medicalRecordRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(registrationRepository.findById(100L)).thenReturn(Optional.of(reg));
        when(medicalRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MedicalRecord result = medicalRecordService.saveRecord(input, 10L);
        assertEquals("新主诉", result.getChiefComplaint());
        assertEquals("旧诊断", result.getDiagnosis()); // 保持不变
    }

    @Test
    void saveRecord_ShouldUpdateAllOptionalFields_WhenIdProvided() {
        Registration reg = new Registration();
        reg.setId(100L);
        reg.setPatientId(1L);
        reg.setDoctorId(10L);
        reg.setStatus("就诊中");

        MedicalRecord existing = new MedicalRecord();
        existing.setId(1L);
        existing.setRegistrationId(100L);
        existing.setChiefComplaint("旧主诉");

        MedicalRecord input = new MedicalRecord();
        input.setId(1L);
        input.setChiefComplaint("新主诉");
        input.setPresentIllness("新现病史");
        input.setPastHistory("新既往史");
        input.setPhysicalExamination("新体格检查");
        input.setDiagnosis("新诊断");
        input.setTreatment("新治疗意见");

        when(medicalRecordRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(registrationRepository.findById(100L)).thenReturn(Optional.of(reg));
        when(medicalRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MedicalRecord result = medicalRecordService.saveRecord(input, 10L);
        assertEquals("新主诉", result.getChiefComplaint());
        assertEquals("新现病史", result.getPresentIllness());
        assertEquals("新既往史", result.getPastHistory());
        assertEquals("新体格检查", result.getPhysicalExamination());
        assertEquals("新诊断", result.getDiagnosis());
        assertEquals("新治疗意见", result.getTreatment());
    }

    @Test
    void saveRecord_ShouldThrow_WhenRecordNotFound_ById() {
        MedicalRecord input = new MedicalRecord();
        input.setId(99L);
        input.setChiefComplaint("新主诉");

        when(medicalRecordRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> medicalRecordService.saveRecord(input, 10L));
    }

    @Test
    void saveRecord_ShouldUpdateFromRegistration_WhenRecordExistsForSameRegistration() {
        Registration reg = new Registration();
        reg.setId(100L);
        reg.setPatientId(1L);
        reg.setDoctorId(10L);
        reg.setDepartmentId(50L);
        reg.setStatus("就诊中");

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

        MedicalRecord result = medicalRecordService.saveRecord(input, 10L);
        assertEquals("新主诉", result.getChiefComplaint());
    }

    @Test
    void saveRecord_ShouldThrow_WhenModifyByWrongDoctor() {
        Registration reg = new Registration();
        reg.setId(100L);
        reg.setPatientId(1L);
        reg.setDoctorId(20L); // 其他医生的挂号
        reg.setStatus("就诊中");

        MedicalRecord existing = new MedicalRecord();
        existing.setId(1L);
        existing.setRegistrationId(100L);

        MedicalRecord input = new MedicalRecord();
        input.setId(1L);
        input.setChiefComplaint("新主诉");

        when(medicalRecordRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(registrationRepository.findById(100L)).thenReturn(Optional.of(reg));
        assertThrows(BusinessException.class, () -> medicalRecordService.saveRecord(input, 10L));
    }

    @Test
    void saveRecord_ShouldThrow_WhenModifyWrongStatus() {
        Registration reg = new Registration();
        reg.setId(100L);
        reg.setPatientId(1L);
        reg.setDoctorId(10L);
        reg.setStatus("待就诊"); // 未开始看诊

        MedicalRecord existing = new MedicalRecord();
        existing.setId(1L);
        existing.setRegistrationId(100L);

        MedicalRecord input = new MedicalRecord();
        input.setId(1L);

        when(medicalRecordRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(registrationRepository.findById(100L)).thenReturn(Optional.of(reg));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> medicalRecordService.saveRecord(input, 10L));
        assertTrue(ex.getMessage().contains("请先开始看诊"));
    }

    // ========== 查询 ==========

    @Test
    void getDetail_ShouldReturnRecord() {
        Registration reg = new Registration();
        reg.setId(100L);
        reg.setPatientId(1L);
        reg.setDoctorId(10L);

        MedicalRecord record = new MedicalRecord();
        record.setId(1L);
        record.setRegistrationId(100L);

        when(medicalRecordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(registrationRepository.findById(100L)).thenReturn(Optional.of(reg));

        assertEquals(1L, medicalRecordService.getDetail(1L, 1L, "patient").getId());
    }

    @Test
    void getDetail_ShouldThrow_WhenNotFound() {
        when(medicalRecordRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class,
                () -> medicalRecordService.getDetail(99L, 1L, "patient"));
    }

    @Test
    void getDetail_ShouldThrow_WhenUnauthorizedRole() {
        Registration reg = new Registration();
        reg.setId(100L);
        reg.setPatientId(1L);
        reg.setDoctorId(10L);

        MedicalRecord record = new MedicalRecord();
        record.setId(1L);
        record.setRegistrationId(100L);

        when(medicalRecordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(registrationRepository.findById(100L)).thenReturn(Optional.of(reg));
        assertThrows(BusinessException.class,
                () -> medicalRecordService.getDetail(1L, 1L, "pharmacy"));
    }

    @Test
    void getByRegistrationId_ShouldReturnRecord_WhenExists() {
        Registration reg = new Registration();
        reg.setId(100L);
        reg.setPatientId(1L);
        reg.setDoctorId(10L);

        MedicalRecord record = new MedicalRecord();
        record.setRegistrationId(100L);

        when(registrationRepository.findById(100L)).thenReturn(Optional.of(reg));
        when(medicalRecordRepository.findByRegistrationId(100L)).thenReturn(Optional.of(record));

        MedicalRecord result = medicalRecordService.getByRegistrationId(100L, 1L, "patient");
        assertNotNull(result);
        assertEquals(100L, result.getRegistrationId());
    }

    @Test
    void getByRegistrationId_ShouldReturnNull_WhenNotExists() {
        Registration reg = new Registration();
        reg.setId(100L);
        reg.setPatientId(1L);
        reg.setDoctorId(10L);

        when(registrationRepository.findById(100L)).thenReturn(Optional.of(reg));
        when(medicalRecordRepository.findByRegistrationId(100L)).thenReturn(Optional.empty());

        assertNull(medicalRecordService.getByRegistrationId(100L, 1L, "patient"));
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

    // ========== 权限控制白盒测试 ==========

    @Test
    void getDetail_ShouldThrow_WhenPatientAccessOthersRecord() {
        Registration reg = new Registration();
        reg.setId(100L);
        reg.setPatientId(2L); // 其他患者

        MedicalRecord record = new MedicalRecord();
        record.setId(1L);
        record.setRegistrationId(100L);

        when(medicalRecordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(registrationRepository.findById(100L)).thenReturn(Optional.of(reg));
        assertThrows(BusinessException.class,
                () -> medicalRecordService.getDetail(1L, 1L, "patient"));
    }

    @Test
    void getDetail_ShouldThrow_WhenDoctorAccessOthersPatient() {
        Registration reg = new Registration();
        reg.setId(100L);
        reg.setPatientId(1L);
        reg.setDoctorId(20L); // 其他医生

        MedicalRecord record = new MedicalRecord();
        record.setId(1L);
        record.setRegistrationId(100L);

        when(medicalRecordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(registrationRepository.findById(100L)).thenReturn(Optional.of(reg));
        assertThrows(BusinessException.class,
                () -> medicalRecordService.getDetail(1L, 10L, "doctor"));
    }

    @Test
    void getDetail_ShouldSucceed_AsAdmin() {
        Registration reg = new Registration();
        reg.setId(100L);
        reg.setPatientId(2L); // 其他患者
        reg.setDoctorId(20L); // 其他医生

        MedicalRecord record = new MedicalRecord();
        record.setId(1L);
        record.setRegistrationId(100L);

        when(medicalRecordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(registrationRepository.findById(100L)).thenReturn(Optional.of(reg));

        assertEquals(1L, medicalRecordService.getDetail(1L, 99L, "admin").getId());
    }

    // ========== Lambda异常路径覆盖 ==========

    @Test
    void saveRecord_ShouldThrow_WhenRegistrationNotFound_NewRecord() {
        MedicalRecord input = new MedicalRecord();
        input.setRegistrationId(999L);
        input.setChiefComplaint("主诉");

        when(registrationRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> medicalRecordService.saveRecord(input, 10L));
    }

    @Test
    void saveRecord_ShouldThrow_WhenPatientNotFound() {
        Registration reg = new Registration();
        reg.setId(100L);
        reg.setPatientId(1L);
        reg.setDoctorId(10L);
        reg.setStatus("就诊中");

        MedicalRecord input = new MedicalRecord();
        input.setRegistrationId(100L);
        input.setChiefComplaint("主诉");

        when(registrationRepository.findById(100L)).thenReturn(Optional.of(reg));
        when(patientRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> medicalRecordService.saveRecord(input, 10L));
    }

    @Test
    void saveRecord_ShouldThrow_WhenDoctorNotFound() {
        Registration reg = new Registration();
        reg.setId(100L);
        reg.setPatientId(1L);
        reg.setDoctorId(10L);
        reg.setStatus("就诊中");

        Patient patient = new Patient();
        patient.setId(1L);

        MedicalRecord input = new MedicalRecord();
        input.setRegistrationId(100L);
        input.setChiefComplaint("主诉");

        when(registrationRepository.findById(100L)).thenReturn(Optional.of(reg));
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(10L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> medicalRecordService.saveRecord(input, 10L));
    }

    @Test
    void getDetail_ShouldThrow_WhenRegistrationNotFound() {
        MedicalRecord record = new MedicalRecord();
        record.setId(1L);
        record.setRegistrationId(999L); // 关联到不存在的挂号

        when(medicalRecordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(registrationRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class,
                () -> medicalRecordService.getDetail(1L, 1L, "patient"));
    }

    @Test
    void getByRegistrationId_ShouldThrow_WhenRegistrationNotFound() {
        when(registrationRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class,
                () -> medicalRecordService.getByRegistrationId(999L, 1L, "patient"));
    }
}
