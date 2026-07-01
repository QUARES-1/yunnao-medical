package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.annotation.RequireLogin;
import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import com.neusoft.cloud_brain_diagnosis.feign.AiExaminationFeignClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/examination/ai")
@RequiredArgsConstructor
@Tag(name = "AI检验服务", description = "AI检验报告解读、危急值预警、检验结果智能审核")
public class ExaminationAiController {

    private final AiExaminationFeignClient examinationFeignClient;

    // ========== 检验报告解读 ==========

    /**
     * 生成AI解读报告
     */
    @PostMapping("/interpret/{id}")
    @RequireLogin({RoleEnum.LAB, RoleEnum.DOCTOR})
    @Operation(summary = "生成解读", description = "生成检验报告的AI解读")
    public Result<Map<String, Object>> interpret(@PathVariable Long id) {
        Map<String, Object> request = new HashMap<>();
        request.put("examinationId", id);
        return examinationFeignClient.interpret(request);
    }

    /**
     * 患者版解读
     */
    @GetMapping("/interpret-patient/{id}")
    @Operation(summary = "患者版解读", description = "患者查看通俗版解读")
    public Result<Map<String, Object>> getPatientInterpretation(@PathVariable Long id) {
        return examinationFeignClient.getPatientInterpretation(id, 1, 10);
    }

    /**
     * 专业版解读
     */
    @GetMapping("/interpret-pro/{id}")
    @RequireLogin(RoleEnum.DOCTOR)
    @Operation(summary = "专业版解读", description = "医生查看专业版解读")
    public Result<Map<String, Object>> getProInterpretation(@PathVariable Long id) {
        return examinationFeignClient.getProInterpretation(id);
    }

    // ========== 危急值预警 ==========

    /**
     * 待处理预警列表
     */
    @GetMapping("/critical-list")
    @RequireLogin({RoleEnum.DOCTOR, RoleEnum.LAB, RoleEnum.PATIENT})
    @Operation(summary = "待处理预警", description = "查看待处理的危急值预警")
    public Result<Map<String, Object>> getCriticalList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return examinationFeignClient.getCriticalList(null, page, size);
    }

    /**
     * 确认预警
     */
    @PostMapping("/confirm-warning/{id}")
    @RequireLogin(RoleEnum.DOCTOR)
    @Operation(summary = "确认预警", description = "医生确认收到预警")
    public Result<Map<String, Object>> confirmWarning(@PathVariable Long id) {
        return examinationFeignClient.confirmWarning(id);
    }

    /**
     * 处理预警
     */
    @PostMapping("/process-warning/{id}")
    @RequireLogin(RoleEnum.DOCTOR)
    @Operation(summary = "处理预警", description = "医生填写处理意见")
    public Result<Map<String, Object>> processWarning(@PathVariable Long id, @RequestParam String remark) {
        return examinationFeignClient.processWarning(id, remark);
    }

    /**
     * 历史预警列表
     */
    @GetMapping("/critical-history")
    @RequireLogin({RoleEnum.ADMIN, RoleEnum.LAB})
    @Operation(summary = "历史预警", description = "历史预警记录")
    public Result<Map<String, Object>> getCriticalHistory(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        // 历史预警不需要传 patientId，传0表示全部
        return examinationFeignClient.getCriticalHistory(0L, page, size);
    }

    // ========== 检验AI审核 ==========

    /**
     * AI审核检验结果
     */
    @PostMapping("/review/{id}")
    @RequireLogin(RoleEnum.LAB)
    @Operation(summary = "AI审核结果", description = "对检验结果进行AI审核")
    public Result<Map<String, Object>> reviewExamination(@PathVariable Long id) {
        Map<String, Object> request = new HashMap<>();
        request.put("examinationId", id);
        return examinationFeignClient.reviewExamination(request);
    }

    /**
     * 待人工复核列表
     */
    @GetMapping("/manual-list")
    @RequireLogin(RoleEnum.LAB)
    @Operation(summary = "待人工复核", description = "需要人工复核的列表")
    public Result<Map<String, Object>> getManualList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return examinationFeignClient.getManualList(page, size);
    }

    /**
     * 审核记录列表
     */
    @GetMapping("/review-list")
    @RequireLogin(RoleEnum.LAB)
    @Operation(summary = "审核记录列表", description = "查询AI审核记录，可按结论筛选：pass/manual/reject")
    public Result<Map<String, Object>> getReviewList(
            @RequestParam(required = false) String reviewResult,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return examinationFeignClient.getReviewList(reviewResult, page, size);
    }

    /**
     * 审核详情
     */
    @GetMapping("/review-detail/{id}")
    @RequireLogin({RoleEnum.LAB, RoleEnum.DOCTOR})
    @Operation(summary = "审核详情", description = "审核记录详情")
    public Result<Map<String, Object>> getReviewDetail(@PathVariable Long id) {
        return examinationFeignClient.getReviewDetail(id);
    }

    /**
     * 人工确认
     */
    @PostMapping("/manual-confirm/{id}")
    @RequireLogin(RoleEnum.LAB)
    @Operation(summary = "人工确认", description = "人工确认审核结果")
    public Result<Map<String, Object>> manualConfirm(@PathVariable Long id) {
        return examinationFeignClient.manualConfirm(id);
    }

    /**
     * 退回重测
     */
    @PostMapping("/reject/{id}")
    @RequireLogin(RoleEnum.LAB)
    @Operation(summary = "退回重测", description = "退回重测")
    public Result<Map<String, Object>> reject(@PathVariable Long id, @RequestParam String reason) {
        return examinationFeignClient.reject(id, reason);
    }

    /**
     * 审核统计
     */
    @GetMapping("/review-stats")
    @RequireLogin({RoleEnum.LAB, RoleEnum.ADMIN})
    @Operation(summary = "审核统计", description = "统计AI审核数据")
    public Result<Map<String, Object>> getReviewStats() {
        return examinationFeignClient.getReviewStats();
    }
}
