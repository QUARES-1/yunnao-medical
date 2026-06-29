package com.neusoft.cloud_brain_diagnosis.service;

import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.entity.Doctor;
import com.neusoft.cloud_brain_diagnosis.entity.Patient;
import com.neusoft.cloud_brain_diagnosis.entity.Registration;
import com.neusoft.cloud_brain_diagnosis.repository.DoctorRepository;
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
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RegistrationService 单元测试 — 最核心业务模块
 * 覆盖：创建挂号（含完整验证链）、取消、医生接诊流程
 */
@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock private RegistrationRepository registrationRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private DoctorRepository doctorRepository;

    private RegistrationServiceImpl registrationService;

    @BeforeEach
    void setUp() {
        registrationService = new RegistrationServiceImpl(registrationRepository, patientRepository, doctorRepository);
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
        when(registrationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

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
    void completeConsultation_ShouldSucceed() {
        Registration reg = new Registration();
        reg.setId(1L);
        reg.setDoctorId(10L);
        reg.setStatus("就诊中");

        when(registrationRepository.findById(1L)).thenReturn(Optional.of(reg));
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
    void getDetail_ShouldReturnRegistration() {
        Registration reg = new Registration();
        reg.setId(1L);
        when(registrationRepository.findById(1L)).thenReturn(Optional.of(reg));

        Registration result = registrationService.getDetail(1L);
        assertEquals(1L, result.getId());
    }

    @Test
    void getDetail_ShouldThrow_WhenNotFound() {
        when(registrationRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> registrationService.getDetail(99L));
    }

    @Test
    void getDoctorTodayList_ShouldReturnList() {
        LocalDate today = LocalDate.now();
        when(registrationRepository.findByDoctorIdAndRegistrationDateOrderByCreateTimeAsc(10L, today))
                .thenReturn(List.of(new Registration()));

        List<Registration> list = registrationService.getDoctorTodayList(10L);
        assertEquals(1, list.size());
    }
}
