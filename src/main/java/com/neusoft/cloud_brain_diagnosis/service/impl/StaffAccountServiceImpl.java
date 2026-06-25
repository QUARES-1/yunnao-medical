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

@Service
@RequiredArgsConstructor
public class StaffAccountServiceImpl implements StaffAccountService {
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();
    private final StaffAccountRepository repository;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public String login(String username, String password, String role) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new BusinessException("请输入账号和密码");
        }
        StaffAccount account = repository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("账号不存在"));
        if (Boolean.FALSE.equals(account.getEnabled())) throw new BusinessException("账号已停用");
        if (role != null && !role.equals(account.getRole())) throw new BusinessException("账号角色不匹配");
        String stored = account.getPassword();
        boolean encoded = stored != null && stored.startsWith("$2");
        if (!(encoded ? ENCODER.matches(password, stored) : password.equals(stored))) {
            throw new BusinessException("密码错误");
        }
        if (!encoded) {
            account.setPassword(ENCODER.encode(password));
            repository.save(account);
        }
        return jwtUtil.generateToken(account.getId(), account.getRole());
    }

    @Override
    public StaffAccount getInfo(Long id) {
        StaffAccount account = repository.findById(id)
                .orElseThrow(() -> new BusinessException("工作人员不存在"));
        account.setPassword(null);
        return account;
    }

    @Override
    @Transactional
    public String changePassword(Long id, String oldPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) throw new BusinessException("新密码至少6位");
        StaffAccount account = repository.findById(id)
                .orElseThrow(() -> new BusinessException("工作人员不存在"));
        String stored = account.getPassword();
        boolean matches = stored != null && stored.startsWith("$2")
                ? ENCODER.matches(oldPassword, stored)
                : oldPassword != null && oldPassword.equals(stored);
        if (!matches) throw new BusinessException("原密码错误");
        account.setPassword(ENCODER.encode(newPassword));
        repository.save(account);
        return "密码修改成功";
    }

    @Override
    public StaffAccount create(StaffAccount account) {
        if (account.getUsername() == null || account.getUsername().length() < 3) throw new BusinessException("账号至少3位");
        if (repository.existsByUsername(account.getUsername())) throw new BusinessException("账号已存在");
        if (!"pharmacy".equals(account.getRole()) && !"lab".equals(account.getRole())) throw new BusinessException("角色不正确");
        String password = account.getPassword();
        if (password == null || password.length() < 6) throw new BusinessException("密码至少6位");
        account.setPassword(ENCODER.encode(password));
        return repository.save(account);
    }
}
