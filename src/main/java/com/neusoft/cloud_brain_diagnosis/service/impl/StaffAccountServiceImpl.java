package com.neusoft.cloud_brain_diagnosis.service.impl;

import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.common.util.JwtUtil;
import com.neusoft.cloud_brain_diagnosis.entity.StaffAccount;
import com.neusoft.cloud_brain_diagnosis.repository.StaffAccountRepository;
import com.neusoft.cloud_brain_diagnosis.service.StaffAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class StaffAccountServiceImpl implements StaffAccountService {
    private static final Set<String> ROLES = Set.of("pharmacy", "lab");
    private final StaffAccountRepository staffAccountRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    @Transactional
    public String login(String username, String password, String role) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new BusinessException("请输入账号和密码");
        }
        StaffAccount account = staffAccountRepository.findByUsername(username.trim())
                .orElseThrow(() -> new BusinessException("账号不存在"));
        if (!Boolean.TRUE.equals(account.getEnabled())) throw new BusinessException("账号已停用");
        if (role != null && !role.equals(account.getRole())) throw new BusinessException("账号角色不匹配");
        if (!matches(password, account.getPassword())) throw new BusinessException("密码错误");
        if (!isBcrypt(account.getPassword())) {
            account.setPassword(encoder.encode(password));
            staffAccountRepository.save(account);
        }
        return jwtUtil.generateToken(account.getId(), account.getRole());
    }

    @Override
    public StaffAccount getInfo(Long id) {
        StaffAccount account = staffAccountRepository.findById(id)
                .orElseThrow(() -> new BusinessException("工作人员不存在"));
        account.setPassword(null);
        return account;
    }

    @Override
    @Transactional
    public String changePassword(Long id, String oldPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) throw new BusinessException("新密码长度不能少于6位");
        StaffAccount account = staffAccountRepository.findById(id)
                .orElseThrow(() -> new BusinessException("工作人员不存在"));
        if (!matches(oldPassword, account.getPassword())) throw new BusinessException("旧密码错误");
        account.setPassword(encoder.encode(newPassword));
        staffAccountRepository.save(account);
        return "密码修改成功";
    }

    @Override
    @Transactional
    public StaffAccount create(StaffAccount account) {
        if (account.getUsername() == null || account.getUsername().trim().length() < 3) throw new BusinessException("用户名长度不能少于3位");
        if (account.getPassword() == null || account.getPassword().length() < 6) throw new BusinessException("密码长度不能少于6位");
        if (staffAccountRepository.existsByUsername(account.getUsername().trim())) throw new BusinessException("用户名已存在");
        if (!ROLES.contains(account.getRole())) throw new BusinessException("角色不合法");
        account.setUsername(account.getUsername().trim());
        account.setPassword(encoder.encode(account.getPassword()));
        account.setEnabled(true);
        return staffAccountRepository.save(account);
    }

    private boolean matches(String raw, String stored) {
        return stored != null && (isBcrypt(stored) ? encoder.matches(raw, stored) : stored.equals(raw));
    }

    private boolean isBcrypt(String value) {
        return value != null && value.startsWith("$2");
    }
}
