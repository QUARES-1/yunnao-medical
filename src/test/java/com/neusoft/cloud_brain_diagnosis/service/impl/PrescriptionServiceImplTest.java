package com.neusoft.cloud_brain_diagnosis.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.entity.Doctor;
import com.neusoft.cloud_brain_diagnosis.entity.Medicine;
import com.neusoft.cloud_brain_diagnosis.entity.Patient;
import com.neusoft.cloud_brain_diagnosis.entity.Prescription;
import com.neusoft.cloud_brain_diagnosis.entity.Registration;
import com.neusoft.cloud_brain_diagnosis.repository.DoctorRepository;
import com.neusoft.cloud_brain_diagnosis.repository.MedicineRepository;
import com.neusoft.cloud_brain_diagnosis.repository.PatientRepository;
import com.neusoft.cloud_brain_diagnosis.repository.PrescriptionRepository;
import com.neusoft.cloud_brain_diagnosis.repository.RegistrationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrescriptionServiceImplTest {

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private MedicineRepository medicineRepository;

    private PrescriptionServiceImpl prescriptionService;

    private Registration createRegistration(Long id, Long patientId, Long doctorId, String status) {
        Registration registration = new Registration();
        registration.setId(id);
        registration.setPatientId(patientId);
        registration.setDoctorId(doctorId);
        registration.setStatus(status);
        registration.setDepartmentId(1L);
        return registration;
    }

    private Patient createPatient(Long id, String name) {
        Patient patient = new Patient();
        patient.setId(id);
        patient.setName(name);
        return patient;
    }

    private Doctor createDoctor(Long id, String name) {
        Doctor doctor = new Doctor();
        doctor.setId(id);
        doctor.setName(name);
        return doctor;
    }

    private Medicine createMedicine(Long id, String name, BigDecimal price, Integer stock) {
        Medicine medicine = new Medicine();
        medicine.setId(id);
        medicine.setName(name);
        medicine.setPrice(price);
        medicine.setStock(stock);
        medicine.setUnit("盒");
        return medicine;
    }

    private Prescription createPrescription(Long id, Long registrationId, Long patientId, Long doctorId, String status) {
        Prescription prescription = new Prescription();
        prescription.setId(id);
        prescription.setRegistrationId(registrationId);
        prescription.setPatientId(patientId);
        prescription.setDoctorId(doctorId);
        prescription.setStatus(status);
        prescription.setDrugs("[{\"medicineId\":1,\"quantity\":2}]");
        prescription.setTotalAmount(BigDecimal.valueOf(100));
        return prescription;
    }

    @BeforeEach
    void setUp() {
        prescriptionService = new PrescriptionServiceImpl(
                prescriptionRepository,
                registrationRepository,
                patientRepository,
                doctorRepository,
                medicineRepository,
                new ObjectMapper()
        );
        lenient().when(patientRepository.findById(200L))
                .thenReturn(Optional.of(createPatient(200L, "张三")));
        lenient().when(doctorRepository.findById(100L))
                .thenReturn(Optional.of(createDoctor(100L, "李医生")));
    }

    // ==================== createPrescription ====================

    @Test
    void createPrescription_ShouldCreateSuccessfully_WithValidData() throws Exception {
        Long registrationId = 1L;
        Long doctorId = 100L;
        Long patientId = 200L;
        Long medicineId = 1L;

        Registration registration = createRegistration(registrationId, patientId, doctorId, "就诊中");
        Patient patient = createPatient(patientId, "张三");
        Doctor doctor = createDoctor(doctorId, "李医生");
        Medicine medicine = createMedicine(medicineId, "阿莫西林", BigDecimal.valueOf(50), 100);

        Prescription prescription = new Prescription();
        prescription.setRegistrationId(registrationId);
        prescription.setDrugs("[{\"medicineId\":" + medicineId + ",\"quantity\":2}]");

        when(registrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        lenient().when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine));
        when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(invocation -> {
            Prescription saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        Prescription result = prescriptionService.createPrescription(prescription, doctorId);

        assertNotNull(result);
        assertEquals(patientId, result.getPatientId());
        assertEquals("张三", result.getPatientName());
        assertEquals(doctorId, result.getDoctorId());
        assertEquals("李医生", result.getDoctorName());
        assertEquals("待发药", result.getStatus());
        assertEquals(BigDecimal.valueOf(100), result.getTotalAmount());
        verify(medicineRepository, times(2)).findById(medicineId);
        verify(medicineRepository).save(any(Medicine.class));
    }

    @Test
    void createPrescription_ShouldThrowException_WhenRegistrationIdIsNull() {
        Prescription prescription = new Prescription();

        BusinessException exception = assertThrows(BusinessException.class,
                () -> prescriptionService.createPrescription(prescription, 100L));

        assertEquals("挂号ID不能为空", exception.getMessage());
    }

    @Test
    void createPrescription_ShouldThrowException_WhenRegistrationNotFound() {
        Prescription prescription = new Prescription();
        prescription.setRegistrationId(999L);

        when(registrationRepository.findById(999L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> prescriptionService.createPrescription(prescription, 100L));

        assertEquals("挂号记录不存在", exception.getMessage());
    }

    @Test
    void createPrescription_ShouldThrowException_WhenPatientNotFound() {
        Long registrationId = 1L;
        Long doctorId = 100L;

        Registration registration = createRegistration(registrationId, 200L, doctorId, "就诊中");

        Prescription prescription = new Prescription();
        prescription.setRegistrationId(registrationId);
        prescription.setDrugs("[{\"medicineId\":1,\"quantity\":2}]");

        when(registrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));
        when(patientRepository.findById(200L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> prescriptionService.createPrescription(prescription, doctorId));

        assertEquals("患者不存在", exception.getMessage());
    }

    @Test
    void createPrescription_ShouldThrowException_WhenDoctorNotFound() {
        Long registrationId = 1L;
        Long doctorId = 100L;

        Registration registration = createRegistration(registrationId, 200L, doctorId, "就诊中");
        Patient patient = createPatient(200L, "张三");

        Prescription prescription = new Prescription();
        prescription.setRegistrationId(registrationId);
        prescription.setDrugs("[{\"medicineId\":1,\"quantity\":2}]");

        when(registrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));
        when(patientRepository.findById(200L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(doctorId)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> prescriptionService.createPrescription(prescription, doctorId));

        assertEquals("医生不存在", exception.getMessage());
    }

    @Test
    void createPrescription_ShouldThrowException_WhenDrugsIsNull() {
        Long registrationId = 1L;
        Long doctorId = 100L;

        Registration registration = createRegistration(registrationId, 200L, doctorId, "就诊中");

        Prescription prescription = new Prescription();
        prescription.setRegistrationId(registrationId);
        prescription.setDrugs(null);

        when(registrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> prescriptionService.createPrescription(prescription, doctorId));

        assertEquals("请添加药品", exception.getMessage());
    }

    @Test
    void createPrescription_ShouldThrowException_WhenDrugsIsEmpty() {
        Long registrationId = 1L;
        Long doctorId = 100L;

        Registration registration = createRegistration(registrationId, 200L, doctorId, "就诊中");

        Prescription prescription = new Prescription();
        prescription.setRegistrationId(registrationId);
        prescription.setDrugs("[]");

        when(registrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> prescriptionService.createPrescription(prescription, doctorId));

        assertEquals("请添加药品", exception.getMessage());
    }

    @Test
    void createPrescription_ShouldThrowException_WhenMedicineNotFound() {
        Long registrationId = 1L;
        Long doctorId = 100L;
        Long medicineId = 999L;

        Registration registration = createRegistration(registrationId, 200L, doctorId, "就诊中");

        Prescription prescription = new Prescription();
        prescription.setRegistrationId(registrationId);
        prescription.setDrugs("[{\"medicineId\":" + medicineId + ",\"quantity\":2}]");

        when(registrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));
        when(medicineRepository.findById(medicineId)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> prescriptionService.createPrescription(prescription, doctorId));

        assertEquals("药品不存在，药品ID：" + medicineId, exception.getMessage());
    }

    @Test
    void createPrescription_ShouldThrowException_WhenMedicineStockInsufficient() {
        Long registrationId = 1L;
        Long doctorId = 100L;
        Long medicineId = 1L;

        Registration registration = createRegistration(registrationId, 200L, doctorId, "就诊中");
        Medicine medicine = createMedicine(medicineId, "阿莫西林", BigDecimal.valueOf(50), 1);

        Prescription prescription = new Prescription();
        prescription.setRegistrationId(registrationId);
        prescription.setDrugs("[{\"medicineId\":" + medicineId + ",\"quantity\":5}]");

        when(registrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));
        lenient().when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> prescriptionService.createPrescription(prescription, doctorId));

        assertEquals("药品库存不足：阿莫西林", exception.getMessage());
    }

    @Test
    void createPrescription_ShouldThrowException_WhenNoPermission() {
        Long registrationId = 1L;
        Long doctorId = 100L;
        Long otherDoctorId = 200L;

        Registration registration = createRegistration(registrationId, 300L, otherDoctorId, "就诊中");

        Prescription prescription = new Prescription();
        prescription.setRegistrationId(registrationId);
        prescription.setDrugs("[{\"medicineId\":1,\"quantity\":2}]");

        when(registrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> prescriptionService.createPrescription(prescription, doctorId));

        assertEquals("无权操作其他医生的患者", exception.getMessage());
    }

    @Test
    void createPrescription_ShouldThrowException_WhenRegistrationNotInProgress() {
        Long registrationId = 1L;
        Long doctorId = 100L;

        Registration registration = createRegistration(registrationId, 200L, doctorId, "待就诊");

        Prescription prescription = new Prescription();
        prescription.setRegistrationId(registrationId);
        prescription.setDrugs("[{\"medicineId\":1,\"quantity\":2}]");

        when(registrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> prescriptionService.createPrescription(prescription, doctorId));

        assertEquals("请先开始看诊", exception.getMessage());
    }

    @Test
    void createPrescription_ShouldCalculateTotalAmount_WhenNotProvided() throws Exception {
        Long registrationId = 1L;
        Long doctorId = 100L;
        Long patientId = 200L;
        Long medicineId = 1L;

        Registration registration = createRegistration(registrationId, patientId, doctorId, "就诊中");
        Patient patient = createPatient(patientId, "张三");
        Doctor doctor = createDoctor(doctorId, "李医生");
        Medicine medicine = createMedicine(medicineId, "阿莫西林", BigDecimal.valueOf(30), 100);

        Prescription prescription = new Prescription();
        prescription.setRegistrationId(registrationId);
        prescription.setDrugs("[{\"medicineId\":" + medicineId + ",\"quantity\":3}]");
        prescription.setTotalAmount(null);

        when(registrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine));
        when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(invocation -> {
            Prescription saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        Prescription result = prescriptionService.createPrescription(prescription, doctorId);

        assertNotNull(result.getTotalAmount());
        assertEquals(BigDecimal.valueOf(90), result.getTotalAmount());
    }

    @Test
    void createPrescription_ShouldThrowException_WhenDrugsFormatInvalid() {
        Long registrationId = 1L;
        Long doctorId = 100L;

        Registration registration = createRegistration(registrationId, 200L, doctorId, "就诊中");

        Prescription prescription = new Prescription();
        prescription.setRegistrationId(registrationId);
        prescription.setDrugs("invalid json");

        when(registrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> prescriptionService.createPrescription(prescription, doctorId));

        assertEquals("处方药品明细解析失败", exception.getMessage());
    }

    @Test
    void createPrescription_ShouldThrowException_WhenDrugsMissingMedicineId() {
        Long registrationId = 1L;
        Long doctorId = 100L;

        Registration registration = createRegistration(registrationId, 200L, doctorId, "就诊中");

        Prescription prescription = new Prescription();
        prescription.setRegistrationId(registrationId);
        prescription.setDrugs("[{\"quantity\":2}]");

        when(registrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> prescriptionService.createPrescription(prescription, doctorId));

        assertEquals("处方药品缺少药品ID", exception.getMessage());
    }

    @Test
    void createPrescription_ShouldThrowException_WhenDrugsQuantityNotPositive() {
        Long registrationId = 1L;
        Long doctorId = 100L;
        Long medicineId = 1L;

        Registration registration = createRegistration(registrationId, 200L, doctorId, "就诊中");
        Medicine medicine = createMedicine(medicineId, "阿莫西林", BigDecimal.valueOf(50), 100);

        Prescription prescription = new Prescription();
        prescription.setRegistrationId(registrationId);
        prescription.setDrugs("[{\"medicineId\":" + medicineId + ",\"quantity\":0}]");

        when(registrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));
        lenient().when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> prescriptionService.createPrescription(prescription, doctorId));

        assertEquals("处方药品数量必须大于0，药品ID：" + medicineId, exception.getMessage());
    }

    // ==================== getDetail ====================

    @Test
    void getDetail_ShouldReturnPrescription_WhenDoctorAccessingOwnPrescription() {
        Long prescriptionId = 1L;
        Long doctorId = 100L;

        Prescription prescription = createPrescription(prescriptionId, 10L, 200L, doctorId, "待发药");

        when(prescriptionRepository.findById(prescriptionId)).thenReturn(Optional.of(prescription));

        Prescription result = prescriptionService.getDetail(prescriptionId, doctorId, "doctor");

        assertNotNull(result);
        assertEquals(prescriptionId, result.getId());
    }

    @Test
    void getDetail_ShouldReturnPrescription_WhenPatientAccessingOwnPrescription() {
        Long prescriptionId = 1L;
        Long patientId = 200L;

        Prescription prescription = createPrescription(prescriptionId, 10L, patientId, 100L, "待发药");

        when(prescriptionRepository.findById(prescriptionId)).thenReturn(Optional.of(prescription));

        Prescription result = prescriptionService.getDetail(prescriptionId, patientId, "patient");

        assertNotNull(result);
        assertEquals(prescriptionId, result.getId());
    }

    @Test
    void getDetail_ShouldReturnPrescription_WhenAdminAccessing() {
        Long prescriptionId = 1L;

        Prescription prescription = createPrescription(prescriptionId, 10L, 200L, 100L, "待发药");

        when(prescriptionRepository.findById(prescriptionId)).thenReturn(Optional.of(prescription));

        Prescription result = prescriptionService.getDetail(prescriptionId, 999L, "admin");

        assertNotNull(result);
        assertEquals(prescriptionId, result.getId());
    }

    @Test
    void getDetail_ShouldReturnPrescription_WhenPharmacyAccessing() {
        Long prescriptionId = 1L;

        Prescription prescription = createPrescription(prescriptionId, 10L, 200L, 100L, "待发药");

        when(prescriptionRepository.findById(prescriptionId)).thenReturn(Optional.of(prescription));

        Prescription result = prescriptionService.getDetail(prescriptionId, 999L, "pharmacy");

        assertNotNull(result);
        assertEquals(prescriptionId, result.getId());
    }

    @Test
    void getDetail_ShouldThrowException_WhenPrescriptionNotFound() {
        when(prescriptionRepository.findById(999L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> prescriptionService.getDetail(999L, 100L, "doctor"));

        assertEquals("处方不存在", exception.getMessage());
    }

    @Test
    void getDetail_ShouldThrowException_WhenPatientAccessingOtherPatientPrescription() {
        Long prescriptionId = 1L;
        Long ownerPatientId = 200L;
        Long otherPatientId = 300L;

        Prescription prescription = createPrescription(prescriptionId, 10L, ownerPatientId, 100L, "待发药");

        when(prescriptionRepository.findById(prescriptionId)).thenReturn(Optional.of(prescription));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> prescriptionService.getDetail(prescriptionId, otherPatientId, "patient"));

        assertEquals("无权查看其他患者的处方", exception.getMessage());
    }

    @Test
    void getDetail_ShouldThrowException_WhenDoctorAccessingOtherDoctorPrescription() {
        Long prescriptionId = 1L;
        Long ownerDoctorId = 100L;
        Long otherDoctorId = 200L;

        Prescription prescription = createPrescription(prescriptionId, 10L, 300L, ownerDoctorId, "待发药");

        when(prescriptionRepository.findById(prescriptionId)).thenReturn(Optional.of(prescription));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> prescriptionService.getDetail(prescriptionId, otherDoctorId, "doctor"));

        assertEquals("无权查看其他医生的处方", exception.getMessage());
    }

    @Test
    void getDetail_ShouldThrowException_WhenInvalidRoleAccessing() {
        Long prescriptionId = 1L;

        Prescription prescription = createPrescription(prescriptionId, 10L, 200L, 100L, "待发药");

        when(prescriptionRepository.findById(prescriptionId)).thenReturn(Optional.of(prescription));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> prescriptionService.getDetail(prescriptionId, 999L, "lab"));

        assertEquals("当前角色无权查看处方详情", exception.getMessage());
    }

    // ==================== cancelPrescription ====================

    @Test
    void cancelPrescription_ShouldSucceed_WhenPrescriptionIsPending() {
        Long prescriptionId = 1L;
        Long doctorId = 100L;
        Long medicineId = 1L;

        Prescription prescription = createPrescription(prescriptionId, 10L, 200L, doctorId, "待发药");
        prescription.setDrugs("[{\"medicineId\":" + medicineId + ",\"quantity\":2}]");
        Medicine medicine = createMedicine(medicineId, "阿莫西林", BigDecimal.valueOf(50), 100);

        when(prescriptionRepository.findById(prescriptionId)).thenReturn(Optional.of(prescription));
        when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine));
        when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String result = prescriptionService.cancelPrescription(prescriptionId, doctorId);

        assertEquals("处方已撤销，药品库存已返还", result);
        assertEquals("已撤销", prescription.getStatus());
        verify(medicineRepository).save(any(Medicine.class));
    }

    @Test
    void cancelPrescription_ShouldThrowException_WhenPrescriptionNotFound() {
        when(prescriptionRepository.findById(999L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> prescriptionService.cancelPrescription(999L, 100L));

        assertEquals("处方不存在", exception.getMessage());
    }

    @Test
    void cancelPrescription_ShouldThrowException_WhenNoPermissionToCancel() {
        Long prescriptionId = 1L;
        Long ownerDoctorId = 100L;
        Long otherDoctorId = 200L;

        Prescription prescription = createPrescription(prescriptionId, 10L, 300L, ownerDoctorId, "待发药");

        when(prescriptionRepository.findById(prescriptionId)).thenReturn(Optional.of(prescription));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> prescriptionService.cancelPrescription(prescriptionId, otherDoctorId));

        assertEquals("无权撤销其他医生的处方", exception.getMessage());
    }

    @Test
    void cancelPrescription_ShouldThrowException_WhenPrescriptionNotPending() {
        Long prescriptionId = 1L;
        Long doctorId = 100L;

        Prescription prescription = createPrescription(prescriptionId, 10L, 200L, doctorId, "已发药");

        when(prescriptionRepository.findById(prescriptionId)).thenReturn(Optional.of(prescription));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> prescriptionService.cancelPrescription(prescriptionId, doctorId));

        assertEquals("只有待发药处方可以撤销", exception.getMessage());
    }

    @Test
    void cancelPrescription_ShouldThrowException_WhenPrescriptionAlreadyCancelled() {
        Long prescriptionId = 1L;
        Long doctorId = 100L;

        Prescription prescription = createPrescription(prescriptionId, 10L, 200L, doctorId, "已撤销");

        when(prescriptionRepository.findById(prescriptionId)).thenReturn(Optional.of(prescription));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> prescriptionService.cancelPrescription(prescriptionId, doctorId));

        assertEquals("只有待发药处方可以撤销", exception.getMessage());
    }

    // ==================== getByRegistrationId ====================

    @Test
    void getByRegistrationId_ShouldReturnPrescriptions_WhenDoctorHasAccess() {
        Long registrationId = 10L;
        Long doctorId = 100L;

        Registration registration = createRegistration(registrationId, 200L, doctorId, "就诊中");
        Prescription prescription1 = createPrescription(1L, registrationId, 200L, doctorId, "待发药");
        Prescription prescription2 = createPrescription(2L, registrationId, 200L, doctorId, "已发药");
        List<Prescription> prescriptions = List.of(prescription1, prescription2);

        when(registrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));
        when(prescriptionRepository.findByRegistrationIdAndStatusNotOrderByCreateTimeDesc(registrationId, "已撤销"))
                .thenReturn(prescriptions);

        List<Prescription> result = prescriptionService.getByRegistrationId(registrationId, doctorId);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void getByRegistrationId_ShouldThrowException_WhenRegistrationNotFound() {
        when(registrationRepository.findById(999L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> prescriptionService.getByRegistrationId(999L, 100L));

        assertEquals("挂号记录不存在", exception.getMessage());
    }

    @Test
    void getByRegistrationId_ShouldThrowException_WhenDoctorNoPermission() {
        Long registrationId = 10L;
        Long ownerDoctorId = 100L;
        Long otherDoctorId = 200L;

        Registration registration = createRegistration(registrationId, 300L, ownerDoctorId, "就诊中");

        when(registrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> prescriptionService.getByRegistrationId(registrationId, otherDoctorId));

        assertEquals("无权查看其他医生患者的处方", exception.getMessage());
    }

    // ==================== getPatientList ====================

    @Test
    void getPatientList_ShouldReturnPageOfPrescriptions() {
        Long patientId = 200L;
        Prescription prescription1 = createPrescription(1L, 10L, patientId, 100L, "待发药");
        Prescription prescription2 = createPrescription(2L, 20L, patientId, 100L, "已发药");
        List<Prescription> prescriptions = List.of(prescription1, prescription2);
        Page<Prescription> page = new PageImpl<>(prescriptions, PageRequest.of(0, 10), 2);

        when(prescriptionRepository.findByPatientIdOrderByCreateTimeDesc(eq(patientId), any(PageRequest.class)))
                .thenReturn(page);

        Page<Prescription> result = prescriptionService.getPatientList(patientId, 1, 10);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
    }

    // ==================== getDoctorList ====================

    @Test
    void getDoctorList_ShouldReturnPageOfPrescriptions() {
        Long doctorId = 100L;
        Prescription prescription1 = createPrescription(1L, 10L, 200L, doctorId, "待发药");
        Prescription prescription2 = createPrescription(2L, 20L, 300L, doctorId, "待发药");
        List<Prescription> prescriptions = List.of(prescription1, prescription2);
        Page<Prescription> page = new PageImpl<>(prescriptions, PageRequest.of(0, 10), 2);

        when(prescriptionRepository.findByDoctorIdOrderByCreateTimeDesc(eq(doctorId), any(PageRequest.class)))
                .thenReturn(page);

        Page<Prescription> result = prescriptionService.getDoctorList(doctorId, 1, 10);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
    }

    // ==================== getPharmacyList ====================

    @Test
    void getPharmacyList_ShouldReturnAllPrescriptions_WhenStatusIsNull() {
        Prescription prescription1 = createPrescription(1L, 10L, 200L, 100L, "待发药");
        Prescription prescription2 = createPrescription(2L, 20L, 300L, 100L, "已发药");
        List<Prescription> prescriptions = List.of(prescription1, prescription2);
        Page<Prescription> page = new PageImpl<>(prescriptions, PageRequest.of(0, 10), 2);

        when(prescriptionRepository.findAllByOrderByCreateTimeDesc(any(PageRequest.class)))
                .thenReturn(page);

        Page<Prescription> result = prescriptionService.getPharmacyList(null, 1, 10);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
    }

    @Test
    void getPharmacyList_ShouldReturnAllPrescriptions_WhenStatusIsEmpty() {
        Prescription prescription1 = createPrescription(1L, 10L, 200L, 100L, "待发药");
        List<Prescription> prescriptions = List.of(prescription1);
        Page<Prescription> page = new PageImpl<>(prescriptions, PageRequest.of(0, 10), 1);

        when(prescriptionRepository.findAllByOrderByCreateTimeDesc(any(PageRequest.class)))
                .thenReturn(page);

        Page<Prescription> result = prescriptionService.getPharmacyList("", 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getPharmacyList_ShouldReturnAllPrescriptions_WhenStatusIs全部() {
        Prescription prescription1 = createPrescription(1L, 10L, 200L, 100L, "待发药");
        List<Prescription> prescriptions = List.of(prescription1);
        Page<Prescription> page = new PageImpl<>(prescriptions, PageRequest.of(0, 10), 1);

        when(prescriptionRepository.findAllByOrderByCreateTimeDesc(any(PageRequest.class)))
                .thenReturn(page);

        Page<Prescription> result = prescriptionService.getPharmacyList("全部", 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getPharmacyList_ShouldReturnFilteredPrescriptions_WhenStatusIs待发药() {
        Prescription prescription1 = createPrescription(1L, 10L, 200L, 100L, "待发药");
        List<Prescription> prescriptions = List.of(prescription1);
        Page<Prescription> page = new PageImpl<>(prescriptions, PageRequest.of(0, 10), 1);

        when(prescriptionRepository.findByStatusOrderByCreateTimeDesc(eq("待发药"), any(PageRequest.class)))
                .thenReturn(page);

        Page<Prescription> result = prescriptionService.getPharmacyList("待发药", 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("待发药", result.getContent().get(0).getStatus());
    }

    @Test
    void getPharmacyList_ShouldReturnFilteredPrescriptions_WhenStatusIs已发药() {
        Prescription prescription1 = createPrescription(1L, 10L, 200L, 100L, "已发药");
        List<Prescription> prescriptions = List.of(prescription1);
        Page<Prescription> page = new PageImpl<>(prescriptions, PageRequest.of(0, 10), 1);

        when(prescriptionRepository.findByStatusOrderByCreateTimeDesc(eq("已发药"), any(PageRequest.class)))
                .thenReturn(page);

        Page<Prescription> result = prescriptionService.getPharmacyList("已发药", 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("已发药", result.getContent().get(0).getStatus());
    }

    @Test
    void getPharmacyList_ShouldReturnFilteredPrescriptions_WhenStatusIs已撤销() {
        Prescription prescription1 = createPrescription(1L, 10L, 200L, 100L, "已撤销");
        List<Prescription> prescriptions = List.of(prescription1);
        Page<Prescription> page = new PageImpl<>(prescriptions, PageRequest.of(0, 10), 1);

        when(prescriptionRepository.findByStatusOrderByCreateTimeDesc(eq("已撤销"), any(PageRequest.class)))
                .thenReturn(page);

        Page<Prescription> result = prescriptionService.getPharmacyList("已撤销", 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("已撤销", result.getContent().get(0).getStatus());
    }

    @Test
    void getPharmacyList_ShouldThrowException_WhenStatusIsInvalid() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> prescriptionService.getPharmacyList("非法状态", 1, 10));

        assertEquals("状态参数不正确", exception.getMessage());
    }

    // ==================== dispense ====================

    @Test
    void dispense_ShouldSucceed_WhenPrescriptionIsPending() {
        Long prescriptionId = 1L;
        Long medicineId = 1L;

        Prescription prescription = createPrescription(prescriptionId, 10L, 200L, 100L, "待发药");
        prescription.setDrugs("[{\"medicineId\":" + medicineId + ",\"quantity\":2}]");
        Medicine medicine = createMedicine(medicineId, "阿莫西林", BigDecimal.valueOf(50), 100);

        when(prescriptionRepository.findById(prescriptionId)).thenReturn(Optional.of(prescription));
        when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine));
        when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String result = prescriptionService.dispense(prescriptionId);

        assertEquals("发药成功", result);
        assertEquals("已发药", prescription.getStatus());
        assertNotNull(prescription.getDispenseTime());
        verify(medicineRepository).save(any(Medicine.class));
    }

    @Test
    void dispense_ShouldThrowException_WhenPrescriptionNotFound() {
        when(prescriptionRepository.findById(999L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> prescriptionService.dispense(999L));

        assertEquals("处方不存在", exception.getMessage());
    }

    @Test
    void dispense_ShouldThrowException_WhenPrescriptionNotPending() {
        Long prescriptionId = 1L;

        Prescription prescription = createPrescription(prescriptionId, 10L, 200L, 100L, "已发药");

        when(prescriptionRepository.findById(prescriptionId)).thenReturn(Optional.of(prescription));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> prescriptionService.dispense(prescriptionId));

        assertEquals("当前状态不能发药，当前状态：已发药", exception.getMessage());
    }

    @Test
    void dispense_ShouldThrowException_WhenPrescriptionAlreadyCancelled() {
        Long prescriptionId = 1L;

        Prescription prescription = createPrescription(prescriptionId, 10L, 200L, 100L, "已撤销");

        when(prescriptionRepository.findById(prescriptionId)).thenReturn(Optional.of(prescription));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> prescriptionService.dispense(prescriptionId));

        assertEquals("当前状态不能发药，当前状态：已撤销", exception.getMessage());
    }

    @Test
    void dispense_ShouldThrowException_WhenMedicineNotFound() {
        Long prescriptionId = 1L;
        Long medicineId = 999L;

        Prescription prescription = createPrescription(prescriptionId, 10L, 200L, 100L, "待发药");
        prescription.setDrugs("[{\"medicineId\":" + medicineId + ",\"quantity\":2}]");

        when(prescriptionRepository.findById(prescriptionId)).thenReturn(Optional.of(prescription));
        when(medicineRepository.findById(medicineId)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> prescriptionService.dispense(prescriptionId));

        assertEquals("药品不存在，药品ID：" + medicineId, exception.getMessage());
    }

    @Test
    void dispense_ShouldThrowException_WhenMedicineStockInsufficient() {
        Long prescriptionId = 1L;
        Long medicineId = 1L;

        Prescription prescription = createPrescription(prescriptionId, 10L, 200L, 100L, "待发药");
        prescription.setDrugs("[{\"medicineId\":" + medicineId + ",\"quantity\":10}]");
        Medicine medicine = createMedicine(medicineId, "阿莫西林", BigDecimal.valueOf(50), 5);

        when(prescriptionRepository.findById(prescriptionId)).thenReturn(Optional.of(prescription));
        when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> prescriptionService.dispense(prescriptionId));

        assertTrue(exception.getMessage().contains("药品库存不足：阿莫西林"));
        assertTrue(exception.getMessage().contains("当前库存"));
        assertTrue(exception.getMessage().contains("需要"));
    }
}
