package com.neusoft.cloud_brain_diagnosis.service;

import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.entity.*;
import com.neusoft.cloud_brain_diagnosis.repository.*;
import com.neusoft.cloud_brain_diagnosis.service.ai.AiExaminationService;
import com.neusoft.cloud_brain_diagnosis.service.impl.ExaminationServiceImpl;
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
 * ExaminationService 白盒单元测试
 * 覆盖：开立检查、撤销检查、填写结果、查询检查/项目列表
 */
@ExtendWith(MockitoExtension.class)
class ExaminationServiceTest {

    @Mock private ExaminationRepository examinationRepository;
    @Mock private ExaminationItemRepository examinationItemRepository;
    @Mock private RegistrationRepository registrationRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private AiExaminationService aiExaminationService;

    private ExaminationServiceImpl examinationService;

    @BeforeEach
    void setUp() {
        examinationService = new ExaminationServiceImpl(
                examinationRepository, examinationItemRepository,
                registrationRepository, patientRepository, doctorRepository, aiExaminationService);
    }

    // ========== 开立检查 ==========

    @Test
    void createExamination_ShouldSucceed() {
        Registration reg = new Registration();
        reg.setId(100L);
        reg.setPatientId(1L);
        reg.setDoctorId(10L);
        reg.setDepartmentId(50L);
        reg.setStatus("就诊中");

        ExaminationItem item = new ExaminationItem();
        item.setId(200L);
        item.setName("血常规");
        item.setType("检验");

        Patient patient = new Patient();
        patient.setId(1L);
        patient.setName("患者张三");

        Doctor doctor = new Doctor();
        doctor.setId(10L);
        doctor.setName("医生李四");

        Examination input = new Examination();
        input.setRegistrationId(100L);
        input.setItemId(200L);

        when(registrationRepository.findById(100L)).thenReturn(Optional.of(reg));
        when(examinationItemRepository.findById(200L)).thenReturn(Optional.of(item));
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(10L)).thenReturn(Optional.of(doctor));
        when(examinationRepository.existsByRegistrationIdAndItemIdAndStatus(100L, 200L, "待检查")).thenReturn(false);
        when(examinationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Examination result = examinationService.createExamination(input, 10L);
        assertEquals("患者张三", result.getPatientName());
        assertEquals("医生李四", result.getDoctorName());
        assertEquals("血常规", result.getItemName());
        assertEquals("检验", result.getType());
        assertEquals("待检查", result.getStatus());
    }

    @Test
    void createExamination_ShouldThrow_WhenRegistrationIdNull() {
        Examination input = new Examination();
        assertThrows(BusinessException.class,
                () -> examinationService.createExamination(input, 10L));
    }

    @Test
    void createExamination_ShouldThrow_WhenItemIdNull() {
        Registration reg = new Registration();
        reg.setId(100L);
        reg.setDoctorId(10L); // 设置医生ID，否则 validateDoctorCanOperate 会抛出 NPE
        reg.setStatus("就诊中");

        Examination input = new Examination();
        input.setRegistrationId(100L);
        // itemId 为 null

        when(registrationRepository.findById(100L)).thenReturn(Optional.of(reg));
        assertThrows(BusinessException.class,
                () -> examinationService.createExamination(input, 10L));
    }

    @Test
    void createExamination_ShouldThrow_WhenDuplicateExamination() {
        Registration reg = new Registration();
        reg.setId(100L);
        reg.setDoctorId(10L);
        reg.setStatus("就诊中");

        ExaminationItem item = new ExaminationItem();
        item.setId(200L);

        Examination input = new Examination();
        input.setRegistrationId(100L);
        input.setItemId(200L);

        when(registrationRepository.findById(100L)).thenReturn(Optional.of(reg));
        when(examinationItemRepository.findById(200L)).thenReturn(Optional.of(item));
        when(examinationRepository.existsByRegistrationIdAndItemIdAndStatus(100L, 200L, "待检查")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> examinationService.createExamination(input, 10L));
        assertTrue(ex.getMessage().contains("已开立"));
    }

    @Test
    void createExamination_ShouldThrow_WhenDoctorNotOperate() {
        Registration reg = new Registration();
        reg.setId(100L);
        reg.setDoctorId(20L); // 其他医生的挂号
        reg.setStatus("就诊中");

        Examination input = new Examination();
        input.setRegistrationId(100L);
        input.setItemId(200L);

        when(registrationRepository.findById(100L)).thenReturn(Optional.of(reg));
        assertThrows(BusinessException.class,
                () -> examinationService.createExamination(input, 10L));
    }

    @Test
    void createExamination_ShouldThrow_WhenRegistrationNotConsulting() {
        Registration reg = new Registration();
        reg.setId(100L);
        reg.setDoctorId(10L);
        reg.setStatus("待就诊"); // 未开始看诊

        Examination input = new Examination();
        input.setRegistrationId(100L);
        input.setItemId(200L);

        when(registrationRepository.findById(100L)).thenReturn(Optional.of(reg));
        assertThrows(BusinessException.class,
                () -> examinationService.createExamination(input, 10L));
    }

    // ========== 撤销检查 ==========

    @Test
    void cancelExamination_ShouldSucceed() {
        Examination exam = new Examination();
        exam.setId(1L);
        exam.setDoctorId(10L);
        exam.setStatus("待检查");

        when(examinationRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(examinationRepository.save(any())).thenReturn(exam);

        String result = examinationService.cancelExamination(1L, 10L);
        assertEquals("检查项目已撤销", result);
        assertEquals("已撤销", exam.getStatus());
    }

    @Test
    void cancelExamination_ShouldThrow_WhenNotOwnDoctor() {
        Examination exam = new Examination();
        exam.setId(1L);
        exam.setDoctorId(20L); // 其他医生

        when(examinationRepository.findById(1L)).thenReturn(Optional.of(exam));
        assertThrows(BusinessException.class,
                () -> examinationService.cancelExamination(1L, 10L));
    }

    @Test
    void cancelExamination_ShouldThrow_WhenNotPending() {
        Examination exam = new Examination();
        exam.setId(1L);
        exam.setDoctorId(10L);
        exam.setStatus("已完成");

        when(examinationRepository.findById(1L)).thenReturn(Optional.of(exam));
        assertThrows(BusinessException.class,
                () -> examinationService.cancelExamination(1L, 10L));
    }

    // ========== 检查详情 ==========

    @Test
    void getDetail_ShouldReturnExamination() {
        Examination exam = new Examination();
        exam.setId(1L);
        exam.setPatientId(1L);
        exam.setDoctorId(10L);

        when(examinationRepository.findById(1L)).thenReturn(Optional.of(exam));
        assertEquals(1L, examinationService.getDetail(1L, 1L, "patient").getId());
    }

    @Test
    void getDetail_ShouldThrow_WhenNotFound() {
        when(examinationRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class,
                () -> examinationService.getDetail(99L, 1L, "patient"));
    }

    @Test
    void getDetail_ShouldThrow_WhenPatientAccessOthers() {
        Examination exam = new Examination();
        exam.setId(1L);
        exam.setPatientId(2L);

        when(examinationRepository.findById(1L)).thenReturn(Optional.of(exam));
        assertThrows(BusinessException.class,
                () -> examinationService.getDetail(1L, 1L, "patient"));
    }

    // ========== Lambda异常路径覆盖 ==========

    @Test
    void createExamination_ShouldThrow_WhenRegistrationNotFound() {
        Examination input = new Examination();
        input.setRegistrationId(999L);
        input.setItemId(200L);

        when(registrationRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class,
                () -> examinationService.createExamination(input, 10L));
    }

    @Test
    void createExamination_ShouldThrow_WhenPatientNotFound() {
        Registration reg = new Registration();
        reg.setId(100L);
        reg.setDoctorId(10L);
        reg.setStatus("就诊中");
        reg.setPatientId(null); // 显式 null，与 input 默认值一致

        ExaminationItem item = new ExaminationItem();
        item.setId(200L);

        Examination input = new Examination();
        input.setRegistrationId(100L);
        input.setItemId(200L);

        when(registrationRepository.findById(100L)).thenReturn(Optional.of(reg));
        when(examinationItemRepository.findById(200L)).thenReturn(Optional.of(item));
        when(patientRepository.findById(null)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class,
                () -> examinationService.createExamination(input, 10L));
    }

    @Test
    void createExamination_ShouldThrow_WhenDoctorNotFound() {
        Registration reg = new Registration();
        reg.setId(100L);
        reg.setDoctorId(10L);
        reg.setPatientId(1L);
        reg.setStatus("就诊中");

        ExaminationItem item = new ExaminationItem();
        item.setId(200L);

        Patient patient = new Patient();
        patient.setId(1L);

        Examination input = new Examination();
        input.setRegistrationId(100L);
        input.setItemId(200L);

        when(registrationRepository.findById(100L)).thenReturn(Optional.of(reg));
        when(examinationItemRepository.findById(200L)).thenReturn(Optional.of(item));
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(10L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class,
                () -> examinationService.createExamination(input, 10L));
    }

    @Test
    void getDetail_ShouldThrow_WhenDoctorMismatches() {
        Examination exam = new Examination();
        exam.setId(1L);
        exam.setPatientId(1L);
        exam.setDoctorId(20L); // 不同医生

        when(examinationRepository.findById(1L)).thenReturn(Optional.of(exam));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> examinationService.getDetail(1L, 10L, "doctor"));
        assertTrue(ex.getMessage().contains("无权查看其他医生患者"));
    }

    @Test
    void getDetail_ShouldThrow_WhenUnauthorizedRole() {
        Examination exam = new Examination();
        exam.setId(1L);
        exam.setPatientId(1L);
        exam.setDoctorId(10L);

        when(examinationRepository.findById(1L)).thenReturn(Optional.of(exam));
        assertThrows(BusinessException.class,
                () -> examinationService.getDetail(1L, 1L, "pharmacy"));
    }

    @Test
    void getDetail_ShouldSucceed_AsLab() {
        Examination exam = new Examination();
        exam.setId(1L);
        exam.setPatientId(1L);
        exam.setDoctorId(10L);

        when(examinationRepository.findById(1L)).thenReturn(Optional.of(exam));
        assertEquals(1L, examinationService.getDetail(1L, 99L, "lab").getId());
    }

    @Test
    void getDetail_ShouldSucceed_AsAdmin() {
        Examination exam = new Examination();
        exam.setId(1L);
        exam.setPatientId(1L);
        exam.setDoctorId(10L);

        when(examinationRepository.findById(1L)).thenReturn(Optional.of(exam));
        assertEquals(1L, examinationService.getDetail(1L, 99L, "admin").getId());
    }

    @Test
    void getPatientList_ShouldReturnPage() {
        Page<Examination> page = new PageImpl<>(List.of(new Examination()));
        when(examinationRepository.findByPatientIdOrderByCreateTimeDesc(eq(1L), any(Pageable.class)))
                .thenReturn(page);
        assertEquals(1, examinationService.getPatientList(1L, 1, 10).getContent().size());
    }

    @Test
    void getDoctorList_ShouldReturnPage() {
        Page<Examination> page = new PageImpl<>(List.of(new Examination()));
        when(examinationRepository.findByDoctorIdOrderByCreateTimeDesc(eq(10L), any(Pageable.class)))
                .thenReturn(page);
        assertEquals(1, examinationService.getDoctorList(10L, 1, 10).getContent().size());
    }

    @Test
    void getLabList_ShouldReturnPendingPage() {
        Page<Examination> page = new PageImpl<>(List.of(new Examination()));
        when(examinationRepository.findByStatusOrderByCreateTimeDesc(eq("待检查"), any(Pageable.class)))
                .thenReturn(page);
        assertEquals(1, examinationService.getLabList("待检查", 1, 10).getContent().size());
    }

    @Test
    void getLabList_ShouldReturnAll_WhenStatusIsAll() {
        Page<Examination> page = new PageImpl<>(List.of(new Examination()));
        when(examinationRepository.findAllByOrderByCreateTimeDesc(any(Pageable.class)))
                .thenReturn(page);
        assertEquals(1, examinationService.getLabList("全部", 1, 10).getContent().size());
    }

    @Test
    void getLabList_ShouldThrow_WhenInvalidStatus() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> examinationService.getLabList("无效状态", 1, 10));
        assertTrue(ex.getMessage().contains("状态参数不正确"));
    }

    // ========== 填写检查结果 ==========

    @Test
    void updateResult_ShouldSucceed() {
        Examination exam = new Examination();
        exam.setId(1L);
        exam.setStatus("待检查");

        when(examinationRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(examinationRepository.save(any())).thenReturn(exam);

        String result = examinationService.updateResult(1L, "一切正常", null);
        assertEquals("结果提交成功", result);
        assertEquals("已完成", exam.getStatus());
        assertEquals("一切正常", exam.getResult());
        assertNotNull(exam.getCompleteTime());
    }

    @Test
    void updateResult_ShouldThrow_WhenNotPending() {
        Examination exam = new Examination();
        exam.setId(1L);
        exam.setStatus("已完成");

        when(examinationRepository.findById(1L)).thenReturn(Optional.of(exam));
        assertThrows(BusinessException.class,
                () -> examinationService.updateResult(1L, "结果", null));
    }

    @Test
    void updateResult_ShouldThrow_WhenResultEmpty() {
        Examination exam = new Examination();
        exam.setId(1L);
        exam.setStatus("待检查");

        when(examinationRepository.findById(1L)).thenReturn(Optional.of(exam));
        assertThrows(BusinessException.class,
                () -> examinationService.updateResult(1L, null, null));
    }

    @Test
    void updateResult_ShouldThrow_WhenResultBlank() {
        Examination exam = new Examination();
        exam.setId(1L);
        exam.setStatus("待检查");

        when(examinationRepository.findById(1L)).thenReturn(Optional.of(exam));
        assertThrows(BusinessException.class,
                () -> examinationService.updateResult(1L, "   ", null));
    }

    // ========== 检查项目列表 ==========

    @Test
    void getItemList_ShouldReturnAll_WhenTypeNull() {
        when(examinationItemRepository.findAll()).thenReturn(List.of(new ExaminationItem()));
        assertEquals(1, examinationService.getItemList(null).size());
    }

    @Test
    void getItemList_ShouldReturnAll_WhenTypeBlank() {
        when(examinationItemRepository.findAll()).thenReturn(List.of(new ExaminationItem()));
        assertEquals(1, examinationService.getItemList("").size());
    }

    @Test
    void getItemList_ShouldFilterByType() {
        when(examinationItemRepository.findByType("检验")).thenReturn(List.of(new ExaminationItem()));
        assertEquals(1, examinationService.getItemList("检验").size());
    }

    // ========== 查处方列表 ==========

    @Test
    void getByRegistrationId_ShouldReturnList() {
        Registration reg = new Registration();
        reg.setId(100L);
        reg.setDoctorId(10L);

        when(registrationRepository.findById(100L)).thenReturn(Optional.of(reg));
        when(examinationRepository.findByRegistrationIdOrderByCreateTimeDesc(100L))
                .thenReturn(List.of(new Examination()));

        assertEquals(1, examinationService.getByRegistrationId(100L, 10L).size());
    }

    @Test
    void getByRegistrationId_ShouldThrow_WhenNotOwnDoctor() {
        Registration reg = new Registration();
        reg.setId(100L);
        reg.setDoctorId(20L);

        when(registrationRepository.findById(100L)).thenReturn(Optional.of(reg));
        assertThrows(BusinessException.class,
                () -> examinationService.getByRegistrationId(100L, 10L));
    }
}
