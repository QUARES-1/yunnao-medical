package com.neusoft.ai.controller;

import com.neusoft.ai.common.context.UserContext;
import com.neusoft.ai.common.result.Result;
import com.neusoft.ai.entity.FollowUpPlan;
import com.neusoft.ai.entity.FollowUpRecord;
import com.neusoft.ai.service.ai.AiFollowUpService;
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

    @PostMapping("/plan/create")
    @Operation(summary = "创建随访计划", description = "医生创建随访计划")
    public Result<FollowUpPlan> createPlan(@RequestBody Map<String, Object> request) {
        Long doctorId = UserContext.getUserId();
        return Result.success(followUpService.createPlan(request, doctorId));
    }

    @GetMapping("/patient/plans")
    @Operation(summary = "我的随访计划", description = "患者的随访计划列表")
    public Result<Page<FollowUpPlan>> getPatientPlans(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long patientId = UserContext.getUserId();
        return Result.success(followUpService.getPatientPlans(patientId, page, size));
    }

    @GetMapping("/pending")
    @Operation(summary = "待随访列表", description = "患者待填写的随访")
    public Result<Page<FollowUpRecord>> getPendingRecords(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long patientId = UserContext.getUserId();
        return Result.success(followUpService.getPendingRecords(patientId, page, size));
    }

    @PostMapping("/submit/{id}")
    @Operation(summary = "提交随访", description = "患者提交随访问卷")
    public Result<String> submitRecord(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        String answerJson = request.get("answers") != null ? request.get("answers").toString() : "{}";
        Long patientId = UserContext.getUserId();
        return Result.success(followUpService.submitRecord(id, answerJson, patientId));
    }

    @GetMapping("/detail/{id}")
    @Operation(summary = "随访详情")
    public Result<Map<String, Object>> getDetail(@PathVariable Long id) {
        return Result.success(followUpService.getDetail(id));
    }

    @GetMapping("/doctor/list")
    @Operation(summary = "医生随访列表")
    public Result<Page<FollowUpPlan>> getDoctorList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long doctorId = UserContext.getUserId();
        return Result.success(followUpService.getDoctorList(doctorId, page, size));
    }

    @PostMapping("/doctor-reply/{id}")
    @Operation(summary = "医生回复异常随访")
    public Result<String> doctorReply(@PathVariable Long id, @RequestParam String remark) {
        Long doctorId = UserContext.getUserId();
        return Result.success(followUpService.doctorReply(id, remark, doctorId));
    }
}
