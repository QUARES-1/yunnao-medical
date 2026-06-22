package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.annotation.RequireLogin;
import com.neusoft.cloud_brain_diagnosis.common.context.UserContext;
import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import com.neusoft.cloud_brain_diagnosis.entity.Doctor;
import com.neusoft.cloud_brain_diagnosis.service.DoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 医生管理Controller
 * 公开接口：登录、列表、详情、排班
 * 医生接口：个人信息、修改信息
 * 管理员接口：增删改查、重置密码
 */
@RestController
@RequestMapping("/api/doctor")
@RequiredArgsConstructor
@Tag(name = "医生管理", description = "医生登录、信息查询、管理员管理")
public class DoctorController {

    private final DoctorService doctorService;

    // ========================================
    // 公开接口
    // ========================================

    /**
     * 医生Web端登录
     * 返回JWT token
     */
    @PostMapping("/login")
    @Operation(summary = "医生登录", description = "公开接口，账号密码登录，返回token")
    public Result<String> login(
            @RequestBody Map<String, String> body) {
        return Result.success(doctorService.login(body.get("username"), body.get("password")));
    }

    /**
     * 医生自助注册
     * 注册成功后直接返回JWT token
     */
    @PostMapping("/register")
    @Operation(summary = "医生注册", description = "公开接口，注册医生账号，返回token")
    public Result<String> register(
            @RequestBody Map<String, String> body) {
        return Result.success(doctorService.register(body.get("username"), body.get("password"), body.get("name")));
    }

    /**
     * 医生自助注册
     * 注册成功后直接返回JWT token
     */
    @PostMapping("/register")
    @Operation(summary = "医生注册", description = "公开接口，注册医生账号，返回token")
    public Result<String> register(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam(required = false) String name) {
        return Result.success(doctorService.register(username, password, name));
    }

    /**
     * 医生列表（公开）
     * 支持按科室筛选
     */
    @GetMapping("/list")
    @Operation(summary = "医生列表", description = "公开接口，支持按科室ID筛选")
    public Result<List<Doctor>> getDoctorList(
            @RequestParam(required = false) Long departmentId) {
        return Result.success(doctorService.getDoctorList(departmentId));
    }

    /**
     * 医生详情（公开）
     * 小程序医生主页用
     */
    @GetMapping("/detail/{id}")
    @Operation(summary = "医生详情", description = "公开接口，小程序医生主页用")
    public Result<Doctor> getDoctorDetail(@PathVariable Long id) {
        return Result.success(doctorService.getDoctorDetail(id));
    }

    /**
     * 获取医生排班（公开）
     * 返回可预约的日期和时间段
     */
    @GetMapping("/{id}/schedule")
    @Operation(summary = "获取医生排班", description = "公开接口，返回可预约的日期和时间段")
    public Result<Map<String, Object>> getSchedule(@PathVariable Long id) {
        return Result.success(doctorService.getSchedule(id));
    }

    // ========================================
    // 医生端接口（需要医生登录）
    // ========================================

    /**
     * 获取当前登录医生信息
     */
    @GetMapping("/info")
    @RequireLogin(RoleEnum.DOCTOR)
    @Operation(summary = "获取医生信息", description = "医生登录后获取自己的信息")
    public Result<Doctor> getDoctorInfo() {
        Long doctorId = UserContext.getUserId();
        return Result.success(doctorService.getDoctorInfo(doctorId));
    }

    /**
     * 医生修改个人信息
     */
    @PutMapping("/update")
    @RequireLogin(RoleEnum.DOCTOR)
    @Operation(summary = "修改个人信息", description = "医生修改自己的信息")
    public Result<String> updateDoctorInfo(@RequestBody Doctor doctor) {
        doctor.setId(UserContext.getUserId());
        return Result.success(doctorService.updateDoctorInfo(doctor));
    }

    @PutMapping("/change-password")
    @RequireLogin(RoleEnum.DOCTOR)
    public Result<String> changePassword(@RequestBody Map<String, String> body) {
        return Result.success(doctorService.changePassword(
                UserContext.getUserId(), body.get("oldPassword"), body.get("newPassword")));
    }

    // ========================================
    // 管理员端接口（需要管理员登录）
    // ========================================

    /**
     * 管理员添加医生
     */
    @PostMapping("/add")
    @RequireLogin(RoleEnum.ADMIN)
    @Operation(summary = "添加医生", description = "管理员权限，新增医生账号")
    public Result<String> addDoctor(@RequestBody Doctor doctor) {
        return Result.success(doctorService.addDoctor(doctor));
    }

    /**
     * 管理员-医生分页列表
     */
    @GetMapping("/page")
    @RequireLogin(RoleEnum.ADMIN)
    @Operation(summary = "医生分页列表", description = "管理员权限，后台管理用")
    public Result<Page<Doctor>> getDoctorPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(doctorService.getDoctorPage(page, size));
    }

    /**
     * 管理员重置医生密码
     * 重置为默认密码：123456
     */
    @PutMapping("/reset-pwd/{id}")
    @RequireLogin(RoleEnum.ADMIN)
    @Operation(summary = "重置密码", description = "管理员权限，重置为默认密码123456")
    public Result<String> resetPassword(@PathVariable Long id) {
        return Result.success(doctorService.resetPassword(id));
    }

    /**
     * 管理员删除医生
     */
    @DeleteMapping("/delete/{id}")
    @RequireLogin(RoleEnum.ADMIN)
    @Operation(summary = "删除医生", description = "管理员权限")
    public Result<String> deleteDoctor(@PathVariable Long id) {
        return Result.success(doctorService.deleteDoctor(id));
    }
}
