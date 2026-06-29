package com.neusoft.cloud_brain_diagnosis.service.impl;

import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.common.util.JwtUtil;
import com.neusoft.cloud_brain_diagnosis.entity.Doctor;
import com.neusoft.cloud_brain_diagnosis.entity.Registration;
import com.neusoft.cloud_brain_diagnosis.repository.DoctorRepository;
import com.neusoft.cloud_brain_diagnosis.repository.RegistrationRepository;
import com.neusoft.cloud_brain_diagnosis.service.DoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DoctorServiceImpl implements DoctorService {
    private static final int SLOT_CAPACITY = 20;

    private final DoctorRepository doctorRepository;
    private final RegistrationRepository registrationRepository;
    private final JwtUtil jwtUtil;

    @Override
    public String login(String username, String password) {
        Doctor doctor = doctorRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("账号不存在"));
        // 明文比较密码
        if (!password.equals(doctor.getPassword())) {
            throw new BusinessException("密码错误");
        }
        return jwtUtil.generateToken(doctor.getId(), RoleEnum.DOCTOR.getCode());
    }

    @Override
    public List<Doctor> getDoctorList(Long departmentId) {
        List<Doctor> doctors;
        if (departmentId != null && departmentId > 0) {
            doctors = doctorRepository.findByDepartmentId(departmentId);
        } else {
            doctors = doctorRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        }
        doctors.forEach(d -> d.setPassword(null));
        return doctors;
    }

    @Override
    public Doctor getDoctorDetail(Long id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new BusinessException("医生不存在"));
        doctor.setPassword(null);
        return doctor;
    }

    @Override
    public Doctor getDoctorInfo(Long id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new BusinessException("医生不存在"));
        doctor.setPassword(null);
        return doctor;
    }

    @Override
    @Transactional
    public String addDoctor(Doctor doctor) {
        if (doctorRepository.existsByUsername(doctor.getUsername())) {
            throw new BusinessException("该账号已存在");
        }
        String defaultPassword = doctor.getPassword() != null ? doctor.getPassword() : "123456";
        doctor.setPassword(defaultPassword); // 明文存储
        doctorRepository.save(doctor);
        return "医生添加成功，默认密码：123456";
    }

    @Override
    @Transactional
    public String updateDoctorInfo(Doctor doctor) {
        Doctor exist = doctorRepository.findById(doctor.getId())
                .orElseThrow(() -> new BusinessException("医生不存在"));
        if (doctor.getName() != null) exist.setName(doctor.getName());
        if (doctor.getPhone() != null) exist.setPhone(doctor.getPhone());
        if (doctor.getTitle() != null) exist.setTitle(doctor.getTitle());
        if (doctor.getAvatar() != null) exist.setAvatar(doctor.getAvatar());
        if (doctor.getIntroduction() != null) exist.setIntroduction(doctor.getIntroduction());
        if (doctor.getSpecialty() != null) exist.setSpecialty(doctor.getSpecialty());
        if (doctor.getDepartmentId() != null) exist.setDepartmentId(doctor.getDepartmentId());
        if (doctor.getDepartmentName() != null) exist.setDepartmentName(doctor.getDepartmentName());
        doctorRepository.save(exist);
        return "信息修改成功";
    }

    @Override
    public Page<Doctor> getDoctorPage(Integer page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<Doctor> doctorPage = doctorRepository.findAll(pageRequest);
        doctorPage.getContent().forEach(d -> d.setPassword(null));
        return doctorPage;
    }

    @Override
    @Transactional
    public String resetPassword(Long id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new BusinessException("医生不存在"));
        doctor.setPassword("123456"); // 明文存储
        doctorRepository.save(doctor);
        return "密码重置成功，新密码：123456";
    }

    @Override
    @Transactional
    public String deleteDoctor(Long id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new BusinessException("医生不存在"));

        // 检查是否有未完成的挂号
        boolean hasActiveRegistrations = false;
        List<Registration> registrations = registrationRepository.findByDoctorIdAndRegistrationDateOrderByCreateTimeAsc(id, LocalDate.now());
        for (Registration reg : registrations) {
            if (!"已就诊".equals(reg.getStatus()) && !"已取消".equals(reg.getStatus())) {
                hasActiveRegistrations = true;
                break;
            }
        }
        if (hasActiveRegistrations) {
            throw new BusinessException("该医生还有未完成的挂号记录，无法删除");
        }

        doctorRepository.deleteById(id);
        return "医生删除成功";
    }

    @Override
    @Transactional
    public String register(String username, String password, String name) {
        // 1. 检查用户名是否已存在
        if (doctorRepository.existsByUsername(username)) {
            throw new BusinessException("用户名已存在");
        }

        // 2. 校验参数
        if (username == null || username.length() < 3) {
            throw new BusinessException("用户名至少3位");
        }
        if (password == null || password.length() < 6) {
            throw new BusinessException("密码至少6位");
        }

        // 3. 创建医生账号
        Doctor doctor = new Doctor();
        doctor.setUsername(username);
        doctor.setPassword(password); // 明文存储
        doctor.setName(name != null ? name : username);
        doctorRepository.save(doctor);

        // 4. 直接返回token，免去注册后再登录
        return jwtUtil.generateToken(doctor.getId(), RoleEnum.DOCTOR.getCode());
    }

    @Override
    public Map<String, Object> getSchedule(Long doctorId) {
        doctorRepository.findById(doctorId)
                .orElseThrow(() -> new BusinessException("医生不存在"));

        Map<String, Object> result = new HashMap<>();
        List<LocalDate> dates = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            dates.add(LocalDate.now().plusDays(i));
        }
        result.put("dates", dates);
        List<String> timeSlots = new ArrayList<>();
        timeSlots.add("上午");
        timeSlots.add("下午");
        result.put("timeSlots", timeSlots);

        List<Map<String, Object>> availability = new ArrayList<>();
        for (LocalDate date : dates) {
            for (String timeSlot : timeSlots) {
                long occupied = registrationRepository
                        .countByDoctorIdAndRegistrationDateAndTimeSlotAndStatusNot(
                                doctorId, date, timeSlot, "已取消");
                int remaining = Math.max(0, SLOT_CAPACITY - Math.toIntExact(occupied));

                Map<String, Object> item = new HashMap<>();
                item.put("date", date);
                item.put("timeSlot", timeSlot);
                item.put("capacity", SLOT_CAPACITY);
                item.put("remaining", remaining);
                availability.add(item);
            }
        }
        result.put("availability", availability);
        return result;
    }
}
