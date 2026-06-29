package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.annotation.RequireLogin;
import com.neusoft.cloud_brain_diagnosis.common.context.UserContext;
import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import com.neusoft.cloud_brain_diagnosis.entity.FollowUpPlan;
import com.neusoft.cloud_brain_diagnosis.entity.FollowUpRecord;
import com.neusoft.cloud_brain_diagnosis.service.ai.AiFollowUpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/follow-up")
@RequiredArgsConstructor
@Tag(name = "AI智能随访", description = "AI智能随访模块")
public class FollowUpController {

    private final AiFollowUpService followUpService;

    /**
     * 创建随访计划（医生）
     */
    @PostMapping("/plan/create")
    @RequireLogin(RoleEnum.DOCTOR)
    @Operation(summary = "创建随访计划", description = "医生创建随访计划")
    public Result<FollowUpPlan> createPlan(@RequestBody Map<String, Object> request) {
        Long doctorId = UserContext.getUserId();
        return Result.success(followUpService.createPlan(request, doctorId));
    }

    /**
     * 我的随访计划（患者）
     */
    @GetMapping("/patient/plans")
    @RequireLogin(RoleEnum.PATIENT)
    @Operation(summary = "我的随访计划", description = "患者的随访计划列表")
    public Result<Page<FollowUpPlan>> getPatientPlans(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long patientId = UserContext.getUserId();
        return Result.success(followUpService.getPatientPlans(patientId, page, size));
    }

    /**
     * 待随访列表（患者）
     */
    @GetMapping("/pending")
    @RequireLogin(RoleEnum.PATIENT)
    @Operation(summary = "待随访列表", description = "患者待填写的随访")
    public Result<Page<FollowUpRecord>> getPendingRecords(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long patientId = UserContext.getUserId();
        return Result.success(followUpService.getPendingRecords(patientId, page, size));
    }

    /**
     * 提交随访（患者）
     */
    @PostMapping("/submit/{id}")
    @RequireLogin(RoleEnum.PATIENT)
    @Operation(summary = "提交随访", description = "患者提交随访问卷")
    public Result<String> submitRecord(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        String answerJson = request.get("answers") != null ? request.get("answers").toString() : "{}";
        Long patientId = UserContext.getUserId();
        return Result.success(followUpService.submitRecord(id, answerJson, patientId));
    }

    /**
     * 随访详情
     */
    @GetMapping("/detail/{id}")
    @RequireLogin
    @Operation(summary = "随访详情", description = "患者/医生查看随访记录详情")
    public Result<Map<String, Object>> getDetail(@PathVariable Long id) {
        return Result.success(followUpService.getDetail(id));
    }

    /**
     * 医生随访列表
     */
    @GetMapping("/doctor/list")
    @RequireLogin(RoleEnum.DOCTOR)
    @Operation(summary = "医生随访列表", description = "我负责的随访")
    public Result<Page<FollowUpPlan>> getDoctorList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long doctorId = UserContext.getUserId();
        return Result.success(followUpService.getDoctorList(doctorId, page, size));
    }

    /**
     * 医生回复异常随访
     */
    @PostMapping("/doctor-reply/{id}")
    @RequireLogin(RoleEnum.DOCTOR)
    @Operation(summary = "医生回复", description = "医生回复异常随访")
    public Result<String> doctorReply(@PathVariable Long id, @RequestParam String remark) {
        Long doctorId = UserContext.getUserId();
        return Result.success(followUpService.doctorReply(id, remark, doctorId));
    }
}
