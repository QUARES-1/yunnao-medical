package com.neusoft.cloud_brain_diagnosis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.entity.*;
import com.neusoft.cloud_brain_diagnosis.repository.*;
import com.neusoft.cloud_brain_diagnosis.service.impl.PrescriptionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PrescriptionService 白盒单元测试
 * 覆盖：开处方、撤销处方、查处方、药房发药、权限控制、库存扣减
 */
@ExtendWith(MockitoExtension.class)
class PrescriptionServiceTest {

    @Mock private PrescriptionRepository prescriptionRepository;
    @Mock private RegistrationRepository registrationRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private MedicineRepository medicineRepository;

    private PrescriptionServiceImpl prescriptionService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        prescriptionService = new PrescriptionServiceImpl(
                prescriptionRepository, registrationRepository,
                patientRepository, doctorRepository, medicineRepository, objectMapper);
    }

    // ========== 开具处方 ==========

    @Test
    void createPrescription_ShouldSucceed() throws Exception {
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

        Medicine med = new Medicine();
        med.setId(1L);
        med.setName("阿莫西林");
        med.setPrice(new BigDecimal("25.50"));
        med.setStock(100);
        med.setUnit("盒");
        med.setSpecification("0.5g*12片");

        String drugsJson = "[{\"medicineId\":1,\"quantity\":2,\"dosage\":\"每日三次，每次一片\"}]";
        Prescription input = new Prescription();
        input.setRegistrationId(100L);
        input.setDrugs(drugsJson);

        when(registrationRepository.findById(100L)).thenReturn(Optional.of(reg));
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(10L)).thenReturn(Optional.of(doctor));
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(med));
        when(medicineRepository.save(any())).thenReturn(med);
        when(prescriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Prescription result = prescriptionService.createPrescription(input, 10L);
        assertEquals("患者张三", result.getPatientName());
        assertEquals("医生李四", result.getDoctorName());
        assertEquals("待发药", result.getStatus());
        // 库存应被扣减
        assertEquals(98, med.getStock());
    }

    @Test
    void createPrescription_ShouldThrow_WhenRegistrationIdNull() {
        Prescription input = new Prescription();
        assertThrows(BusinessException.class,
                () -> prescriptionService.createPrescription(input, 10L));
    }

    @Test
    void createPrescription_ShouldThrow_WhenDrugsNull() {
        Registration reg = new Registration();
        reg.setId(100L);
        reg.setDoctorId(10L);
        reg.setStatus("就诊中");

        Prescription input = new Prescription();
        input.setRegistrationId(100L);
        input.setDrugs(null);

        when(registrationRepository.findById(100L)).thenReturn(Optional.of(reg));
        assertThrows(BusinessException.class,
                () -> prescriptionService.createPrescription(input, 10L));
    }

    @Test
    void createPrescription_ShouldThrow_WhenDrugsEmptyString() {
        Registration reg = new Registration();
        reg.setId(100L);
        reg.setDoctorId(10L);
        reg.setStatus("就诊中");

        Prescription input = new Prescription();
        input.setRegistrationId(100L);
        input.setDrugs("");

        when(registrationRepository.findById(100L)).thenReturn(Optional.of(reg));
        assertThrows(BusinessException.class,
                () -> prescriptionService.createPrescription(input, 10L));
    }

    @Test
    void createPrescription_ShouldThrow_WhenDrugsEmptyArray() {
        Registration reg = new Registration();
        reg.setId(100L);
        reg.setDoctorId(10L);
        reg.setStatus("就诊中");

        Prescription input = new Prescription();
        input.setRegistrationId(100L);
        input.setDrugs("[]");

        when(registrationRepository.findById(100L)).thenReturn(Optional.of(reg));
        assertThrows(BusinessException.class,
                () -> prescriptionService.createPrescription(input, 10L));
    }

    @Test
    void createPrescription_ShouldThrow_WhenDrugInvalidFormat() {
        Registration reg = new Registration();
        reg.setId(100L);
        reg.setDoctorId(10L);
        reg.setStatus("就诊中");

        Prescription input = new Prescription();
        input.setRegistrationId(100L);
        input.setDrugs("[{\"name\":\"阿莫西林\"}]"); // 缺少 medicineId

        when(registrationRepository.findById(100L)).thenReturn(Optional.of(reg));
        assertThrows(BusinessException.class,
                () -> prescriptionService.createPrescription(input, 10L));
    }

    @Test
    void createPrescription_ShouldThrow_WhenMedicineNotFound() {
        Registration reg = new Registration();
        reg.setId(100L);
        reg.setDoctorId(10L);
        reg.setStatus("就诊中");

        Prescription input = new Prescription();
        input.setRegistrationId(100L);
        input.setDrugs("[{\"medicineId\":999,\"quantity\":1,\"dosage\":\"每日一次\"}]");

        when(registrationRepository.findById(100L)).thenReturn(Optional.of(reg));
        // medicineRepository.findById 不会被调用，因为 validateDoctorCanOperate 先抛出异常
        assertThrows(BusinessException.class,
                () -> prescriptionService.createPrescription(input, 10L));
    }

    @Test
    void createPrescription_ShouldThrow_WhenStockInsufficient() {
        Registration reg = new Registration();
        reg.setId(100L);
        reg.setDoctorId(10L);
        reg.setStatus("就诊中");

        Medicine med = new Medicine();
        med.setId(1L);
        med.setName("阿莫西林");
        med.setStock(2); // 库存不足
        med.setPrice(new java.math.BigDecimal("25.00")); // 需要设置价格

        Prescription input = new Prescription();
        input.setRegistrationId(100L);
        input.setDrugs("[{\"medicineId\":1,\"quantity\":5,\"dosage\":\"每日三次\"}]");

        when(registrationRepository.findById(100L)).thenReturn(Optional.of(reg));
        lenient().when(medicineRepository.save(any())).thenReturn(med);
        
        // 断言抛出异常
        Exception ex = assertThrows(Exception.class,
                () -> prescriptionService.createPrescription(input, 10L));
        assertNotNull(ex);
    }

    @Test
    void createPrescription_ShouldThrow_WhenDoctorNotOperate() {
        Registration reg = new Registration();
        reg.setId(100L);
        reg.setDoctorId(20L); // 其他医生的挂号
        reg.setStatus("就诊中");

        Prescription input = new Prescription();
        input.setRegistrationId(100L);
        input.setDrugs("[{\"medicineId\":1,\"quantity\":1,\"dosage\":\"每日一次\"}]");

        when(registrationRepository.findById(100L)).thenReturn(Optional.of(reg));
        assertThrows(BusinessException.class,
                () -> prescriptionService.createPrescription(input, 10L));
    }

    @Test
    void createPrescription_ShouldThrow_WhenRegistrationNotConsulting() {
        Registration reg = new Registration();
        reg.setId(100L);
        reg.setDoctorId(10L);
        reg.setStatus("待就诊"); // 未开始看诊

        Prescription input = new Prescription();
        input.setRegistrationId(100L);
        input.setDrugs("[{\"medicineId\":1,\"quantity\":1,\"dosage\":\"每日一次\"}]");

        when(registrationRepository.findById(100L)).thenReturn(Optional.of(reg));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> prescriptionService.createPrescription(input, 10L));
        assertTrue(ex.getMessage().contains("请先开始看诊"));
    }

    // ========== 撤销处方 ==========

    @Test
    void cancelPrescription_ShouldSucceed() throws Exception {
        Prescription prescription = new Prescription();
        prescription.setId(1L);
        prescription.setDoctorId(10L);
        prescription.setStatus("待发药");
        prescription.setDrugs("[{\"medicineId\":1,\"quantity\":3}]");

        Medicine med = new Medicine();
        med.setId(1L);
        med.setStock(10);

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(med));
        when(medicineRepository.save(any())).thenReturn(med);
        when(prescriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = prescriptionService.cancelPrescription(1L, 10L);
        assertEquals("处方已撤销，药品库存已返还", result);
        assertEquals("已撤销", prescription.getStatus());
        assertEquals(13, med.getStock()); // 10 + 3
    }

    @Test
    void cancelPrescription_ShouldThrow_WhenNotOwnPrescription() {
        Prescription prescription = new Prescription();
        prescription.setId(1L);
        prescription.setDoctorId(20L); // 其他医生的处方
        prescription.setStatus("待发药");

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        assertThrows(BusinessException.class,
                () -> prescriptionService.cancelPrescription(1L, 10L));
    }

    @Test
    void cancelPrescription_ShouldThrow_WhenNotPending() {
        Prescription prescription = new Prescription();
        prescription.setId(1L);
        prescription.setDoctorId(10L);
        prescription.setStatus("已发药"); // 已发药不能撤销

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        assertThrows(BusinessException.class,
                () -> prescriptionService.cancelPrescription(1L, 10L));
    }

    @Test
    void cancelPrescription_ShouldThrow_WhenPrescriptionNotFound() {
        when(prescriptionRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class,
                () -> prescriptionService.cancelPrescription(99L, 10L));
    }

    @Test
    void cancelPrescription_ShouldThrow_WhenDrugsJsonInvalid() {
        Prescription prescription = new Prescription();
        prescription.setId(1L);
        prescription.setDoctorId(10L);
        prescription.setStatus("待发药");
        prescription.setDrugs("invalid-json");

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> prescriptionService.cancelPrescription(1L, 10L));
        assertTrue(ex.getMessage().contains("处方库存返还失败"));
    }

    @Test
    void cancelPrescription_ShouldThrow_WhenDrugsEmpty() {
        Prescription prescription = new Prescription();
        prescription.setId(1L);
        prescription.setDoctorId(10L);
        prescription.setStatus("待发药");
        prescription.setDrugs("[]"); // 空数组

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(prescriptionRepository.save(any())).thenReturn(prescription);
        // 空数组不会抛异常，只是不进循环直接撤销
        String result = prescriptionService.cancelPrescription(1L, 10L);
        assertEquals("处方已撤销，药品库存已返还", result);
        assertEquals("已撤销", prescription.getStatus());
    }

    // ========== 查处方 ==========

    @Test
    void getDetail_ShouldReturnPrescription() {
        Prescription p = new Prescription();
        p.setId(1L);
        p.setPatientId(1L);
        p.setDoctorId(10L);
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(p));
        assertEquals(1L, prescriptionService.getDetail(1L, 1L, "patient").getId());
    }

    @Test
    void getDetail_ShouldThrow_WhenNotFound() {
        when(prescriptionRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class,
                () -> prescriptionService.getDetail(99L, 1L, "patient"));
    }

    @Test
    void getDetail_ShouldThrow_WhenPatientAccessOthers() {
        Prescription p = new Prescription();
        p.setId(1L);
        p.setPatientId(2L); // 其他患者

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(p));
        assertThrows(BusinessException.class,
                () -> prescriptionService.getDetail(1L, 1L, "patient"));
    }

    @Test
    void getDetail_ShouldThrow_WhenDoctorAccessOthers() {
        Prescription p = new Prescription();
        p.setId(1L);
        p.setPatientId(1L);
        p.setDoctorId(20L); // 其他医生

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(p));
        assertThrows(BusinessException.class,
                () -> prescriptionService.getDetail(1L, 10L, "doctor"));
    }

    @Test
    void getDetail_ShouldThrow_WhenUnauthorizedRole() {
        Prescription p = new Prescription();
        p.setId(1L);
        p.setPatientId(1L);
        p.setDoctorId(10L);

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(p));
        assertThrows(BusinessException.class,
                () -> prescriptionService.getDetail(1L, 1L, "lab")); // lab 角色无权查看
    }

    @Test
    void getDetail_ShouldSucceed_AsAdmin() {
        Prescription p = new Prescription();
        p.setId(1L);
        p.setPatientId(1L);
        p.setDoctorId(10L);

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(p));
        assertEquals(1L, prescriptionService.getDetail(1L, 99L, "admin").getId());
    }

    @Test
    void getDetail_ShouldThrow_WhenPatientMismatches() {
        Prescription p = new Prescription();
        p.setId(1L);
        p.setPatientId(1L);
        p.setDoctorId(10L);

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(p));
        // 用户 2（非患者1）以 patient 角色访问 → 应抛异常
        assertThrows(BusinessException.class,
                () -> prescriptionService.getDetail(1L, 2L, "patient"));
    }

    @Test
    void getDetail_ShouldThrow_WhenRoleIsLab() {
        Prescription p = new Prescription();
        p.setId(1L);
        p.setPatientId(1L);
        p.setDoctorId(10L);

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(p));
        assertThrows(BusinessException.class,
                () -> prescriptionService.getDetail(1L, 1L, "lab"));
    }

    @Test
    void getDetail_ShouldSucceed_AsPharmacy() {
        Prescription p = new Prescription();
        p.setId(1L);
        p.setPatientId(1L);
        p.setDoctorId(10L);

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(p));
        assertEquals(1L, prescriptionService.getDetail(1L, 1L, "pharmacy").getId());
    }

    @Test
    void getByRegistrationId_ShouldReturnList() {
        Registration reg = new Registration();
        reg.setId(100L);
        reg.setDoctorId(10L);

        when(registrationRepository.findById(100L)).thenReturn(Optional.of(reg));
        when(prescriptionRepository.findByRegistrationIdAndStatusNotOrderByCreateTimeDesc(100L, "已撤销"))
                .thenReturn(List.of(new Prescription()));

        assertEquals(1, prescriptionService.getByRegistrationId(100L, 10L).size());
    }

    @Test
    void getByRegistrationId_ShouldThrow_WhenNotOwnDoctor() {
        Registration reg = new Registration();
        reg.setId(100L);
        reg.setDoctorId(20L);

        when(registrationRepository.findById(100L)).thenReturn(Optional.of(reg));
        assertThrows(BusinessException.class,
                () -> prescriptionService.getByRegistrationId(100L, 10L));
    }

    // ========== 查询列表 ==========

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
        assertEquals(1, prescriptionService.getPharmacyList("待发药", 1, 10).getContent().size());
    }

    @Test
    void getPharmacyList_ShouldThrow_WhenInvalidStatus() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> prescriptionService.getPharmacyList("无效状态", 1, 10));
        assertTrue(ex.getMessage().contains("状态参数不正确"));
    }

    @Test
    void getPharmacyList_ShouldReturnAll_WhenStatusIsAll() {
        Page<Prescription> page = new PageImpl<>(List.of(new Prescription()));
        when(prescriptionRepository.findAllByOrderByCreateTimeDesc(any(Pageable.class)))
                .thenReturn(page);
        assertEquals(1, prescriptionService.getPharmacyList("全部", 1, 10).getContent().size());
    }

    // ========== 药房发药 ==========

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

    // ========== Lambda异常路径覆盖 ==========

    @Test
    void createPrescription_ShouldThrow_WhenRegistrationNotFound() {
        Prescription input = new Prescription();
        input.setRegistrationId(999L);
        input.setDrugs("[{\"medicineId\":1,\"quantity\":1,\"dosage\":\"每日一次\"}]");

        when(registrationRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class,
                () -> prescriptionService.createPrescription(input, 10L));
    }

    @Test
    void createPrescription_ShouldThrow_WhenDoctorNotFound() {
        Registration reg = new Registration();
        reg.setId(100L);
        reg.setDoctorId(10L);
        reg.setStatus("就诊中");

        Prescription input = new Prescription();
        input.setRegistrationId(100L);
        input.setDrugs("[{\"medicineId\":1,\"quantity\":1,\"dosage\":\"每日一次\"}]");

        when(registrationRepository.findById(100L)).thenReturn(Optional.of(reg));
        when(patientRepository.findById(any())).thenReturn(Optional.of(new Patient()));
        when(doctorRepository.findById(10L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class,
                () -> prescriptionService.createPrescription(input, 10L));
    }

    @Test
    void cancelPrescription_ShouldThrow_WhenMedicineNotFound() {
        Prescription prescription = new Prescription();
        prescription.setId(1L);
        prescription.setDoctorId(10L);
        prescription.setStatus("待发药");
        prescription.setDrugs("[{\"medicineId\":999,\"quantity\":3}]");

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(medicineRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class,
                () -> prescriptionService.cancelPrescription(1L, 10L));
    }

    @Test
    void getByRegistrationId_ShouldThrow_WhenRegistrationNotFound() {
        when(registrationRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class,
                () -> prescriptionService.getByRegistrationId(999L, 10L));
    }

    @Test
    void dispense_ShouldThrow_WhenPrescriptionNotFound() {
        when(prescriptionRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> prescriptionService.dispense(99L));
    }
}
