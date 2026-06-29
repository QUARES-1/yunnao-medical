package com.neusoft.cloud_brain_diagnosis.service.impl;

import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.common.util.JwtUtil;
import com.neusoft.cloud_brain_diagnosis.entity.Admin;
import com.neusoft.cloud_brain_diagnosis.repository.*;
import com.neusoft.cloud_brain_diagnosis.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {
    private final AdminRepository adminRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final RegistrationRepository registrationRepository;
    private final DepartmentRepository departmentRepository;
    private final JwtUtil jwtUtil;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public String login(String username, String password) {
        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("账号不存在"));
        if (!passwordMatches(password, admin.getPassword())) {
            throw new BusinessException("密码错误");
        }
        return jwtUtil.generateToken(admin.getId(), RoleEnum.ADMIN.getCode());
    }

    private boolean passwordMatches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }
        if (isBcrypt(storedPassword)) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        return rawPassword.equals(storedPassword);
    }

    private boolean isBcrypt(String password) {
        return password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$");
    }

    @Override
    public Admin getAdminInfo(Long id) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new BusinessException("管理员不存在"));
        admin.setPassword(null);
        return admin;
    }

    @Override
    public String changePassword(Long id, String oldPassword, String newPassword) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new BusinessException("管理员不存在"));
        if (!passwordMatches(oldPassword, admin.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        admin.setPassword(passwordEncoder.encode(newPassword));
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

    @Override
    public String register(String username, String password, String name) {
        if (adminRepository.findByUsername(username).isPresent()) {
            throw new BusinessException("用户名已存在");
        }
        if (username == null || username.length() < 3) {
            throw new BusinessException("用户名至少3位");
        }
        if (password == null || password.length() < 6) {
            throw new BusinessException("密码至少6位");
        }

        Admin admin = new Admin();
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setName(name != null ? name : username);
        admin.setCreateTime(LocalDateTime.now());

        adminRepository.save(admin);
        return "注册成功";
    }
}