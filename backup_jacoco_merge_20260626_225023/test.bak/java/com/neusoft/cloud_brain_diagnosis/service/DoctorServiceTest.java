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
 * DoctorService 单元测试
 * 覆盖：登录、注册、CRUD、排班
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

    @Test
    void register_ShouldReturnToken() {
        when(doctorRepository.existsByUsername("newdoc")).thenReturn(false);
        when(doctorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtUtil.generateToken(any(), eq(RoleEnum.DOCTOR.getCode()))).thenReturn("reg-token");

        String token = doctorService.register("newdoc", "123456", "新医生");
        assertNotNull(token);
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
    }

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
    void addDoctor_ShouldSucceed() {
        Doctor doc = new Doctor();
        doc.setUsername("newdoc");
        doc.setName("医生");
        doc.setPassword("mypass");

        when(doctorRepository.existsByUsername("newdoc")).thenReturn(false);
        when(doctorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = doctorService.addDoctor(doc);
        assertTrue(result.contains("医生添加成功"));
        assertEquals("mypass", doc.getPassword()); // 保留自定义密码
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
        assertEquals("123456", doc.getPassword()); // 默认密码
    }

    @Test
    void addDoctor_ShouldThrow_WhenUsernameExists() {
        Doctor doc = new Doctor();
        doc.setUsername("exists");
        when(doctorRepository.existsByUsername("exists")).thenReturn(true);
        assertThrows(BusinessException.class, () -> doctorService.addDoctor(doc));
    }

    @Test
    void updateDoctorInfo_ShouldUpdateOnlyNonNullFields() {
        Doctor existing = new Doctor();
        existing.setId(1L);
        existing.setName("旧名");
        existing.setTitle("医师");

        Doctor update = new Doctor();
        update.setId(1L);
        update.setName("新名");
        update.setTitle(null); // 不更新

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(doctorRepository.save(any())).thenReturn(existing);

        doctorService.updateDoctorInfo(update);
        assertEquals("新名", existing.getName());
        assertEquals("医师", existing.getTitle()); // 保持不变
    }

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

    @Test
    void resetPassword_ShouldResetToDefault() {
        Doctor doc = new Doctor();
        doc.setId(1L);
        doc.setPassword("old");

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doc));
        when(doctorRepository.save(any())).thenReturn(doc);

        String result = doctorService.resetPassword(1L);
        assertEquals("密码重置成功，新密码：123456", result);
        assertEquals("123456", doc.getPassword());
    }

    @Test
    void deleteDoctor_ShouldSucceed_WhenNoActiveRegistrations() {
        Doctor doc = new Doctor();
        doc.setId(1L);
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doc));
        when(registrationRepository.findByDoctorIdAndRegistrationDateOrderByCreateTimeAsc(1L, LocalDate.now()))
                .thenReturn(List.of());

        String result = doctorService.deleteDoctor(1L);
        assertEquals("医生删除成功", result);
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

        assertThrows(BusinessException.class, () -> doctorService.deleteDoctor(1L));
    }

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
}
