package com.neusoft.cloud_brain_diagnosis.service;

import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.entity.Doctor;
import com.neusoft.cloud_brain_diagnosis.entity.Patient;
import com.neusoft.cloud_brain_diagnosis.entity.Registration;
import com.neusoft.cloud_brain_diagnosis.repository.DoctorRepository;
import com.neusoft.cloud_brain_diagnosis.repository.MedicalRecordRepository;
import com.neusoft.cloud_brain_diagnosis.repository.PatientRepository;
import com.neusoft.cloud_brain_diagnosis.repository.RegistrationRepository;
import com.neusoft.cloud_brain_diagnosis.service.impl.RegistrationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RegistrationService 白盒单元测试 — 预约挂号核心业务
 * 覆盖：创建挂号（含完整验证链）、取消、医生接诊流程、挂号详情权限控制
 */
@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock private RegistrationRepository registrationRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private MedicalRecordRepository medicalRecordRepository;

    private RegistrationServiceImpl registrationService;

    @BeforeEach
    void setUp() {
        registrationService = new RegistrationServiceImpl(
                registrationRepository, patientRepository, doctorRepository, medicalRecordRepository);
    }

    // ========== 创建挂号 ==========

    @Test
    void createRegistration_ShouldSucceed() {
        Patient patient = new Patient();
        patient.setId(1L);
        patient.setName("患者张三");

        Doctor doctor = new Doctor();
        doctor.setId(10L);
        doctor.setName("医生李四");
        doctor.setDepartmentId(100L);
        doctor.setDepartmentName("内科");

        Registration input = new Registration();
        input.setPatientId(1L);
        input.setDoctorId(10L);
        input.setRegistrationDate(LocalDate.now().plusDays(1));
        input.setTimeSlot("上午");

        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(10L)).thenReturn(Optional.of(doctor));
        when(registrationRepository.findByDoctorIdAndRegistrationDateOrderByCreateTimeAsc(10L, LocalDate.now().plusDays(1)))
                .thenReturn(List.of());
        when(registrationRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        Registration result = registrationService.createRegistration(input);
        assertEquals("患者张三", result.getPatientName());
        assertEquals("医生李四", result.getDoctorName());
        assertEquals("内科", result.getDepartmentName());
        assertEquals(100L, result.getDepartmentId());
        assertEquals("待就诊", result.getStatus());
    }

    @Test
    void createRegistration_ShouldThrow_WhenPatientNotFound() {
        Registration input = new Registration();
        input.setPatientId(99L);

        when(patientRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> registrationService.createRegistration(input));
    }

    @Test
    void createRegistration_ShouldThrow_WhenDoctorNotFound() {
        Registration input = new Registration();
        input.setPatientId(1L);
        input.setDoctorId(99L);

        Patient patient = new Patient();
        patient.setId(1L);

        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> registrationService.createRegistration(input));
    }

    @Test
    void createRegistration_ShouldThrow_WhenDateIsNull() {
        Registration input = new Registration();
        input.setPatientId(1L);
        input.setDoctorId(10L);

        when(patientRepository.findById(1L)).thenReturn(Optional.of(new Patient()));
        when(doctorRepository.findById(10L)).thenReturn(Optional.of(new Doctor()));

        assertThrows(BusinessException.class, () -> registrationService.createRegistration(input));
    }

    @Test
    void createRegistration_ShouldThrow_WhenTimeSlotIsNull() {
        Registration input = new Registration();
        input.setPatientId(1L);
        input.setDoctorId(10L);
        input.setRegistrationDate(LocalDate.now().plusDays(1));

        when(patientRepository.findById(1L)).thenReturn(Optional.of(new Patient()));
        when(doctorRepository.findById(10L)).thenReturn(Optional.of(new Doctor()));

        assertThrows(BusinessException.class, () -> registrationService.createRegistration(input));
    }

    @Test
    void createRegistration_ShouldThrow_WhenDateIsPast() {
        Registration input = new Registration();
        input.setPatientId(1L);
        input.setDoctorId(10L);
        input.setRegistrationDate(LocalDate.now().minusDays(1));
        input.setTimeSlot("上午");

        when(patientRepository.findById(1L)).thenReturn(Optional.of(new Patient()));
        when(doctorRepository.findById(10L)).thenReturn(Optional.of(new Doctor()));

        assertThrows(BusinessException.class, () -> registrationService.createRegistration(input));
    }

    @Test
    void createRegistration_ShouldThrow_WhenDuplicateRegistration() {
        Patient patient = new Patient();
        patient.setId(1L);
        patient.setName("患者");

        Doctor doctor = new Doctor();
        doctor.setId(10L);
        doctor.setName("医生");
        doctor.setDepartmentId(100L);
        doctor.setDepartmentName("内科");

        Registration existing = new Registration();
        existing.setPatientId(1L);
        existing.setTimeSlot("上午");
        existing.setStatus("待就诊");

        Registration input = new Registration();
        input.setPatientId(1L);
        input.setDoctorId(10L);
        input.setRegistrationDate(LocalDate.now().plusDays(1));
        input.setTimeSlot("上午");

        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(10L)).thenReturn(Optional.of(doctor));
        when(registrationRepository.findByDoctorIdAndRegistrationDateOrderByCreateTimeAsc(10L, LocalDate.now().plusDays(1)))
                .thenReturn(List.of(existing));

        assertThrows(BusinessException.class, () -> registrationService.createRegistration(input));
    }

    @Test
    void createRegistration_ShouldAllow_WhenDuplicateIsCancelled() {
        Patient patient = new Patient();
        patient.setId(1L);
        patient.setName("患者");

        Doctor doctor = new Doctor();
        doctor.setId(10L);
        doctor.setName("医生");
        doctor.setDepartmentId(100L);
        doctor.setDepartmentName("内科");

        Registration existing = new Registration();
        existing.setPatientId(1L);
        existing.setTimeSlot("上午");
        existing.setStatus("已取消"); // 已取消的挂号不算重复

        Registration input = new Registration();
        input.setPatientId(1L);
        input.setDoctorId(10L);
        input.setRegistrationDate(LocalDate.now().plusDays(1));
        input.setTimeSlot("上午");

        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(10L)).thenReturn(Optional.of(doctor));
        when(registrationRepository.findByDoctorIdAndRegistrationDateOrderByCreateTimeAsc(10L, LocalDate.now().plusDays(1)))
                .thenReturn(List.of(existing));
        when(registrationRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        Registration result = registrationService.createRegistration(input);
        assertEquals("待就诊", result.getStatus());
    }

    // ========== 挂号详情（权限控制） ==========

    @Test
    void getDetail_ShouldReturnRegistration_AsPatient() {
        Patient patient = new Patient();
        patient.setId(1L);
        patient.setName("张三");
        patient.setGender("男");
        patient.setAge(30);
        patient.setPhone("13800138000");
        patient.setAllergyHistory("无");

        Registration reg = new Registration();
        reg.setId(1L);
        reg.setPatientId(1L);
        reg.setDoctorId(10L);

        when(registrationRepository.findById(1L)).thenReturn(Optional.of(reg));
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));

        Registration result = registrationService.getDetail(1L, 1L, "patient");
        assertEquals(1L, result.getId());
        assertEquals("男", result.getPatientGender());
        assertEquals(30, result.getPatientAge());
        assertEquals("13800138000", result.getPatientPhone());
        assertEquals("无", result.getPatientAllergyHistory());
    }

    @Test
    void getDetail_ShouldReturnRegistration_AsDoctor() {
        Patient patient = new Patient();
        patient.setId(1L);
        patient.setName("张三");
        patient.setGender("男");
        patient.setAge(30);

        Registration reg = new Registration();
        reg.setId(1L);
        reg.setPatientId(1L);
        reg.setDoctorId(10L);

        when(registrationRepository.findById(1L)).thenReturn(Optional.of(reg));
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));

        Registration result = registrationService.getDetail(1L, 10L, "doctor");
        assertEquals(1L, result.getId());
    }

    @Test
    void getDetail_ShouldReturnRegistration_AsAdmin() {
        Patient patient = new Patient();
        patient.setId(1L);
        patient.setName("张三");

        Registration reg = new Registration();
        reg.setId(1L);
        reg.setPatientId(1L);
        reg.setDoctorId(10L);

        when(registrationRepository.findById(1L)).thenReturn(Optional.of(reg));
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));

        Registration result = registrationService.getDetail(1L, 99L, "admin");
        assertEquals(1L, result.getId());
    }

    @Test
    void getDetail_ShouldThrow_WhenNotFound() {
        when(registrationRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class,
                () -> registrationService.getDetail(99L, 1L, "patient"));
    }

    @Test
    void getDetail_ShouldThrow_WhenPatientAccessOthersRecord() {
        Registration reg = new Registration();
        reg.setId(1L);
        reg.setPatientId(2L); // 其他患者

        when(registrationRepository.findById(1L)).thenReturn(Optional.of(reg));
        assertThrows(BusinessException.class,
                () -> registrationService.getDetail(1L, 1L, "patient"));
    }

    @Test
    void getDetail_ShouldThrow_WhenDoctorAccessOthersPatient() {
        Registration reg = new Registration();
        reg.setId(1L);
        reg.setPatientId(1L);
        reg.setDoctorId(20L); // 其他医生

        when(registrationRepository.findById(1L)).thenReturn(Optional.of(reg));
        assertThrows(BusinessException.class,
                () -> registrationService.getDetail(1L, 10L, "doctor"));
    }

    @Test
    void getDetail_ShouldThrow_WhenUnauthorizedRole() {
        Registration reg = new Registration();
        reg.setId(1L);
        reg.setPatientId(1L);
        reg.setDoctorId(10L);

        when(registrationRepository.findById(1L)).thenReturn(Optional.of(reg));
        assertThrows(BusinessException.class,
                () -> registrationService.getDetail(1L, 1L, "pharmacy"));
    }

    // ========== 患者-取消挂号 ==========

    @Test
    void cancelRegistration_ShouldSucceed() {
        Registration reg = new Registration();
        reg.setId(1L);
        reg.setPatientId(1L);
        reg.setStatus("待就诊");

        when(registrationRepository.findById(1L)).thenReturn(Optional.of(reg));
        when(registrationRepository.save(any())).thenReturn(reg);

        String result = registrationService.cancelRegistration(1L, 1L);
        assertEquals("取消挂号成功", result);
        assertEquals("已取消", reg.getStatus());
    }

    @Test
    void cancelRegistration_ShouldThrow_WhenNotOwnRegistration() {
        Registration reg = new Registration();
        reg.setId(1L);
        reg.setPatientId(2L); // 不同患者

        when(registrationRepository.findById(1L)).thenReturn(Optional.of(reg));
        assertThrows(BusinessException.class, () -> registrationService.cancelRegistration(1L, 1L));
    }

    @Test
    void cancelRegistration_ShouldThrow_WhenNotPendingStatus() {
        Registration reg = new Registration();
        reg.setId(1L);
        reg.setPatientId(1L);
        reg.setStatus("已就诊");

        when(registrationRepository.findById(1L)).thenReturn(Optional.of(reg));
        assertThrows(BusinessException.class, () -> registrationService.cancelRegistration(1L, 1L));
    }

    @Test
    void cancelRegistration_ShouldThrow_WhenStatusIsConsulting() {
        Registration reg = new Registration();
        reg.setId(1L);
        reg.setPatientId(1L);
        reg.setStatus("就诊中");

        when(registrationRepository.findById(1L)).thenReturn(Optional.of(reg));
        assertThrows(BusinessException.class, () -> registrationService.cancelRegistration(1L, 1L));
    }

    // ========== 医生端 ==========

    @Test
    void startConsultation_ShouldSucceed() {
        Registration reg = new Registration();
        reg.setId(1L);
        reg.setDoctorId(10L);
        reg.setStatus("待就诊");

        when(registrationRepository.findById(1L)).thenReturn(Optional.of(reg));
        when(registrationRepository.save(any())).thenReturn(reg);

        String result = registrationService.startConsultation(1L, 10L);
        assertEquals("开始看诊", result);
        assertEquals("就诊中", reg.getStatus());
    }

    @Test
    void startConsultation_ShouldThrow_WhenNotOwn() {
        Registration reg = new Registration();
        reg.setId(1L);
        reg.setDoctorId(20L); // 不同医生

        when(registrationRepository.findById(1L)).thenReturn(Optional.of(reg));
        assertThrows(BusinessException.class, () -> registrationService.startConsultation(1L, 10L));
    }

    @Test
    void startConsultation_ShouldThrow_WhenAlreadyCompleted() {
        Registration reg = new Registration();
        reg.setId(1L);
        reg.setDoctorId(10L);
        reg.setStatus("已就诊"); // 已就诊

        when(registrationRepository.findById(1L)).thenReturn(Optional.of(reg));
        assertThrows(BusinessException.class, () -> registrationService.startConsultation(1L, 10L));
    }

    @Test
    void startConsultation_ShouldThrow_WhenAlreadyCancelled() {
        Registration reg = new Registration();
        reg.setId(1L);
        reg.setDoctorId(10L);
        reg.setStatus("已取消");

        when(registrationRepository.findById(1L)).thenReturn(Optional.of(reg));
        assertThrows(BusinessException.class, () -> registrationService.startConsultation(1L, 10L));
    }

    @Test
    void completeConsultation_ShouldSucceed() {
        Registration reg = new Registration();
        reg.setId(1L);
        reg.setDoctorId(10L);
        reg.setStatus("就诊中");

        com.neusoft.cloud_brain_diagnosis.entity.MedicalRecord record =
                new com.neusoft.cloud_brain_diagnosis.entity.MedicalRecord();
        record.setChiefComplaint("头痛");
        record.setDiagnosis("感冒");
        record.setTreatment("休息三天");

        when(registrationRepository.findById(1L)).thenReturn(Optional.of(reg));
        when(medicalRecordRepository.findByRegistrationId(1L)).thenReturn(Optional.of(record));
        when(registrationRepository.save(any())).thenReturn(reg);

        String result = registrationService.completeConsultation(1L, 10L);
        assertEquals("看诊完成", result);
        assertEquals("已就诊", reg.getStatus());
    }

    @Test
    void completeConsultation_ShouldThrow_WhenNotConsulting() {
        Registration reg = new Registration();
        reg.setId(1L);
        reg.setDoctorId(10L);
        reg.setStatus("待就诊");

        when(registrationRepository.findById(1L)).thenReturn(Optional.of(reg));
        assertThrows(BusinessException.class, () -> registrationService.completeConsultation(1L, 10L));
    }

    @Test
    void completeConsultation_ShouldThrow_WhenNoMedicalRecord() {
        Registration reg = new Registration();
        reg.setId(1L);
        reg.setDoctorId(10L);
        reg.setStatus("就诊中");

        when(registrationRepository.findById(1L)).thenReturn(Optional.of(reg));
        when(medicalRecordRepository.findByRegistrationId(1L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> registrationService.completeConsultation(1L, 10L));
        assertTrue(ex.getMessage().contains("请先保存病历"));
    }

    @Test
    void completeConsultation_ShouldThrow_WhenMedicalRecordIncomplete() {
        Registration reg = new Registration();
        reg.setId(1L);
        reg.setDoctorId(10L);
        reg.setStatus("就诊中");

        com.neusoft.cloud_brain_diagnosis.entity.MedicalRecord record =
                new com.neusoft.cloud_brain_diagnosis.entity.MedicalRecord();
        record.setChiefComplaint("头痛");
        // 缺少 Diagnosis

        when(registrationRepository.findById(1L)).thenReturn(Optional.of(reg));
        when(medicalRecordRepository.findByRegistrationId(1L)).thenReturn(Optional.of(record));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> registrationService.completeConsultation(1L, 10L));
        assertTrue(ex.getMessage().contains("病历未填写完整"));
    }

    @Test
    void completeConsultation_ShouldThrow_WhenNotOwnDoctor() {
        Registration reg = new Registration();
        reg.setId(1L);
        reg.setDoctorId(20L); // 不同的医生

        when(registrationRepository.findById(1L)).thenReturn(Optional.of(reg));
        assertThrows(BusinessException.class,
                () -> registrationService.completeConsultation(1L, 10L));
    }

    @Test
    void completeConsultation_ShouldThrow_WhenStatusNotConsulting() {
        Registration reg = new Registration();
        reg.setId(1L);
        reg.setDoctorId(10L);
        reg.setStatus("待就诊"); // 非就诊中状态

        when(registrationRepository.findById(1L)).thenReturn(Optional.of(reg));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> registrationService.completeConsultation(1L, 10L));
        assertTrue(ex.getMessage().contains("当前状态不能完成看诊"));
    }

    // ========== Lambda异常路径覆盖 ==========

    @Test
    void cancelRegistration_ShouldThrow_WhenNotFound() {
        when(registrationRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class,
                () -> registrationService.cancelRegistration(99L, 1L));
    }

    @Test
    void startConsultation_ShouldThrow_WhenNotFound() {
        when(registrationRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class,
                () -> registrationService.startConsultation(99L, 10L));
    }

    @Test
    void completeConsultation_ShouldThrow_WhenNotFound() {
        when(registrationRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class,
                () -> registrationService.completeConsultation(99L, 10L));
    }

    @Test
    void getDetail_ShouldThrow_WhenPatientNotFound() {
        Registration reg = new Registration();
        reg.setId(1L);
        reg.setPatientId(1L);
        reg.setDoctorId(10L);

        when(registrationRepository.findById(1L)).thenReturn(Optional.of(reg));
        when(patientRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class,
                () -> registrationService.getDetail(1L, 10L, "doctor"));
    }

    @Test
    void getDetail_ShouldSucceed_AsAdmin() {
        Registration reg = new Registration();
        reg.setId(1L);
        reg.setPatientId(1L);
        reg.setDoctorId(10L);

        Patient patient = new Patient();
        patient.setId(1L);
        patient.setGender("女");
        patient.setAge(28);

        when(registrationRepository.findById(1L)).thenReturn(Optional.of(reg));
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));

        Registration result = registrationService.getDetail(1L, 99L, "admin");
        assertEquals(1L, result.getId());
    }

    // ========== 查询 ==========

    @Test
    void getPatientRegistrationList_ShouldReturnPage() {
        Page<Registration> page = new PageImpl<>(List.of(new Registration()));
        when(registrationRepository.findByPatientIdOrderByCreateTimeDesc(eq(1L), any(Pageable.class)))
                .thenReturn(page);

        Page<Registration> result = registrationService.getPatientRegistrationList(1L, null, 1, 10);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getPatientRegistrationList_ShouldFilterByStatus() {
        Page<Registration> page = new PageImpl<>(List.of(new Registration()));
        when(registrationRepository.findByPatientIdAndStatusOrderByCreateTimeDesc(eq(1L), eq("待就诊"), any(Pageable.class)))
                .thenReturn(page);

        Page<Registration> result = registrationService.getPatientRegistrationList(1L, "待就诊", 1, 10);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getDoctorTodayList_ShouldReturnList() {
        LocalDate today = LocalDate.now();
        when(registrationRepository.findByDoctorIdAndRegistrationDateOrderByCreateTimeAsc(10L, today))
                .thenReturn(List.of(new Registration()));

        List<Registration> list = registrationService.getDoctorTodayList(10L);
        assertEquals(1, list.size());
    }

    // ========== 医生-历史挂号列表（分页） ==========

    @Test
    void getDoctorList_ShouldReturnPage_WhenAllParamsProvided() {
        Page<Registration> page = new PageImpl<>(List.of(new Registration()));
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createTime"));
        when(registrationRepository.searchDoctorHistory(eq(10L), eq("张三"), eq("待就诊"), eq(pageRequest)))
                .thenReturn(page);

        Page<Registration> result = registrationService.getDoctorList(10L, "张三", "待就诊", 1, 10);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getDoctorList_ShouldReturnPage_WhenKeywordAndStatusAreNull() {
        Page<Registration> page = new PageImpl<>(List.of());
        PageRequest pageRequest = PageRequest.of(1, 10, Sort.by(Sort.Direction.DESC, "createTime"));
        when(registrationRepository.searchDoctorHistory(eq(10L), isNull(), isNull(), eq(pageRequest)))
                .thenReturn(page);

        Page<Registration> result = registrationService.getDoctorList(10L, null, null, 2, 10);
        assertEquals(0, result.getContent().size());
    }

    @Test
    void getDoctorList_ShouldReturnPage_WhenStatusIsEmptyString() {
        Page<Registration> page = new PageImpl<>(List.of(new Registration()));
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createTime"));
        when(registrationRepository.searchDoctorHistory(eq(10L), any(), eq(""), eq(pageRequest)))
                .thenReturn(page);

        Page<Registration> result = registrationService.getDoctorList(10L, "张三", "", 1, 10);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getDoctorList_ShouldReturnPage_WhenDoctorIdIsNull() {
        // null doctorId 不会抛 BusinessException，Spring Data 查询返回空结果
        Page<Registration> page = new PageImpl<>(List.of());
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createTime"));
        when(registrationRepository.searchDoctorHistory(isNull(), isNull(), isNull(), eq(pageRequest)))
                .thenReturn(page);

        Page<Registration> result = registrationService.getDoctorList(null, null, null, 1, 10);
        assertEquals(0, result.getContent().size());
    }
}
