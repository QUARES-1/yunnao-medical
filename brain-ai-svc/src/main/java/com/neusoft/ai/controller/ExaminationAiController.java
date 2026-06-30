package com.neusoft.ai.controller;

import com.neusoft.ai.common.result.Result;
import com.neusoft.ai.service.ai.AiExaminationService;
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

    @PostMapping("/interpret")
    @Operation(summary = "AI解读检查结果")
    public Result<Map<String, Object>> interpret(@RequestBody Map<String, Object> request) {
        Long examinationId = request.get("examinationId") != null ? ((Number) request.get("examinationId")).longValue() : null;
        Long patientId = request.get("patientId") != null ? ((Number) request.get("patientId")).longValue() : null;
        return Result.success(examinationService.interpret(examinationId, patientId));
    }

    @GetMapping("/interpret-patient/{patientId}")
    @Operation(summary = "患者AI解读列表")
    public Result<Page<?>> getPatientInterpretation(@PathVariable Long patientId,
                                                    @RequestParam(defaultValue = "1") Integer page,
                                                    @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(examinationService.getPatientInterpretation(patientId, page, size));
    }

    @GetMapping("/interpret-pro/{id}")
    @Operation(summary = "专业解读详情")
    public Result<Map<String, Object>> getProInterpretation(@PathVariable Long id) {
        return Result.success(examinationService.getProInterpretation(id));
    }

    @GetMapping("/critical/list")
    @Operation(summary = "危急值预警列表")
    public Result<Page<Map<String, Object>>> getCriticalList(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(examinationService.getCriticalList(status, page, size));
    }

    @GetMapping("/critical/detect/{examinationId}")
    @Operation(summary = "检测危急值")
    public Result<Map<String, Object>> detectCriticalValue(@PathVariable Long examinationId) {
        return Result.success(examinationService.detectCriticalValue(examinationId));
    }

    @PostMapping("/critical/confirm/{id}")
    @Operation(summary = "确认危急值")
    public Result<Map<String, Object>> confirmWarning(@PathVariable Long id) {
        return Result.success(examinationService.confirmWarning(id));
    }

    @PostMapping("/critical/process/{id}")
    @Operation(summary = "处理危急值")
    public Result<Map<String, Object>> processWarning(@PathVariable Long id, @RequestParam String note) {
        return Result.success(examinationService.processWarning(id, note));
    }

    @GetMapping("/critical/history/{patientId}")
    @Operation(summary = "患者危急值历史")
    public Result<Page<Map<String, Object>>> getCriticalHistory(@PathVariable Long patientId,
                                                                 @RequestParam(defaultValue = "1") Integer page,
                                                                 @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(examinationService.getCriticalHistory(patientId, page, size));
    }

    @PostMapping("/review")
    @Operation(summary = "AI审核检查结果")
    public Result<Map<String, Object>> reviewExamination(@RequestBody Map<String, Object> request) {
        Long doctorId = com.neusoft.ai.common.context.UserContext.getUserId();
        return Result.success(examinationService.reviewExamination(request, doctorId));
    }

    @GetMapping("/manual-list")
    @Operation(summary = "医生手动审核列表")
    public Result<Page<Map<String, Object>>> getManualList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long doctorId = com.neusoft.ai.common.context.UserContext.getUserId();
        return Result.success(examinationService.getManualList(doctorId, page, size));
    }

    @GetMapping("/review-list")
    @Operation(summary = "AI审核列表")
    public Result<Page<Map<String, Object>>> getReviewList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long doctorId = com.neusoft.ai.common.context.UserContext.getUserId();
        return Result.success(examinationService.getReviewList(doctorId, page, size));
    }

    @GetMapping("/review/{id}")
    @Operation(summary = "审核详情")
    public Result<Map<String, Object>> getReviewDetail(@PathVariable Long id) {
        return Result.success(examinationService.getReviewDetail(id));
    }

    @PostMapping("/manual-confirm/{id}")
    @Operation(summary = "人工确认通过")
    public Result<Map<String, Object>> manualConfirm(@PathVariable Long id) {
        return Result.success(examinationService.manualConfirm(id));
    }

    @PostMapping("/reject/{id}")
    @Operation(summary = "驳回审核")
    public Result<Map<String, Object>> reject(@PathVariable Long id) {
        return Result.success(examinationService.reject(id));
    }

    @GetMapping("/review-stats")
    @Operation(summary = "审核统计")
    public Result<Map<String, Object>> getReviewStats() {
        return Result.success(examinationService.getReviewStats());
    }
}
