package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.annotation.RequireLogin;
import com.neusoft.cloud_brain_diagnosis.common.context.UserContext;
import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import com.neusoft.cloud_brain_diagnosis.entity.AiChatRecord;
import com.neusoft.cloud_brain_diagnosis.entity.OperationAiReport;
import com.neusoft.cloud_brain_diagnosis.entity.QualityCheckDetail;
import com.neusoft.cloud_brain_diagnosis.entity.QualityCheckRecord;
import com.neusoft.cloud_brain_diagnosis.service.ai.AiChatService;
import com.neusoft.cloud_brain_diagnosis.service.ai.AiOperationService;
import com.neusoft.cloud_brain_diagnosis.service.ai.AiQualityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/ai")
@RequiredArgsConstructor
@RequireLogin(RoleEnum.ADMIN)
@Tag(name = "AI管理端", description = "AI运营分析、医疗质检、问答日志")
public class AiAdminController {

    private final AiOperationService operationService;
    private final AiQualityService qualityService;
    private final AiChatService chatService;

    // ========== 运营分析 ==========

    /**
     * 生成运营报告
     */
    @PostMapping("/operation-report/generate")
    @Operation(summary = "生成运营报告", description = "生成指定时间段的AI运营分析报告")
    public Result<Map<String, Object>> generateReport(@RequestBody Map<String, Object> request) {
        String reportType = (String) request.getOrDefault("reportType", "daily");
        String startDate = (String) request.get("startDate");
        String endDate = (String) request.get("endDate");
        return Result.success(operationService.generateReport(reportType, startDate, endDate));
    }

    /**
     * 运营报告详情
     */
    @GetMapping("/operation-report/{id}")
    @Operation(summary = "运营报告详情", description = "查看运营分析报告详情")
    public Result<OperationAiReport> getReportDetail(@PathVariable Long id) {
        return Result.success(operationService.getReportDetail(id));
    }

    /**
     * 报告列表
     */
    @GetMapping("/operation-report/list")
    @Operation(summary = "报告列表", description = "历史运营报告列表")
    public Result<Page<OperationAiReport>> getReportList(
            @RequestParam(required = false) String reportType,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(operationService.getReportList(reportType, page, size));
    }

    /**
     * 首页AI概览
     */
    @GetMapping("/operation-overview")
    @Operation(summary = "首页AI概览", description = "管理员首页仪表盘AI分析概览")
    public Result<Map<String, Object>> getOperationOverview() {
        return Result.success(operationService.getOperationOverview());
    }

    // ========== 医疗质量质检 ==========

    /**
     * 发起AI质检
     */
    @PostMapping("/quality-check/start")
    @Operation(summary = "发起AI质检", description = "发起一次AI医疗质量质检")
    public Result<Map<String, Object>> startQualityCheck(@RequestBody Map<String, Object> request) {
        String checkType = (String) request.getOrDefault("checkType", "medical_record");
        Integer sampleSize = request.get("sampleSize") != null ? ((Number) request.get("sampleSize")).intValue() : 10;
        return Result.success(qualityService.startQualityCheck(checkType, sampleSize));
    }

    /**
     * 质检记录列表
     */
    @GetMapping("/quality-check/list")
    @Operation(summary = "质检记录列表", description = "历史质检记录")
    public Result<Page<QualityCheckRecord>> getCheckList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(qualityService.getCheckList(page, size));
    }

    /**
     * 质检详情
     */
    @GetMapping("/quality-check/{id}")
    @Operation(summary = "质检详情", description = "质检记录详情")
    public Result<QualityCheckRecord> getCheckDetail(@PathVariable Long id) {
        return Result.success(qualityService.getCheckDetail(id));
    }

    /**
     * 问题明细列表
     */
    @GetMapping("/quality-check/{id}/details")
    @Operation(summary = "问题明细列表", description = "质检问题明细")
    public Result<Page<QualityCheckDetail>> getCheckDetails(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(qualityService.getCheckDetails(id, page, size));
    }

    /**
     * 医生质检统计
     */
    @GetMapping("/quality-check/doctor-stats")
    @Operation(summary = "医生质检统计", description = "各医生质检统计")
    public Result<Map<String, Object>> getDoctorStats() {
        return Result.success(qualityService.getDoctorStats());
    }

    // ========== 问答日志 ==========

    /**
     * 问答日志
     */
    @GetMapping("/chat-log")
    @Operation(summary = "问答日志", description = "查看所有AI问答日志")
    public Result<Page<AiChatRecord>> getChatLogs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(chatService.getChatLogs(page, size));
    }
}
