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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DoctorServiceImpl implements DoctorService {
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();
    private final DoctorRepository doctorRepository;
    private final RegistrationRepository registrationRepository;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public String login(String username, String password) {
        Doctor doctor = doctorRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("账号不存在"));
        String storedPassword = doctor.getPassword();
        boolean encoded = storedPassword != null && storedPassword.startsWith("$2");
        boolean matched = encoded
                ? PASSWORD_ENCODER.matches(password, storedPassword)
                : password != null && password.equals(storedPassword);
        if (!matched) {
            throw new BusinessException("密码错误");
        }
        // 兼容旧演示数据：首次成功登录后自动升级为 BCrypt。
        if (!encoded) {
            doctor.setPassword(PASSWORD_ENCODER.encode(password));
            doctorRepository.save(doctor);
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
        doctor.setPassword(PASSWORD_ENCODER.encode(defaultPassword));
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
    @Transactional
    public String changePassword(Long doctorId, String oldPassword, String newPassword) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new BusinessException("医生不存在"));
        if (!matchesPassword(oldPassword, doctor.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException("新密码至少6位");
        }
        if (matchesPassword(newPassword, doctor.getPassword())) {
            throw new BusinessException("新密码不能与原密码相同");
        }
        doctor.setPassword(PASSWORD_ENCODER.encode(newPassword));
        doctorRepository.save(doctor);
        return "密码修改成功";
    }

    private boolean matchesPassword(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) return false;
        return storedPassword.startsWith("$2")
                ? PASSWORD_ENCODER.matches(rawPassword, storedPassword)
                : rawPassword.equals(storedPassword);
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
        doctor.setPassword(PASSWORD_ENCODER.encode("123456"));
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
        doctor.setPassword(PASSWORD_ENCODER.encode(password));
        doctor.setName(name != null ? name : username);
        doctorRepository.save(doctor);

        // 4. 直接返回token，免去注册后再登录
        return jwtUtil.generateToken(doctor.getId(), RoleEnum.DOCTOR.getCode());
    }

    @Override
    public Map<String, Object> getSchedule(Long doctorId) {
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

        // 当前阶段没有独立排班表，使用稳定的每日基础号源并扣除已有预约。
        // 同一医生、日期、时段每次查询得到相同容量，便于前端展示真实余号。
        List<Map<String, Object>> availability = new ArrayList<>();
        for (LocalDate date : dates) {
            for (String timeSlot : timeSlots) {
                int capacity = 12 + Math.floorMod(
                        doctorId.intValue() + date.getDayOfMonth() + timeSlot.hashCode(), 7);
                long booked = registrationRepository.countByDoctorIdAndRegistrationDateAndTimeSlot(
                        doctorId, date, timeSlot);
                Map<String, Object> slot = new HashMap<>();
                slot.put("date", date);
                slot.put("timeSlot", timeSlot);
                slot.put("capacity", capacity);
                slot.put("remaining", Math.max(0, capacity - booked));
                availability.add(slot);
            }
        }
        result.put("availability", availability);
        return result;
    }
}
