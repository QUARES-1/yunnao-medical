package com.neusoft.cloud_brain_diagnosis.service;

import com.neusoft.cloud_brain_diagnosis.entity.StaffAccount;

public interface StaffAccountService {
    String login(String username, String password, String role);
    StaffAccount getInfo(Long id);
    String changePassword(Long id, String oldPassword, String newPassword);
    StaffAccount create(StaffAccount account);
}
