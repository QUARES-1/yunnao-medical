package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.annotation.RequireLogin;
import com.neusoft.cloud_brain_diagnosis.common.context.UserContext;
import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import com.neusoft.cloud_brain_diagnosis.entity.Admin;
import com.neusoft.cloud_brain_diagnosis.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员Controller
 * 登录、个人信息、修改密码、统计
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "管理员管理", description = "管理员登录、信息、统计")
public class AdminController {

    private final AdminService adminService;

    /**
     * 管理员登录
     * 返回JWT token
     */
    @PostMapping("/login")
    @Operation(summary = "管理员登录", description = "公开接口，账号密码登录，返回token")
    public Result<String> login(
            @RequestParam String username,
            @RequestParam String password) {
        return Result.success(adminService.login(username, password));
    }

    /**
     * 获取当前登录管理员信息
     */
    @GetMapping("/info")
    @RequireLogin(RoleEnum.ADMIN)
    @Operation(summary = "获取管理员信息", description = "管理员登录后获取自己的信息")
    public Result<Admin> getAdminInfo() {
        Long adminId = UserContext.getUserId();
        return Result.success(adminService.getAdminInfo(adminId));
    }

    /**
     * 修改密码
     */
    @PutMapping("/change-pwd")
    @RequireLogin(RoleEnum.ADMIN)
    @Operation(summary = "修改密码", description = "管理员修改自己的密码")
    public Result<String> changePassword(
            @RequestParam String oldPassword,
            @RequestParam String newPassword) {
        Long adminId = UserContext.getUserId();
        return Result.success(adminService.changePassword(adminId, oldPassword, newPassword));
    }

    /**
     * 首页统计概览
     * 返回患者数、医生数、科室数、挂号数等
     */
    @GetMapping("/statistics/overview")
    @RequireLogin(RoleEnum.ADMIN)
    @Operation(summary = "首页统计概览", description = "管理员首页统计数据")
    public Result<Map<String, Object>> getOverviewStatistics() {
        return Result.success(adminService.getOverviewStatistics());
    }
}