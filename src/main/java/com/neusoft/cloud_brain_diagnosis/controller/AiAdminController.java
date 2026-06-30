package com.neusoft.cloud_brain_diagnosis.controller;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

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
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.LinkedHashMap;

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

    /**
     * 流式生成运营报告
     */
    @PostMapping(value = "/operation-report/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式生成运营报告", description = "通过 SSE 逐字返回 AI 运营报告内容，前端可实时展示生成过程")
    public SseEmitter streamGenerateReport(@RequestBody Map<String, Object> request) {
        SseEmitter emitter = new SseEmitter(120_000L);
        new Thread(() -> {
            try {
                String reportType = (String) request.getOrDefault("reportType", "daily");
                String startDate = (String) request.get("startDate");
                String endDate = (String) request.get("endDate");
                Map<String, Object> generated = operationService.generateReport(reportType, startDate, endDate);
                Long reportId = generated.get("id") instanceof Number ? ((Number) generated.get("id")).longValue() : null;
                OperationAiReport report = reportId != null ? operationService.getReportDetail(reportId) : null;
                String streamText = buildStreamReportText(report, generated);
                for (int i = 0; i < streamText.length(); i++) {
                    emitter.send(SseEmitter.event().name("delta").data(String.valueOf(streamText.charAt(i))));
                    Thread.sleep(14L);
                }
                emitter.send(SseEmitter.event().name("done").data(String.valueOf(generated.get("id"))));
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage() == null ? "流式生成失败" : e.getMessage()));
                } catch (Exception ignored) {}
                emitter.completeWithError(e);
            }
        }, "ai-operation-report-stream").start();
        return emitter;
    }

    private String buildStreamReportText(OperationAiReport report, Map<String, Object> generated) {
        if (report == null) {
            return "一、运营摘要\n" + String.valueOf(generated.getOrDefault("summary", "暂无摘要"));
        }
        StringBuilder builder = new StringBuilder();
        builder.append("一、运营摘要\n").append(cleanText(report.getSummary())).append("\n\n");
        builder.append("二、核心指标\n").append(formatMetrics(report.getKeyMetrics())).append("\n");
        builder.append("三、趋势分析\n").append(cleanText(report.getTrendsAnalysis())).append("\n\n");
        builder.append("四、风险提醒\n").append(formatWarnings(report.getWarnings())).append("\n");
        builder.append("五、优化建议\n").append(formatSuggestions(report.getSuggestions()));
        return builder.toString();
    }

    private String cleanText(String value) {
        return value == null || value.isBlank() ? "暂无" : value.trim();
    }

    private String formatMetrics(String jsonText) {
        if (jsonText == null || jsonText.isBlank()) {
            return "暂无核心指标。\n";
        }
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("aiTriageCount", "AI分诊调用次数");
        labels.put("triageAccuracy", "分诊推荐采纳率");
        labels.put("aiChatCount", "AI问答次数");
        labels.put("knowledgeHitRate", "知识库命中率");
        labels.put("reportInterpretationCount", "检验报告解读次数");
        labels.put("medicationGuideCount", "用药指导生成次数");
        labels.put("criticalWarningCount", "危急值预警次数");
        labels.put("followUpCount", "智能随访任务数");
        labels.put("qualityCheckCount", "AI质检批次数");
        try {
            JSONObject obj = JSONUtil.parseObj(jsonText);
            StringBuilder builder = new StringBuilder();
            for (Map.Entry<String, String> entry : labels.entrySet()) {
                if (obj.containsKey(entry.getKey())) {
                    builder.append("- ").append(entry.getValue()).append("：").append(obj.get(entry.getKey())).append("\n");
                }
            }
            return builder.length() > 0 ? builder.toString() : jsonText + "\n";
        } catch (Exception e) {
            return jsonText + "\n";
        }
    }

    private String formatWarnings(String jsonText) {
        if (jsonText == null || jsonText.isBlank()) {
            return "- 本周期未发现明显风险。\n";
        }
        try {
            JSONArray array = JSONUtil.parseArray(jsonText);
            StringBuilder builder = new StringBuilder();
            for (Object item : array) {
                if (item instanceof JSONObject obj) {
                    builder.append("- ").append(obj.getStr("content", obj.toString())).append("\n");
                } else {
                    builder.append("- ").append(String.valueOf(item)).append("\n");
                }
            }
            return builder.length() > 0 ? builder.toString() : "- 本周期未发现明显风险。\n";
        } catch (Exception e) {
            return jsonText + "\n";
        }
    }

    private String formatSuggestions(String jsonText) {
        if (jsonText == null || jsonText.isBlank()) {
            return "- 暂无优化建议。";
        }
        try {
            JSONArray array = JSONUtil.parseArray(jsonText);
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < array.size(); i++) {
                builder.append(i + 1).append(". ").append(String.valueOf(array.get(i))).append("\n");
            }
            return builder.length() > 0 ? builder.toString() : jsonText;
        } catch (Exception e) {
            return jsonText;
        }
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

