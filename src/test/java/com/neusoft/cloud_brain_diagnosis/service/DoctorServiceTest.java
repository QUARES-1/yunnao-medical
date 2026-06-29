package com.neusoft.cloud_brain_diagnosis.service;

import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.common.util.JwtUtil;
import com.neusoft.cloud_brain_diagnosis.entity.Doctor;
import com.neusoft.cloud_brain_diagnosis.entity.Registration;
import com.neusoft.cloud_brain_diagnosis.repository.DoctorRepository;
import com.neusoft.cloud_brain_diagnosis.repository.RegistrationRepository;
import com.neusoft.cloud_brain_diagnosis.service.impl.DoctorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DoctorService 白盒单元测试
 * 覆盖：登录、注册、CRUD、排班、密码修改、权限验证
 */
@ExtendWith(MockitoExtension.class)
class DoctorServiceTest {

    @Mock private DoctorRepository doctorRepository;
    @Mock private RegistrationRepository registrationRepository;
    @Mock private JwtUtil jwtUtil;

    private DoctorServiceImpl doctorService;

    @BeforeEach
    void setUp() {
        doctorService = new DoctorServiceImpl(doctorRepository, registrationRepository, jwtUtil);
    }

    // ========== 登录 ==========

    @Test
    void login_ShouldReturnToken() {
        Doctor doctor = new Doctor();
        doctor.setId(1L);
        doctor.setUsername("doc1");
        doctor.setPassword("123456");

        when(doctorRepository.findByUsername("doc1")).thenReturn(Optional.of(doctor));
        when(jwtUtil.generateToken(1L, RoleEnum.DOCTOR.getCode())).thenReturn("token");

        assertEquals("token", doctorService.login("doc1", "123456"));
    }

    @Test
    void login_ShouldUpgradePlainPassword_ToBcrypt() {
        Doctor doctor = new Doctor();
        doctor.setId(1L);
        doctor.setUsername("doc1");
        doctor.setPassword("plain123"); // 明文密码

        when(doctorRepository.findByUsername("doc1")).thenReturn(Optional.of(doctor));
        when(jwtUtil.generateToken(1L, RoleEnum.DOCTOR.getCode())).thenReturn("token");
        when(doctorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        doctorService.login("doc1", "plain123");
        assertTrue(doctor.getPassword().startsWith("$2"));
    }

    @Test
    void login_ShouldThrow_WhenUsernameNotFound() {
        when(doctorRepository.findByUsername("unknown")).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> doctorService.login("unknown", "pass"));
    }

    @Test
    void login_ShouldThrow_WhenPasswordWrong() {
        Doctor doctor = new Doctor();
        doctor.setUsername("doc1");
        doctor.setPassword("correct");
        when(doctorRepository.findByUsername("doc1")).thenReturn(Optional.of(doctor));
        assertThrows(BusinessException.class, () -> doctorService.login("doc1", "wrong"));
    }

    // ========== 注册 ==========

    @Test
    void register_ShouldReturnToken() {
        when(doctorRepository.existsByUsername("newdoc")).thenReturn(false);
        when(doctorRepository.save(any())).thenAnswer(inv -> {
            Doctor d = inv.getArgument(0);
            d.setId(1L);
            return d;
        });
        when(jwtUtil.generateToken(any(), eq(RoleEnum.DOCTOR.getCode()))).thenReturn("reg-token");

        String token = doctorService.register("newdoc", "123456", "新医生");
        assertNotNull(token);
    }

    @Test
    void register_ShouldSetNameAsUsername_WhenNameIsNull() {
        when(doctorRepository.existsByUsername("newdoc")).thenReturn(false);
        when(doctorRepository.save(any())).thenAnswer(inv -> {
            Doctor d = inv.getArgument(0);
            assertEquals("newdoc", d.getName());
            return d;
        });
        when(jwtUtil.generateToken(any(), eq(RoleEnum.DOCTOR.getCode()))).thenReturn("token");

        doctorService.register("newdoc", "123456", null);
    }

    @Test
    void register_ShouldThrow_WhenUsernameExists() {
        when(doctorRepository.existsByUsername("exists")).thenReturn(true);
        assertThrows(BusinessException.class,
                () -> doctorService.register("exists", "123456", "test"));
    }

    @Test
    void register_ShouldThrow_WhenUsernameTooShort() {
        assertThrows(BusinessException.class,
                () -> doctorService.register("ab", "123456", "test"));
    }

    @Test
    void register_ShouldThrow_WhenPasswordTooShort() {
        assertThrows(BusinessException.class,
                () -> doctorService.register("abc", "12345", "test"));
    }

    @Test
    void register_ShouldEncodePassword() {
        when(doctorRepository.existsByUsername("newdoc")).thenReturn(false);
        when(doctorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtUtil.generateToken(any(), eq(RoleEnum.DOCTOR.getCode()))).thenReturn("token");

        doctorService.register("newdoc", "123456", "新医生");
        verify(doctorRepository).save(argThat(d ->
            d.getPassword() != null && d.getPassword().startsWith("$2")));
    }

    // ========== 医生列表 ==========

    @Test
    void getDoctorList_ShouldReturnDoctorsWithoutPassword() {
        Doctor doc = new Doctor();
        doc.setId(1L);
        doc.setName("医生1");
        doc.setPassword("secret");

        when(doctorRepository.findAll(any(Sort.class))).thenReturn(List.of(doc));

        List<Doctor> list = doctorService.getDoctorList(null);
        assertEquals(1, list.size());
        assertNull(list.get(0).getPassword());
    }

    @Test
    void getDoctorList_ShouldFilterByDepartment() {
        when(doctorRepository.findByDepartmentId(1L)).thenReturn(List.of(new Doctor()));

        List<Doctor> list = doctorService.getDoctorList(1L);
        assertEquals(1, list.size());
        verify(doctorRepository).findByDepartmentId(1L);
    }

    @Test
    void getDoctorList_ShouldReturnAll_WhenDepartmentIdIsZeroOrNull() {
        when(doctorRepository.findAll(any(Sort.class))).thenReturn(List.of());

        List<Doctor> list1 = doctorService.getDoctorList(0L);
        List<Doctor> list2 = doctorService.getDoctorList(null);
        assertEquals(0, list1.size());
        assertEquals(0, list2.size());
    }

    // ========== 医生详情 ==========

    @Test
    void getDoctorDetail_ShouldReturnDoctorWithoutPassword() {
        Doctor doc = new Doctor();
        doc.setId(1L);
        doc.setPassword("secret");
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doc));

        Doctor result = doctorService.getDoctorDetail(1L);
        assertNull(result.getPassword());
    }

    @Test
    void getDoctorDetail_ShouldThrow_WhenNotFound() {
        when(doctorRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> doctorService.getDoctorDetail(99L));
    }

    @Test
    void getDoctorInfo_ShouldReturnDoctorWithoutPassword() {
        Doctor doc = new Doctor();
        doc.setId(1L);
        doc.setPassword("secret");
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doc));

        Doctor result = doctorService.getDoctorInfo(1L);
        assertNull(result.getPassword());
    }

    @Test
    void getDoctorInfo_ShouldThrow_WhenNotFound() {
        when(doctorRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> doctorService.getDoctorInfo(99L));
    }

    // ========== 添加医生 ==========

    @Test
    void addDoctor_ShouldSucceed() {
        Doctor doc = new Doctor();
        doc.setUsername("newdoc");
        doc.setName("医生");
        doc.setPassword("mypass");

        when(doctorRepository.existsByUsername("newdoc")).thenReturn(false);
        when(doctorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = doctorService.addDoctor(doc);
        assertTrue(result.contains("医生添加成功"));
        assertTrue(doc.getPassword().startsWith("$2")); // 密码已被 BCrypt 编码
    }

    @Test
    void addDoctor_ShouldUseDefaultPassword_WhenNull() {
        Doctor doc = new Doctor();
        doc.setUsername("newdoc");
        doc.setName("医生");
        doc.setPassword(null);

        when(doctorRepository.existsByUsername("newdoc")).thenReturn(false);
        when(doctorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        doctorService.addDoctor(doc);
        // addDoctor 内部会将 null 替换为 "123456" 然后编码
        // 所以保存后 doc.password 应该已被编码
        assertTrue(doc.getPassword().startsWith("$2"), "默认密码应被BCrypt编码");
    }

    @Test
    void addDoctor_ShouldThrow_WhenUsernameExists() {
        Doctor doc = new Doctor();
        doc.setUsername("exists");
        when(doctorRepository.existsByUsername("exists")).thenReturn(true);
        assertThrows(BusinessException.class, () -> doctorService.addDoctor(doc));
    }

    // ========== 修改信息 ==========

    @Test
    void updateDoctorInfo_ShouldUpdateOnlyNonNullFields() {
        Doctor existing = new Doctor();
        existing.setId(1L);
        existing.setName("旧名");
        existing.setTitle("医师");
        existing.setPhone("旧电话");
        existing.setIntroduction("旧介绍");

        Doctor update = new Doctor();
        update.setId(1L);
        update.setName("新名");
        update.setTitle(null); // 不更新
        update.setPhone("新电话");
        update.setIntroduction(null); // 不更新

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(doctorRepository.save(any())).thenReturn(existing);

        doctorService.updateDoctorInfo(update);
        assertEquals("新名", existing.getName());
        assertEquals("医师", existing.getTitle()); // 保持不变
        assertEquals("新电话", existing.getPhone());
        assertEquals("旧介绍", existing.getIntroduction()); // 保持不变
    }

    @Test
    void updateDoctorInfo_ShouldUpdateDepartment() {
        Doctor existing = new Doctor();
        existing.setId(1L);

        Doctor update = new Doctor();
        update.setId(1L);
        update.setDepartmentId(100L);
        update.setDepartmentName("心内科");

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(doctorRepository.save(any())).thenReturn(existing);

        doctorService.updateDoctorInfo(update);
        assertEquals(100L, existing.getDepartmentId());
        assertEquals("心内科", existing.getDepartmentName());
    }

    @Test
    void updateDoctorInfo_ShouldUpdateAllFields() {
        Doctor existing = new Doctor();
        existing.setId(1L);
        existing.setName("旧名");

        Doctor update = new Doctor();
        update.setId(1L);
        update.setTitle("主任医师");
        update.setAvatar("http://new.com/avatar.jpg");
        update.setIntroduction("新介绍");
        update.setSpecialty("心血管");
        update.setDepartmentId(100L);
        update.setDepartmentName("心内科");

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(doctorRepository.save(any())).thenReturn(existing);

        doctorService.updateDoctorInfo(update);
        assertEquals("主任医师", existing.getTitle());
        assertEquals("http://new.com/avatar.jpg", existing.getAvatar());
        assertEquals("新介绍", existing.getIntroduction());
        assertEquals("心血管", existing.getSpecialty());
        assertEquals(100L, existing.getDepartmentId());
        assertEquals("心内科", existing.getDepartmentName());
    }

    @Test
    void updateDoctorInfo_ShouldThrow_WhenDoctorNotFound() {
        Doctor update = new Doctor();
        update.setId(99L);
        when(doctorRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> doctorService.updateDoctorInfo(update));
    }

    @Test
    void changePassword_ShouldThrow_WhenDoctorNotFound() {
        when(doctorRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class,
                () -> doctorService.changePassword(99L, "old", "newpass"));
    }

    // ========== 密码修改 ==========

    @Test
    void changePassword_ShouldSucceed() {
        // 使用真实的 BCrypt 编码后的密码
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
            new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        String encodedOld = encoder.encode("oldpass");

        Doctor doctor = new Doctor();
        doctor.setId(1L);
        doctor.setPassword(encodedOld);

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(doctorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = doctorService.changePassword(1L, "oldpass", "newpass123");
        assertEquals("密码修改成功", result);
    }

    @Test
    void changePassword_ShouldThrow_WhenOldPasswordWrong() {
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
            new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        String encodedOld = encoder.encode("correctpass");

        Doctor doctor = new Doctor();
        doctor.setId(1L);
        doctor.setPassword(encodedOld);

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> doctorService.changePassword(1L, "wrongold", "newpass123"));
        assertEquals("原密码不正确", ex.getMessage());
    }

    @Test
    void changePassword_ShouldThrow_WhenNewPasswordTooShort() {
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
            new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        String encodedOld = encoder.encode("oldpass");

        Doctor doctor = new Doctor();
        doctor.setId(1L);
        doctor.setPassword(encodedOld);

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        assertThrows(BusinessException.class,
                () -> doctorService.changePassword(1L, "oldpass", "12345"));
    }

    @Test
    void changePassword_ShouldThrow_WhenNewSameAsOld() {
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
            new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        String encodedOld = encoder.encode("mypassword");

        Doctor doctor = new Doctor();
        doctor.setId(1L);
        doctor.setPassword(encodedOld);

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> doctorService.changePassword(1L, "mypassword", "mypassword"));
        assertTrue(ex.getMessage().contains("新密码不能与原密码相同"));
    }

    // ========== 分页列表 ==========

    @Test
    void getDoctorPage_ShouldReturnPage() {
        Doctor doc = new Doctor();
        doc.setId(1L);
        doc.setPassword("secret");
        Page<Doctor> page = new PageImpl<>(List.of(doc));

        when(doctorRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<Doctor> result = doctorService.getDoctorPage(1, 10);
        assertEquals(1, result.getContent().size());
        assertNull(result.getContent().get(0).getPassword());
    }

    // ========== 重置密码 ==========

    @Test
    void resetPassword_ShouldResetToDefault() {
        Doctor doc = new Doctor();
        doc.setId(1L);
        doc.setPassword("old");

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doc));
        when(doctorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = doctorService.resetPassword(1L);
        assertEquals("密码重置成功，新密码：123456", result);
        // 重置后密码被编码为 BCrypt
        assertTrue(doc.getPassword().startsWith("$2"), "重置后密码应被BCrypt编码");
    }

    @Test
    void resetPassword_ShouldThrow_WhenNotFound() {
        when(doctorRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> doctorService.resetPassword(99L));
    }

    // ========== 删除医生 ==========

    @Test
    void deleteDoctor_ShouldSucceed_WhenNoActiveRegistrations() {
        Doctor doc = new Doctor();
        doc.setId(1L);
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doc));
        when(registrationRepository.findByDoctorIdAndRegistrationDateOrderByCreateTimeAsc(1L, LocalDate.now()))
                .thenReturn(List.of());

        String result = doctorService.deleteDoctor(1L);
        assertEquals("医生删除成功", result);
        verify(doctorRepository).deleteById(1L);
    }

    @Test
    void deleteDoctor_ShouldThrow_WhenHasActiveRegistrations() {
        Doctor doc = new Doctor();
        doc.setId(1L);

        Registration reg = new Registration();
        reg.setStatus("待就诊"); // 未完成的挂号

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doc));
        when(registrationRepository.findByDoctorIdAndRegistrationDateOrderByCreateTimeAsc(1L, LocalDate.now()))
                .thenReturn(List.of(reg));

        BusinessException ex = assertThrows(BusinessException.class, () -> doctorService.deleteDoctor(1L));
        assertTrue(ex.getMessage().contains("未完成的挂号记录"));
    }

    @Test
    void deleteDoctor_ShouldThrow_WhenHasConsultingRegistrations() {
        Doctor doc = new Doctor();
        doc.setId(1L);

        Registration reg = new Registration();
        reg.setStatus("就诊中"); // 就诊中也是未完成

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doc));
        when(registrationRepository.findByDoctorIdAndRegistrationDateOrderByCreateTimeAsc(1L, LocalDate.now()))
                .thenReturn(List.of(reg));

        assertThrows(BusinessException.class, () -> doctorService.deleteDoctor(1L));
    }

    @Test
    void deleteDoctor_ShouldSucceed_WhenAllRegistrationsCompleted() {
        Doctor doc = new Doctor();
        doc.setId(1L);

        Registration reg1 = new Registration();
        reg1.setStatus("已就诊"); // 已完成

        Registration reg2 = new Registration();
        reg2.setStatus("已取消"); // 已取消

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doc));
        when(registrationRepository.findByDoctorIdAndRegistrationDateOrderByCreateTimeAsc(1L, LocalDate.now()))
                .thenReturn(List.of(reg1, reg2));

        String result = doctorService.deleteDoctor(1L);
        assertEquals("医生删除成功", result);
        verify(doctorRepository).deleteById(1L);
    }

    @Test
    void deleteDoctor_ShouldThrow_WhenNotFound() {
        when(doctorRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> doctorService.deleteDoctor(99L));
    }

    // ========== 排班 ==========

    @Test
    void getSchedule_ShouldReturn7DaysAnd2Slots() {
        Map<String, Object> schedule = doctorService.getSchedule(1L);

        @SuppressWarnings("unchecked")
        List<LocalDate> dates = (List<LocalDate>) schedule.get("dates");
        assertEquals(7, dates.size());
        assertEquals(LocalDate.now(), dates.get(0));

        @SuppressWarnings("unchecked")
        List<String> slots = (List<String>) schedule.get("timeSlots");
        assertEquals(2, slots.size());
        assertTrue(slots.contains("上午"));
        assertTrue(slots.contains("下午"));
    }

    @Test
    void getSchedule_ShouldReturnCorrectAvailability() {
        Map<String, Object> schedule = doctorService.getSchedule(1L);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> availability = (List<Map<String, Object>>) schedule.get("availability");
        assertEquals(14, availability.size()); // 7天 * 2时段

        for (Map<String, Object> slot : availability) {
            assertNotNull(slot.get("date"));
            assertNotNull(slot.get("timeSlot"));
            assertNotNull(slot.get("capacity"));
            assertNotNull(slot.get("remaining"));
        }
    }
}
