package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.annotation.RequireLogin;
import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import com.neusoft.cloud_brain_diagnosis.feign.AiOtherFeignClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/follow-up")
@RequiredArgsConstructor
@Tag(name = "AI智能随访", description = "AI智能随访模块")
public class FollowUpController {

    private final AiOtherFeignClient otherFeignClient;

    /**
     * 创建随访计划（医生）
     */
    @PostMapping("/plan/create")
    @RequireLogin(RoleEnum.DOCTOR)
    @Operation(summary = "创建随访计划", description = "医生创建随访计划")
    public Result<Map<String, Object>> createPlan(@RequestBody Map<String, Object> request) {
        return otherFeignClient.createFollowUpPlan(request);
    }

    /**
     * 我的随访计划（患者）
     */
    @GetMapping("/patient/plans")
    @RequireLogin(RoleEnum.PATIENT)
    @Operation(summary = "我的随访计划", description = "患者的随访计划列表")
    public Result<Map<String, Object>> getPatientPlans(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return otherFeignClient.getPatientPlans(page, size);
    }

    /**
     * 待随访列表（患者）
     */
    @GetMapping("/pending")
    @RequireLogin(RoleEnum.PATIENT)
    @Operation(summary = "待随访列表", description = "患者待填写的随访")
    public Result<Map<String, Object>> getPendingRecords(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return otherFeignClient.getPendingRecords(page, size);
    }

    /**
     * 提交随访（患者）
     */
    @PostMapping("/submit/{id}")
    @RequireLogin(RoleEnum.PATIENT)
    @Operation(summary = "提交随访", description = "患者提交随访问卷")
    public Result<Map<String, Object>> submitRecord(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        return otherFeignClient.submitRecord(id, request);
    }

    /**
     * 随访详情
     */
    @GetMapping("/detail/{id}")
    @RequireLogin
    @Operation(summary = "随访详情", description = "患者/医生查看随访记录详情")
    public Result<Map<String, Object>> getDetail(@PathVariable Long id) {
        return otherFeignClient.getFollowUpDetail(id);
    }

    /**
     * 医生随访列表
     */
    @GetMapping("/doctor/list")
    @RequireLogin(RoleEnum.DOCTOR)
    @Operation(summary = "医生随访列表", description = "我负责的随访")
    public Result<Map<String, Object>> getDoctorList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return otherFeignClient.getDoctorFollowUpList(page, size);
    }

    /**
     * 医生回复异常随访
     */
    @PostMapping("/doctor-reply/{id}")
    @RequireLogin(RoleEnum.DOCTOR)
    @Operation(summary = "医生回复", description = "医生回复异常随访")
    public Result<Map<String, Object>> doctorReply(@PathVariable Long id, @RequestParam String remark) {
        return otherFeignClient.doctorReply(id, remark);
    }
}
