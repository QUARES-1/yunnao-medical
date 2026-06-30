package com.neusoft.ai.controller;

import com.neusoft.ai.common.context.UserContext;
import com.neusoft.ai.common.result.Result;
import com.neusoft.ai.entity.PrescriptionAiReview;
import com.neusoft.ai.service.ai.AiPrescriptionService;
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

    private final AiPrescriptionService prescriptionService;

    @PostMapping("/check")
    @Operation(summary = "AI审核处方", description = "提交处方药品，返回AI审核结果")
    public Result<Map<String, Object>> checkPrescription(@RequestBody Map<String, Object> request) {
        Long doctorId = UserContext.getUserId();
        return Result.success(prescriptionService.checkPrescription(request, doctorId));
    }

    @GetMapping("/review-list")
    @Operation(summary = "审核记录列表", description = "历史审核记录")
    public Result<Page<PrescriptionAiReview>> getReviewList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        String role = UserContext.getRole();
        Long userId = "doctor".equals(role) ? UserContext.getUserId() : null;
        return Result.success(prescriptionService.getReviewList(userId, page, size));
    }

    @GetMapping("/review/{id}")
    @Operation(summary = "审核详情", description = "审核记录详情")
    public Result<PrescriptionAiReview> getReviewDetail(@PathVariable Long id) {
        return Result.success(prescriptionService.getReviewDetail(id));
    }
}
