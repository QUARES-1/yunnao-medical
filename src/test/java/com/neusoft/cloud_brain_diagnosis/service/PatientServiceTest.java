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
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PatientService 白盒单元测试
 * 覆盖：获取信息、更新信息、绑定手机、微信登录
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
    void updatePatientInfo_ShouldAlsoUpdateIdCardAndAddress() {
        Patient existing = new Patient();
        existing.setId(1L);

        Patient update = new Patient();
        update.setId(1L);
        update.setIdCard("110101199001011234");
        update.setAddress("北京市朝阳区");

        when(patientRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(patientRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        patientService.updatePatientInfo(update);
        assertEquals("110101199001011234", existing.getIdCard());
        assertEquals("北京市朝阳区", existing.getAddress());
    }

    @Test
    void updatePatientInfo_ShouldAlsoUpdateAvatar() {
        Patient existing = new Patient();
        existing.setId(1L);
        existing.setAvatar("http://old.com/avatar.jpg");

        Patient update = new Patient();
        update.setId(1L);
        update.setAvatar("http://new.com/avatar.jpg");

        when(patientRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(patientRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        patientService.updatePatientInfo(update);
        assertEquals("http://new.com/avatar.jpg", existing.getAvatar());
    }

    @Test
    void updatePatientInfo_ShouldUpdateAllFieldsAtOnce() {
        Patient existing = new Patient();
        existing.setId(1L);
        existing.setName("旧名");

        Patient update = new Patient();
        update.setId(1L);
        update.setName("新名");
        update.setGender("男");
        update.setAge(40);
        update.setIdCard("110101199001011234");
        update.setAddress("新地址");
        update.setAvatar("http://new.com/avatar.jpg");

        when(patientRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(patientRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        patientService.updatePatientInfo(update);
        assertEquals("新名", existing.getName());
        assertEquals("男", existing.getGender());
        assertEquals(40, existing.getAge());
        assertEquals("110101199001011234", existing.getIdCard());
        assertEquals("新地址", existing.getAddress());
        assertEquals("http://new.com/avatar.jpg", existing.getAvatar());
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

    // ========== 微信登录（需要 mock 微信 API） ==========

    @Test
    void wxLogin_ShouldSucceed_ForExistingPatient() {
        // Mock 微信 API 返回 openid
        try (var mockedStatic = mockStatic(cn.hutool.http.HttpUtil.class)) {
            String wxResponse = "{\"openid\":\"test_openid_123\"}";
            mockedStatic.when(() -> cn.hutool.http.HttpUtil.get(anyString(), any(Map.class)))
                    .thenReturn(wxResponse);

            Patient existing = new Patient();
            existing.setId(1L);
            existing.setName("张三");
            existing.setOpenid("test_openid_123");
            existing.setPhone("13800138000");

            when(patientRepository.findByOpenid("test_openid_123")).thenReturn(Optional.of(existing));
            when(jwtUtil.generateToken(1L, "patient")).thenReturn("jwt_token");

            Map<String, Object> result = patientService.wxLogin("test_code");
            assertEquals("jwt_token", result.get("token"));
            assertEquals(1L, result.get("patientId"));
            assertEquals("张三", result.get("name"));
            assertEquals(false, result.get("needCompleteInfo"));
        }
    }

    @Test
    void wxLogin_ShouldCreateNewPatient_WhenOpenidNotFound() {
        try (var mockedStatic = mockStatic(cn.hutool.http.HttpUtil.class)) {
            String wxResponse = "{\"openid\":\"new_openid_456\"}";
            mockedStatic.when(() -> cn.hutool.http.HttpUtil.get(anyString(), any(Map.class)))
                    .thenReturn(wxResponse);

            when(patientRepository.findByOpenid("new_openid_456")).thenReturn(Optional.empty());
            when(patientRepository.save(any(Patient.class))).thenAnswer(inv -> {
                Patient p = inv.getArgument(0);
                p.setId(2L);
                return p;
            });
            when(jwtUtil.generateToken(2L, "patient")).thenReturn("jwt_new_token");

            Map<String, Object> result = patientService.wxLogin("new_code");
            assertEquals("jwt_new_token", result.get("token"));
            assertEquals(2L, result.get("patientId"));
            assertEquals("微信用户", result.get("name"));
            assertEquals(true, result.get("needCompleteInfo"));
        }
    }

    @Test
    void wxLogin_ShouldThrow_WhenWechatApiReturnsError() {
        try (var mockedStatic = mockStatic(cn.hutool.http.HttpUtil.class)) {
            String wxErrorResponse = "{\"errcode\":40013,\"errmsg\":\"invalid appid\"}";
            mockedStatic.when(() -> cn.hutool.http.HttpUtil.get(anyString(), any(Map.class)))
                    .thenReturn(wxErrorResponse);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> patientService.wxLogin("invalid_code"));
            assertTrue(ex.getMessage().contains("微信授权失败"));
        }
    }

    @Test
    void wxLogin_ShouldThrow_WhenHttpRequestFails() {
        try (var mockedStatic = mockStatic(cn.hutool.http.HttpUtil.class)) {
            mockedStatic.when(() -> cn.hutool.http.HttpUtil.get(anyString(), any(Map.class)))
                    .thenThrow(new RuntimeException("网络错误"));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> patientService.wxLogin("test_code"));
            assertTrue(ex.getMessage().contains("微信登录失败"));
        }
    }
}
