package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.annotation.RequireLogin;
import com.neusoft.cloud_brain_diagnosis.common.context.UserContext;
import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import com.neusoft.cloud_brain_diagnosis.entity.CriticalValueWarning;
import com.neusoft.cloud_brain_diagnosis.entity.ExaminationAiInterpretation;
import com.neusoft.cloud_brain_diagnosis.entity.ExaminationAiReview;
import com.neusoft.cloud_brain_diagnosis.service.ai.AiExaminationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/examination/ai")
@RequiredArgsConstructor
@Tag(name = "AI检验服务", description = "AI检验报告解读、危急值预警、检验结果智能审核")
public class ExaminationAiController {

    private final AiExaminationService examinationService;

    // ========== 检验报告解读 ==========

    /**
     * 生成AI解读报告
     */
    @PostMapping("/interpret/{id}")
    @RequireLogin({RoleEnum.LAB, RoleEnum.DOCTOR})
    @Operation(summary = "生成解读", description = "生成检验报告的AI解读")
    public Result<Map<String, Object>> interpret(@PathVariable Long id) {
        return Result.success(examinationService.interpret(id));
    }

    /**
     * 患者版解读
     */
    @GetMapping("/interpret-patient/{id}")
    @Operation(summary = "患者版解读", description = "患者查看通俗版解读")
    public Result<ExaminationAiInterpretation> getPatientInterpretation(@PathVariable Long id) {
        return Result.success(examinationService.getPatientInterpretation(id));
    }

    /**
     * 专业版解读
     */
    @GetMapping("/interpret-pro/{id}")
    @RequireLogin(RoleEnum.DOCTOR)
    @Operation(summary = "专业版解读", description = "医生查看专业版解读")
    public Result<ExaminationAiInterpretation> getProInterpretation(@PathVariable Long id) {
        return Result.success(examinationService.getProInterpretation(id));
    }

    // ========== 危急值预警 ==========

    /**
     * 待处理预警列表
     */
    @GetMapping("/critical-list")
    @RequireLogin({RoleEnum.DOCTOR, RoleEnum.LAB, RoleEnum.PATIENT})
    @Operation(summary = "待处理预警", description = "查看待处理的危急值预警")
    public Result<Page<CriticalValueWarning>> getCriticalList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(examinationService.getCriticalList(
                UserContext.getUserId(), UserContext.getRole(), page, size));
    }

    /**
     * 确认预警
     */
    @PostMapping("/confirm-warning/{id}")
    @RequireLogin(RoleEnum.DOCTOR)
    @Operation(summary = "确认预警", description = "医生确认收到预警")
    public Result<String> confirmWarning(@PathVariable Long id) {
        Long doctorId = UserContext.getUserId();
        return Result.success(examinationService.confirmWarning(id, doctorId));
    }

    /**
     * 处理预警
     */
    @PostMapping("/process-warning/{id}")
    @RequireLogin(RoleEnum.DOCTOR)
    @Operation(summary = "处理预警", description = "医生填写处理意见")
    public Result<String> processWarning(@PathVariable Long id, @RequestParam String remark) {
        Long doctorId = UserContext.getUserId();
        return Result.success(examinationService.processWarning(id, remark, doctorId));
    }

    /**
     * 历史预警列表
     */
    @GetMapping("/critical-history")
    @RequireLogin({RoleEnum.ADMIN, RoleEnum.LAB})
    @Operation(summary = "历史预警", description = "历史预警记录")
    public Result<Page<CriticalValueWarning>> getCriticalHistory(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(examinationService.getCriticalHistory(page, size));
    }

    // ========== 检验AI审核 ==========

    /**
     * AI审核检验结果
     */
    @PostMapping("/review/{id}")
    @RequireLogin(RoleEnum.LAB)
    @Operation(summary = "AI审核结果", description = "对检验结果进行AI审核")
    public Result<Map<String, Object>> reviewExamination(@PathVariable Long id) {
        Long labStaffId = UserContext.getUserId();
        return Result.success(examinationService.reviewExamination(id, labStaffId));
    }

    /**
     * 待人工复核列表
     */
    @GetMapping("/manual-list")
    @RequireLogin(RoleEnum.LAB)
    @Operation(summary = "待人工复核", description = "需要人工复核的列表")
    public Result<Page<ExaminationAiReview>> getManualList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(examinationService.getManualList(page, size));
    }

    /**
     * 审核记录列表
     */
    @GetMapping("/review-list")
    @RequireLogin(RoleEnum.LAB)
    @Operation(summary = "审核记录列表", description = "查询AI审核记录，可按结论筛选：pass/manual/reject")
    public Result<Page<ExaminationAiReview>> getReviewList(
            @RequestParam(required = false) String reviewResult,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(examinationService.getReviewList(reviewResult, page, size));
    }

    /**
     * 审核详情
     */
    @GetMapping("/review-detail/{id}")
    @RequireLogin({RoleEnum.LAB, RoleEnum.DOCTOR})
    @Operation(summary = "审核详情", description = "审核记录详情")
    public Result<ExaminationAiReview> getReviewDetail(@PathVariable Long id) {
        return Result.success(examinationService.getReviewDetail(id));
    }

    /**
     * 人工确认
     */
    @PostMapping("/manual-confirm/{id}")
    @RequireLogin(RoleEnum.LAB)
    @Operation(summary = "人工确认", description = "人工确认审核结果")
    public Result<String> manualConfirm(@PathVariable Long id) {
        return Result.success(examinationService.manualConfirm(id));
    }

    /**
     * 退回重测
     */
    @PostMapping("/reject/{id}")
    @RequireLogin(RoleEnum.LAB)
    @Operation(summary = "退回重测", description = "退回重测")
    public Result<String> reject(@PathVariable Long id, @RequestParam String reason) {
        return Result.success(examinationService.reject(id, reason));
    }

    /**
     * 审核统计
     */
    @GetMapping("/review-stats")
    @RequireLogin({RoleEnum.LAB, RoleEnum.ADMIN})
    @Operation(summary = "审核统计", description = "统计AI审核数据")
    public Result<Map<String, Object>> getReviewStats() {
        return Result.success(examinationService.getReviewStats());
    }
}
