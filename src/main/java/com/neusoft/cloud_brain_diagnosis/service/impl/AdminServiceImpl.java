package com.neusoft.cloud_brain_diagnosis.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.util.JwtUtil;
import com.neusoft.cloud_brain_diagnosis.entity.Admin;
import com.neusoft.cloud_brain_diagnosis.repository.*;
import com.neusoft.cloud_brain_diagnosis.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {
    private final AdminRepository adminRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final RegistrationRepository registrationRepository;
    private final DepartmentRepository departmentRepository;
    private final JwtUtil jwtUtil;

    @Override
    public String login(String username, String password) {
        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("账号不存在"));
        if (!BCrypt.checkpw(password, admin.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        return jwtUtil.generateToken(admin.getId(), RoleEnum.ADMIN.getCode());
    }

    @Override
    public Admin getAdminInfo(Long id) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("管理员不存在"));
        admin.setPassword(null);
        return admin;
    }

    @Override
    public String changePassword(Long id, String oldPassword, String newPassword) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("管理员不存在"));
        if (!BCrypt.checkpw(oldPassword, admin.getPassword())) {
            throw new RuntimeException("原密码错误");
        }
        admin.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        adminRepository.save(admin);
        return "密码修改成功";
    }

    @Override
    public Map<String, Object> getOverviewStatistics() {
        Map<String, Object> result = new HashMap<>();
        result.put("patientCount", patientRepository.count());
        result.put("doctorCount", doctorRepository.count());
        result.put("departmentCount", departmentRepository.count());
        result.put("registrationCount", registrationRepository.count());
        return result;
    }
}