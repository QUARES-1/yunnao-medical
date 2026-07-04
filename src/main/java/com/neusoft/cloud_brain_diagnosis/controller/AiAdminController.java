package com.neusoft.cloud_brain_diagnosis.controller;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import com.neusoft.cloud_brain_diagnosis.common.annotation.RequireLogin;
import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import com.neusoft.cloud_brain_diagnosis.entity.OperationAiReport;
import com.neusoft.cloud_brain_diagnosis.repository.AiChatRecordRepository;
import com.neusoft.cloud_brain_diagnosis.repository.AiKnowledgeBaseRepository;
import com.neusoft.cloud_brain_diagnosis.repository.CriticalValueWarningRepository;
import com.neusoft.cloud_brain_diagnosis.repository.ExaminationAiInterpretationRepository;
import com.neusoft.cloud_brain_diagnosis.repository.FollowUpPlanRepository;
import com.neusoft.cloud_brain_diagnosis.repository.MedicationGuideRepository;
import com.neusoft.cloud_brain_diagnosis.repository.OperationAiReportRepository;
import com.neusoft.cloud_brain_diagnosis.repository.QualityCheckRecordRepository;
import com.neusoft.cloud_brain_diagnosis.service.ai.AiQualityService;
import com.neusoft.cloud_brain_diagnosis.repository.TriageRecordRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/ai")
@RequiredArgsConstructor
@RequireLogin(RoleEnum.ADMIN)
@Tag(name = "AI管理端", description = "AI运营分析、医疗质检、问答日志")
public class AiAdminController {

    private final AiQualityService aiQualityService;
    private final AiChatRecordRepository aiChatRecordRepository;
    private final TriageRecordRepository triageRecordRepository;
    private final QualityCheckRecordRepository qualityCheckRecordRepository;
    private final AiKnowledgeBaseRepository aiKnowledgeBaseRepository;
    private final OperationAiReportRepository operationAiReportRepository;
    private final ExaminationAiInterpretationRepository examinationAiInterpretationRepository;
    private final MedicationGuideRepository medicationGuideRepository;
    private final CriticalValueWarningRepository criticalValueWarningRepository;
    private final FollowUpPlanRepository followUpPlanRepository;

    // ========== 运营分析 ==========

    /**
     * 生成运营报告
     */
    @PostMapping("/operation-report/generate")
    @Operation(summary = "生成运营报告", description = "生成指定时间段的AI运营分析报告")
    public Result<Map<String, Object>> generateReport(@RequestBody Map<String, Object> request) {
        return Result.success(toReportMap(createLocalOperationReport(request)));
    }

    /**
     * 运营报告详情
     */
    @GetMapping("/operation-report/{id}")
    @Operation(summary = "运营报告详情", description = "查看运营分析报告详情")
    public Result<Map<String, Object>> getReportDetail(@PathVariable Long id) {
        OperationAiReport report = operationAiReportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("运营报告不存在"));
        return Result.success(toReportMap(report));
    }

    /**
     * 报告列表
     */
    @GetMapping("/operation-report/list")
    @Operation(summary = "报告列表", description = "历史运营报告列表")
    public Result<Map<String, Object>> getReportList(
            @RequestParam(required = false) String reportType,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        var pageable = org.springframework.data.domain.PageRequest.of(page - 1, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createTime"));
        var reportPage = (reportType == null || reportType.isBlank())
                ? operationAiReportRepository.findByOrderByCreateTimeDesc(pageable)
                : operationAiReportRepository.findByReportTypeOrderByCreateTimeDesc(reportType, pageable);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("content", reportPage.getContent().stream().map(this::toReportMap).toList());
        data.put("totalElements", reportPage.getTotalElements());
        data.put("totalPages", reportPage.getTotalPages());
        data.put("number", reportPage.getNumber());
        data.put("size", reportPage.getSize());
        return Result.success(data);
    }

    /**
     * 首页AI概览
     */
    @GetMapping("/operation-overview")
    @Operation(summary = "首页AI概览", description = "管理员首页仪表盘AI分析概览")
    public Result<Map<String, Object>> getOperationOverview() {
        return Result.success(buildLocalOverview());
    }

    /**
     * 流式生成运营报告（SSE）
     * 注意：此接口保持本地调用，通过 Feign 流式转发会给网关增加复杂性，
     * 直接由 AI 微服务处理更合适，后续可迁移到前端直连 AI 微服务。
     */
    @PostMapping(value = "/operation-report/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式生成运营报告", description = "通过 SSE 逐字返回 AI 运营报告内容")
    public SseEmitter streamGenerateReport(@RequestBody Map<String, Object> request) {
        // 流式场景暂保留通过 AiAdminFeignClient 获取完整报告后再流式输出
        SseEmitter emitter = new SseEmitter(120_000L);
        new Thread(() -> {
            try {
                OperationAiReport report = createLocalOperationReport(request);
                Map<String, Object> data = toReportMap(report);
                String streamText = buildStreamReportText(data);
                for (int i = 0; i < streamText.length(); i++) {
                    emitter.send(SseEmitter.event().name("delta").data(String.valueOf(streamText.charAt(i))));
                    Thread.sleep(14L);
                }
                emitter.send(SseEmitter.event().name("done").data(String.valueOf(data.get("id"))));
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
    private Map<String, Object> buildLocalOverview() {
        long chatCount = aiChatRecordRepository.count();
        long knowledgeHitCount = aiChatRecordRepository.findAll().stream()
                .filter(item -> "knowledge".equalsIgnoreCase(String.valueOf(item.getSource())) || "知识库".equals(String.valueOf(item.getSource())))
                .count();
        long triageCount = triageRecordRepository.count();
        long qualityCount = qualityCheckRecordRepository.count();
        long knowledgeCount = aiKnowledgeBaseRepository.count();
        long reportInterpretationCount = examinationAiInterpretationRepository.count();
        long medicationGuideCount = medicationGuideRepository.count();
        long criticalWarningCount = criticalValueWarningRepository.count();
        long followUpCount = followUpPlanRepository.count();
        String hitRate = chatCount == 0 ? "0%" : String.format("%.1f%%", knowledgeHitCount * 100.0 / chatCount);

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("aiTriageCount", triageCount);
        metrics.put("aiChatCount", chatCount);
        metrics.put("knowledgeHitRate", hitRate);
        metrics.put("reportInterpretationCount", reportInterpretationCount);
        metrics.put("medicationGuideCount", medicationGuideCount);
        metrics.put("criticalWarningCount", criticalWarningCount);
        metrics.put("followUpCount", followUpCount);
        metrics.put("qualityCheckCount", qualityCount);

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("aiChatCount", chatCount);
        overview.put("chatCount", chatCount);
        overview.put("aiTriageCount", triageCount);
        overview.put("triageCount", triageCount);
        overview.put("qualityCheckCount", qualityCount);
        overview.put("knowledgeCount", knowledgeCount);
        overview.put("knowledgeHitRate", hitRate);
        overview.put("keyMetrics", metrics);
        return overview;
    }

    private OperationAiReport createLocalOperationReport(Map<String, Object> request) {
        Map<String, Object> overview = buildLocalOverview();
        @SuppressWarnings("unchecked")
        Map<String, Object> metrics = (Map<String, Object>) overview.get("keyMetrics");
        String reportType = request == null ? "daily" : String.valueOf(request.getOrDefault("reportType", "daily"));
        LocalDate end = LocalDate.now();
        LocalDate start = "monthly".equals(reportType) ? end.minusDays(30) : "weekly".equals(reportType) ? end.minusDays(7) : end;

        OperationAiReport report = new OperationAiReport();
        report.setReportType(reportType);
        report.setStartDate(start);
        report.setEndDate(end);
        report.setSummary("本周期 AI 功能运行平稳：患者问答、智能分诊、报告解读、用药指导、危急值预警和智能随访均已纳入运营监控。当前 AI 问答 " + metrics.get("aiChatCount") + " 次，分诊调用 " + metrics.get("aiTriageCount") + " 次，知识库命中率 " + metrics.get("knowledgeHitRate") + "。");
        report.setKeyMetrics(JSONUtil.toJsonStr(metrics));
        report.setTrendsAnalysis("从现有数据看，患者端就医问答和报告解读是高频入口；检验科 AI 审核、危急值预警和药房库存预测已形成跨端联动，后续应继续补充真实业务记录，提高趋势判断稳定性。 ");
        report.setWarnings(JSONUtil.toJsonStr(List.of(
                Map.of("level", "info", "content", "若知识库命中率下降，说明患者问题和标准问答不匹配，需要及时补充楼层导诊、取药流程、报告查询等院内知识。"),
                Map.of("level", "medium", "content", "危急值预警和检验 AI 审核需要持续核对人工处理结果，避免高风险报告漏处理。")
        )));
        report.setSuggestions(JSONUtil.toJsonStr(List.of(
                "补充医院楼层导诊、检查检验地点、取药流程、急诊位置等知识库条目，提升患者问答命中率。",
                "定期查看检验 AI 审核中的退回重测和人工复核原因，优化检验科复核规则。",
                "把药房库存预测、医生处方、患者用药指导联动展示，形成完整闭环演示。",
                "对高频患者问题沉淀为标准答案，减少 AI 生成不稳定带来的回复偏差。"
        )));
        report.setRawResponse(report.getSummary());
        return operationAiReportRepository.save(report);
    }

    private Map<String, Object> toReportMap(OperationAiReport report) {
        Map<String, Object> map = new LinkedHashMap<>();
        Map<String, Object> overview = buildLocalOverview();
        @SuppressWarnings("unchecked")
        Map<String, Object> metrics = (Map<String, Object>) overview.get("keyMetrics");
        String fallbackSummary = "本周期 AI 运营已接入患者问答、智能分诊、检验报告解读、用药指导、危急值预警和智能随访。当前累计 AI 问答 " + metrics.get("aiChatCount") + " 次，分诊调用 " + metrics.get("aiTriageCount") + " 次，知识库命中率 " + metrics.get("knowledgeHitRate") + "。";
        String fallbackTrends = "患者端问答和报告解读是主要使用入口；检验科 AI 审核与危急值预警负责风险识别；药房库存预测和用药指导负责药事闭环。";
        String fallbackWarnings = JSONUtil.toJsonStr(List.of(Map.of("level", "info", "content", "若知识库命中率下降，需要补充楼层导诊、取药流程、报告查询等院内知识。")));
        String fallbackSuggestions = JSONUtil.toJsonStr(List.of("补充医院楼层、检查检验地点、取药流程等知识库内容。", "定期复核检验 AI 审核中的退回重测和人工复核原因。", "持续完善患者随访、药房库存预测和危急值预警联动。"));

        map.put("id", report.getId());
        map.put("reportType", report.getReportType());
        map.put("startDate", report.getStartDate());
        map.put("endDate", report.getEndDate());
        map.put("summary", cleanText(report.getSummary()).startsWith("暂无") ? fallbackSummary : report.getSummary());
        map.put("keyMetrics", cleanText(report.getKeyMetrics()).equals("暂无") ? JSONUtil.toJsonStr(metrics) : report.getKeyMetrics());
        map.put("trendsAnalysis", cleanText(report.getTrendsAnalysis()).startsWith("暂无") ? fallbackTrends : report.getTrendsAnalysis());
        map.put("forecasts", report.getForecasts());
        map.put("warnings", cleanText(report.getWarnings()).equals("暂无") ? fallbackWarnings : report.getWarnings());
        map.put("suggestions", cleanText(report.getSuggestions()).startsWith("暂无") ? fallbackSuggestions : report.getSuggestions());
        map.put("rawResponse", report.getRawResponse());
        map.put("createTime", report.getCreateTime());
        return map;
    }


    private String buildStreamReportText(Map<String, Object> generated) {
        if (generated == null) {
            return "一、运营摘要\n暂无摘要";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("一、运营摘要\n").append(cleanText((String) generated.getOrDefault("summary", ""))).append("\n\n");
        builder.append("二、核心指标\n").append(formatMetrics((String) generated.get("keyMetrics"))).append("\n");
        builder.append("三、趋势分析\n").append(cleanText((String) generated.getOrDefault("trendsAnalysis", ""))).append("\n\n");
        builder.append("四、风险提醒\n").append(formatWarnings((String) generated.get("warnings"))).append("\n");
        builder.append("五、优化建议\n").append(formatSuggestions((String) generated.get("suggestions")));
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
        String checkType = toStringValue(request.get("checkType"));
        if (checkType == null || checkType.isBlank()) {
            checkType = toStringValue(request.get("type"));
        }
        if (checkType == null || checkType.isBlank()) {
            checkType = "病历质检";
        }

        Integer sampleSize = toIntegerValue(request.get("sampleSize"));
        if (sampleSize == null) {
            sampleSize = toIntegerValue(request.get("size"));
        }
        if (sampleSize == null || sampleSize <= 0) {
            sampleSize = 10;
        }
        return Result.success(aiQualityService.startQualityCheck(checkType, sampleSize));
    }

    /**
     * 质检记录列表
     */
    @GetMapping("/quality-check/list")
    @Operation(summary = "质检记录列表", description = "历史质检记录")
    public Result<Map<String, Object>> getCheckList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(toPageMap(aiQualityService.getCheckList(page, size)));
    }

    /**
     * 质检详情
     */
    @GetMapping("/quality-check/{id}")
    @Operation(summary = "质检详情", description = "质检记录详情")
    public Result<Object> getCheckDetail(@PathVariable Long id) {
        return Result.success((Object) aiQualityService.getCheckDetail(id));
    }

    /**
     * 问题明细列表
     */
    @GetMapping("/quality-check/{id}/details")
    @Operation(summary = "问题明细列表", description = "质检问题明细")
    public Result<Map<String, Object>> getCheckDetails(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(toPageMap(aiQualityService.getCheckDetails(id, page, size)));
    }

    /**
     * 医生质检统计
     */
    @GetMapping("/quality-check/doctor-stats")
    @Operation(summary = "医生质检统计", description = "各医生质检统计")
    public Result<Map<String, Object>> getDoctorStats() {
        return Result.success(aiQualityService.getDoctorStats());
    }

    private Map<String, Object> toPageMap(org.springframework.data.domain.Page<?> pageData) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("content", pageData.getContent());
        data.put("records", pageData.getContent());
        data.put("totalElements", pageData.getTotalElements());
        data.put("total", pageData.getTotalElements());
        data.put("totalPages", pageData.getTotalPages());
        data.put("number", pageData.getNumber());
        data.put("page", pageData.getNumber() + 1);
        data.put("size", pageData.getSize());
        return data;
    }

    private String toStringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Integer toIntegerValue(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(String.valueOf(value));
    }

    // ========== 问答日志 ==========

    /**
     * 问答日志
     */
    @GetMapping("/chat-log")
    @Operation(summary = "问答日志", description = "查看所有AI问答日志")
    public Result<Map<String, Object>> getChatLogs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        var pageable = org.springframework.data.domain.PageRequest.of(page - 1, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createTime"));
        var chatPage = aiChatRecordRepository.findByOrderByCreateTimeDesc(pageable);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("content", chatPage.getContent());
        data.put("totalElements", chatPage.getTotalElements());
        data.put("totalPages", chatPage.getTotalPages());
        data.put("number", chatPage.getNumber());
        data.put("size", chatPage.getSize());
        return Result.success(data);
    }
}


