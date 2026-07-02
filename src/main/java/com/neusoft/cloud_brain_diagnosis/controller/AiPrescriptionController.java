package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.annotation.RequireLogin;
import com.neusoft.cloud_brain_diagnosis.common.context.UserContext;
import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import com.neusoft.cloud_brain_diagnosis.entity.PrescriptionAiReview;
import com.neusoft.cloud_brain_diagnosis.service.ai.AiPrescriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/prescription/ai")
@RequiredArgsConstructor
@Tag(name = "AI处方审核", description = "AI处方智能审核模块")
public class AiPrescriptionController {

    private final AiPrescriptionService aiPrescriptionService;

    /**
     * AI审核处方。
     * 直接由主后端 8080 完成审核，并在发现高风险时通过 WebSocket 给医生端推送实时预警。
     */
    @PostMapping("/check")
    @RequireLogin({RoleEnum.DOCTOR, RoleEnum.PHARMACY})
    @Operation(summary = "AI审核处方", description = "检查配伍禁忌、相互作用、剂量、重复用药和过敏风险")
    public Result<Map<String, Object>> checkPrescription(@RequestBody Map<String, Object> request) {
        return Result.success(aiPrescriptionService.checkPrescription(request, UserContext.getUserId()));
    }

    /**
     * 审核记录列表
     */
    @GetMapping("/review-list")
    @RequireLogin({RoleEnum.DOCTOR, RoleEnum.PHARMACY})
    @Operation(summary = "审核记录列表", description = "历史 AI 处方审核记录")
    public Result<Page<PrescriptionAiReview>> getReviewList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(aiPrescriptionService.getReviewList(UserContext.getUserId(), page, size));
    }

    /**
     * 审核详情
     */
    @GetMapping("/review/{id}")
    @RequireLogin({RoleEnum.DOCTOR, RoleEnum.PHARMACY})
    @Operation(summary = "审核详情", description = "AI 处方审核记录详情")
    public Result<PrescriptionAiReview> getReviewDetail(@PathVariable Long id) {
        return Result.success(aiPrescriptionService.getReviewDetail(id));
    }
}