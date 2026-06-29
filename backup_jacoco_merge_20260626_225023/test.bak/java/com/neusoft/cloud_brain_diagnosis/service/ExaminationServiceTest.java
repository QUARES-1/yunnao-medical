package com.neusoft.cloud_brain_diagnosis.service;

import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.entity.*;
import com.neusoft.cloud_brain_diagnosis.repository.*;
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
 * ExaminationService 单元测试
 * 覆盖：开立检查、填写结果、查询检查/项目列表
 */
@ExtendWith(MockitoExtension.class)
class ExaminationServiceTest {

    @Mock private ExaminationRepository examinationRepository;
    @Mock private ExaminationItemRepository examinationItemRepository;
    @Mock private RegistrationRepository registrationRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private DoctorRepository doctorRepository;

    private ExaminationServiceImpl examinationService;

    @BeforeEach
    void setUp() {
        examinationService = new ExaminationServiceImpl(
                examinationRepository, examinationItemRepository,
                registrationRepository, patientRepository, doctorRepository);
    }

    @Test
    void createExamination_ShouldSucceed() {
        Registration reg = new Registration();
        reg.setId(100L);
        reg.setPatientId(1L);
        reg.setDoctorId(10L);
        reg.setDepartmentId(50L);

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
        when(examinationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Examination result = examinationService.createExamination(input);
        assertEquals("患者张三", result.getPatientName());
        assertEquals("医生李四", result.getDoctorName());
        assertEquals("血常规", result.getItemName());
        assertEquals("检验", result.getType());
        assertEquals("待检查", result.getStatus());
    }

    @Test
    void createExamination_ShouldThrow_WhenRegistrationIdNull() {
        Examination input = new Examination();
        assertThrows(BusinessException.class, () -> examinationService.createExamination(input));
    }

    @Test
    void createExamination_ShouldThrow_WhenItemIdNull() {
        Examination input = new Examination();
        input.setRegistrationId(100L);

        when(registrationRepository.findById(100L)).thenReturn(Optional.of(new Registration()));
        assertThrows(BusinessException.class, () -> examinationService.createExamination(input));
    }

    @Test
    void getDetail_ShouldReturnExamination() {
        Examination exam = new Examination();
        exam.setId(1L);
        when(examinationRepository.findById(1L)).thenReturn(Optional.of(exam));
        assertEquals(1L, examinationService.getDetail(1L).getId());
    }

    @Test
    void getDetail_ShouldThrow_WhenNotFound() {
        when(examinationRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> examinationService.getDetail(99L));
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
        assertEquals(1, examinationService.getLabList(1, 10).getContent().size());
    }

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
    void getItemList_ShouldReturnAll_WhenTypeNull() {
        when(examinationItemRepository.findAll()).thenReturn(List.of(new ExaminationItem()));
        assertEquals(1, examinationService.getItemList(null).size());
    }

    @Test
    void getItemList_ShouldFilterByType() {
        when(examinationItemRepository.findByType("检验")).thenReturn(List.of(new ExaminationItem()));
        assertEquals(1, examinationService.getItemList("检验").size());
    }
}
