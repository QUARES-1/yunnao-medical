package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.annotation.RequireLogin;
import com.neusoft.cloud_brain_diagnosis.common.context.UserContext;
import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import com.neusoft.cloud_brain_diagnosis.entity.Registration;
import com.neusoft.cloud_brain_diagnosis.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 预约挂号Controller（最核心业务）
 * 患者端：创建、列表、详情、取消
 * 医生端：今日列表、历史列表、开始、完成
 */
@RestController
@RequestMapping("/api/registration")
@RequiredArgsConstructor
@Tag(name = "预约挂号", description = "挂号全流程，最核心业务模块")
public class RegistrationController {

    private final RegistrationService registrationService;

    // ========================================
    // 患者端接口
    // ========================================

    /**
     * 患者-创建挂号
     * 需要传：doctorId, registrationDate, timeSlot
     */
    @PostMapping("/create")
    @RequireLogin(RoleEnum.PATIENT)
    @Operation(summary = "创建挂号", description = "患者提交挂号申请")
    public Result<Registration> createRegistration(@RequestBody Registration registration) {
        registration.setPatientId(UserContext.getUserId());
        return Result.success(registrationService.createRegistration(registration));
    }

    /**
     * 患者-我的挂号记录
     * 支持按状态筛选，分页
     */
    @GetMapping("/patient/list")
    @RequireLogin(RoleEnum.PATIENT)
    @Operation(summary = "我的挂号记录", description = "患者查询自己的挂号记录，支持状态筛选")
    public Result<Page<Registration>> getPatientList(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long patientId = UserContext.getUserId();
        return Result.success(registrationService.getPatientRegistrationList(patientId, status, page, size));
    }

    /**
     * 挂号详情（需要登录）
     */
    @GetMapping("/detail/{id}")
    @RequireLogin
    @Operation(summary = "挂号详情", description = "患者和医生都可以查看")
    public Result<Registration> getDetail(@PathVariable Long id) {
        return Result.success(registrationService.getDetail(id, UserContext.getUserId(), UserContext.getRole()));
    }

    /**
     * 患者-取消挂号
     * 只有待就诊状态才能取消
     */
    @PutMapping("/cancel/{id}")
    @RequireLogin(RoleEnum.PATIENT)
    @Operation(summary = "取消挂号", description = "患者取消挂号，只有待就诊状态才能取消")
    public Result<String> cancelRegistration(@PathVariable Long id) {
        Long patientId = UserContext.getUserId();
        return Result.success(registrationService.cancelRegistration(id, patientId));
    }

    // ========================================
    // 医生端接口
    // ========================================

    /**
     * 医生-今日挂号列表
     * 按时间升序排列
     */
    @GetMapping("/doctor/today")
    @RequireLogin(RoleEnum.DOCTOR)
    @Operation(summary = "今日挂号列表", description = "医生查看今天的挂号患者")
    public Result<List<Registration>> getDoctorTodayList() {
        Long doctorId = UserContext.getUserId();
        return Result.success(registrationService.getDoctorTodayList(doctorId));
    }

    /**
     * 医生-历史挂号列表
     * 分页
     */
    @GetMapping("/doctor/list")
    @RequireLogin(RoleEnum.DOCTOR)
    @Operation(summary = "历史挂号列表", description = "医生查看历史就诊记录，分页")
    public Result<Page<Registration>> getDoctorList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long doctorId = UserContext.getUserId();
        return Result.success(registrationService.getDoctorList(doctorId, keyword, status, page, size));
    }

    /**
     * 医生-开始看诊
     * 状态从 待就诊 → 就诊中
     */
    @PutMapping("/start/{id}")
    @RequireLogin(RoleEnum.DOCTOR)
    @Operation(summary = "开始看诊", description = "医生开始看诊，状态改为就诊中")
    public Result<String> startConsultation(@PathVariable Long id) {
        Long doctorId = UserContext.getUserId();
        return Result.success(registrationService.startConsultation(id, doctorId));
    }

    /**
     * 医生-完成看诊
     * 状态从 就诊中 → 已就诊
     */
    @PutMapping("/complete/{id}")
    @RequireLogin(RoleEnum.DOCTOR)
    @Operation(summary = "完成看诊", description = "医生完成看诊，状态改为已就诊")
    public Result<String> completeConsultation(@PathVariable Long id) {
        Long doctorId = UserContext.getUserId();
        return Result.success(registrationService.completeConsultation(id, doctorId));
    }
}
