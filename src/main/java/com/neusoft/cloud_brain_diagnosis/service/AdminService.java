package com.neusoft.cloud_brain_diagnosis.service;

import com.neusoft.cloud_brain_diagnosis.entity.Admin;
import java.util.Map;

public interface AdminService {
    String login(String username, String password);
    Admin getAdminInfo(Long id);
    String changePassword(Long id, String oldPassword, String newPassword);
    Map<String, Object> getOverviewStatistics();
    /**
     * 管理员注册
     */
    String register(String username, String password, String name);
}