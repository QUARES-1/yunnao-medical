package com.neusoft.cloud_brain_diagnosis.service;

import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.entity.*;
import com.neusoft.cloud_brain_diagnosis.repository.*;
import com.neusoft.cloud_brain_diagnosis.service.impl.PrescriptionServiceImpl;
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
 * PrescriptionService 单元测试
 * 覆盖：开处方、查处方、发药流程
 */
@ExtendWith(MockitoExtension.class)
class PrescriptionServiceTest {

    @Mock private PrescriptionRepository prescriptionRepository;
    @Mock private RegistrationRepository registrationRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private DoctorRepository doctorRepository;

    private PrescriptionServiceImpl prescriptionService;

    @BeforeEach
    void setUp() {
        prescriptionService = new PrescriptionServiceImpl(
                prescriptionRepository, registrationRepository, patientRepository, doctorRepository);
    }

    @Test
    void createPrescription_ShouldSucceed() {
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

        Prescription input = new Prescription();
        input.setRegistrationId(100L);
        input.setDrugs("[{\"name\":\"阿莫西林\",\"qty\":2}]");

        when(registrationRepository.findById(100L)).thenReturn(Optional.of(reg));
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(10L)).thenReturn(Optional.of(doctor));
        when(prescriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Prescription result = prescriptionService.createPrescription(input);
        assertEquals("患者张三", result.getPatientName());
        assertEquals("医生李四", result.getDoctorName());
        assertEquals("待发药", result.getStatus());
    }

    @Test
    void createPrescription_ShouldThrow_WhenRegistrationIdNull() {
        Prescription input = new Prescription();
        assertThrows(BusinessException.class, () -> prescriptionService.createPrescription(input));
    }

    @Test
    void createPrescription_ShouldThrow_WhenDrugsEmpty() {
        Registration reg = new Registration();
        reg.setId(100L);
        reg.setPatientId(1L);
        reg.setDoctorId(10L);

        Prescription input = new Prescription();
        input.setRegistrationId(100L);
        input.setDrugs(null);

        when(registrationRepository.findById(100L)).thenReturn(Optional.of(reg));
        when(patientRepository.findById(1L)).thenReturn(Optional.of(new Patient()));
        when(doctorRepository.findById(10L)).thenReturn(Optional.of(new Doctor()));

        assertThrows(BusinessException.class, () -> prescriptionService.createPrescription(input));
    }

    @Test
    void createPrescription_ShouldThrow_WhenDrugsEmptyString() {
        Registration reg = new Registration();
        reg.setId(100L);
        reg.setPatientId(1L);
        reg.setDoctorId(10L);

        Prescription input = new Prescription();
        input.setRegistrationId(100L);
        input.setDrugs("");

        when(registrationRepository.findById(100L)).thenReturn(Optional.of(reg));
        when(patientRepository.findById(1L)).thenReturn(Optional.of(new Patient()));
        when(doctorRepository.findById(10L)).thenReturn(Optional.of(new Doctor()));

        assertThrows(BusinessException.class, () -> prescriptionService.createPrescription(input));
    }

    @Test
    void getDetail_ShouldReturnPrescription() {
        Prescription p = new Prescription();
        p.setId(1L);
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(p));
        assertEquals(1L, prescriptionService.getDetail(1L).getId());
    }

    @Test
    void getDetail_ShouldThrow_WhenNotFound() {
        when(prescriptionRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> prescriptionService.getDetail(99L));
    }

    @Test
    void getPatientList_ShouldReturnPage() {
        Page<Prescription> page = new PageImpl<>(List.of(new Prescription()));
        when(prescriptionRepository.findByPatientIdOrderByCreateTimeDesc(eq(1L), any(Pageable.class)))
                .thenReturn(page);
        assertEquals(1, prescriptionService.getPatientList(1L, 1, 10).getContent().size());
    }

    @Test
    void getDoctorList_ShouldReturnPage() {
        Page<Prescription> page = new PageImpl<>(List.of(new Prescription()));
        when(prescriptionRepository.findByDoctorIdOrderByCreateTimeDesc(eq(10L), any(Pageable.class)))
                .thenReturn(page);
        assertEquals(1, prescriptionService.getDoctorList(10L, 1, 10).getContent().size());
    }

    @Test
    void getPharmacyList_ShouldReturnPendingPage() {
        Page<Prescription> page = new PageImpl<>(List.of(new Prescription()));
        when(prescriptionRepository.findByStatusOrderByCreateTimeDesc(eq("待发药"), any(Pageable.class)))
                .thenReturn(page);
        assertEquals(1, prescriptionService.getPharmacyList(1, 10).getContent().size());
    }

    @Test
    void dispense_ShouldSucceed() {
        Prescription p = new Prescription();
        p.setId(1L);
        p.setStatus("待发药");

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(p));
        when(prescriptionRepository.save(any())).thenReturn(p);

        String result = prescriptionService.dispense(1L);
        assertEquals("发药成功", result);
        assertEquals("已发药", p.getStatus());
        assertNotNull(p.getDispenseTime());
    }

    @Test
    void dispense_ShouldThrow_WhenAlreadyDispensed() {
        Prescription p = new Prescription();
        p.setId(1L);
        p.setStatus("已发药");

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(p));
        assertThrows(BusinessException.class, () -> prescriptionService.dispense(1L));
    }
}
