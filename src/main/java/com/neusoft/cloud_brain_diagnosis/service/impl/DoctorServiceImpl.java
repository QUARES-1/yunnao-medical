package com.neusoft.cloud_brain_diagnosis.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.util.JwtUtil;
import com.neusoft.cloud_brain_diagnosis.entity.Doctor;
import com.neusoft.cloud_brain_diagnosis.repository.DoctorRepository;
import com.neusoft.cloud_brain_diagnosis.service.DoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DoctorServiceImpl implements DoctorService {
    private final DoctorRepository doctorRepository;
    private final JwtUtil jwtUtil;

    @Override
    public String login(String username, String password) {
        Doctor doctor = doctorRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("账号不存在"));
        if (!BCrypt.checkpw(password, doctor.getPassword())) {
            throw new RuntimeException("密码错误");
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
                .orElseThrow(() -> new RuntimeException("医生不存在"));
        doctor.setPassword(null);
        return doctor;
    }

    @Override
    public Doctor getDoctorInfo(Long id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("医生不存在"));
        doctor.setPassword(null);
        return doctor;
    }

    @Override
    public String addDoctor(Doctor doctor) {
        if (doctorRepository.existsByUsername(doctor.getUsername())) {
            throw new RuntimeException("该账号已存在");
        }
        String defaultPassword = doctor.getPassword() != null ? doctor.getPassword() : "123456";
        doctor.setPassword(BCrypt.hashpw(defaultPassword, BCrypt.gensalt()));
        doctorRepository.save(doctor);
        return "医生添加成功，默认密码：123456";
    }

    @Override
    public String updateDoctorInfo(Doctor doctor) {
        Doctor exist = doctorRepository.findById(doctor.getId())
                .orElseThrow(() -> new RuntimeException("医生不存在"));
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
    public String resetPassword(Long id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("医生不存在"));
        doctor.setPassword(BCrypt.hashpw("123456", BCrypt.gensalt()));
        doctorRepository.save(doctor);
        return "密码重置成功，新密码：123456";
    }

    @Override
    public String deleteDoctor(Long id) {
        doctorRepository.deleteById(id);
        return "医生删除成功";
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
        return result;
    }
}