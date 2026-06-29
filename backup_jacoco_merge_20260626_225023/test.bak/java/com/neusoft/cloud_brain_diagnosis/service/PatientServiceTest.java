package com.neusoft.cloud_brain_diagnosis.service;

import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.common.util.JwtUtil;
import com.neusoft.cloud_brain_diagnosis.config.WechatConfig;
import com.neusoft.cloud_brain_diagnosis.entity.Patient;
import com.neusoft.cloud_brain_diagnosis.repository.PatientRepository;
import com.neusoft.cloud_brain_diagnosis.service.impl.PatientServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * PatientService 单元测试
 * 覆盖：微信登录、获取信息、更新信息、绑定手机
 */
@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock private PatientRepository patientRepository;
    @Mock private WechatConfig wechatConfig;
    @Mock private JwtUtil jwtUtil;

    private PatientServiceImpl patientService;

    @BeforeEach
    void setUp() {
        patientService = new PatientServiceImpl(patientRepository, wechatConfig, jwtUtil);
    }

    // ========== 获取信息 ==========

    @Test
    void getPatientInfo_ShouldReturnPatient() {
        Patient patient = new Patient();
        patient.setId(1L);
        patient.setName("张三");
        patient.setPhone("13800138000");

        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));

        Patient result = patientService.getPatientInfo(1L);
        assertEquals("张三", result.getName());
        assertEquals("13800138000", result.getPhone());
    }

    @Test
    void getPatientInfo_ShouldThrow_WhenNotFound() {
        when(patientRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> patientService.getPatientInfo(99L));
    }

    // ========== 更新信息 ==========

    @Test
    void updatePatientInfo_ShouldUpdateNonNullFields() {
        Patient existing = new Patient();
        existing.setId(1L);
        existing.setName("旧名");

        Patient update = new Patient();
        update.setId(1L);
        update.setName("新名");
        update.setGender("男");
        update.setAge(30);

        when(patientRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(patientRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = patientService.updatePatientInfo(update);
        assertEquals("信息更新成功", result);
        assertEquals("新名", existing.getName());
        assertEquals("男", existing.getGender());
        assertEquals(30, existing.getAge());
    }

    @Test
    void updatePatientInfo_ShouldNotUpdateNullFields() {
        Patient existing = new Patient();
        existing.setId(1L);
        existing.setName("名字");
        existing.setGender("女");
        existing.setAge(25);

        Patient update = new Patient();
        update.setId(1L);
        update.setName(null);  // 不更新
        update.setGender("男");
        update.setAge(null);   // 不更新

        when(patientRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(patientRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        patientService.updatePatientInfo(update);
        assertEquals("名字", existing.getName()); // 保持不变
        assertEquals("男", existing.getGender()); // 更新
        assertEquals(25, existing.getAge());       // 保持不变
    }

    @Test
    void updatePatientInfo_ShouldThrow_WhenPatientNotFound() {
        when(patientRepository.findById(99L)).thenReturn(Optional.empty());
        Patient update = new Patient();
        update.setId(99L);
        assertThrows(BusinessException.class, () -> patientService.updatePatientInfo(update));
    }

    // ========== 绑定手机 ==========

    @Test
    void bindPhone_ShouldSucceed() {
        when(patientRepository.existsByPhone("13800138000")).thenReturn(false);
        Patient patient = new Patient();
        patient.setId(1L);
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(patientRepository.save(any())).thenReturn(patient);

        String result = patientService.bindPhone(1L, "13800138000");
        assertEquals("手机号绑定成功", result);
        assertEquals("13800138000", patient.getPhone());
    }

    @Test
    void bindPhone_ShouldThrow_WhenPhoneAlreadyBound() {
        when(patientRepository.existsByPhone("13800138000")).thenReturn(true);
        assertThrows(BusinessException.class,
                () -> patientService.bindPhone(1L, "13800138000"));
    }

    @Test
    void bindPhone_ShouldThrow_WhenPatientNotFound() {
        when(patientRepository.existsByPhone("13800138000")).thenReturn(false);
        when(patientRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class,
                () -> patientService.bindPhone(99L, "13800138000"));
    }
}
