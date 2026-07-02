package com.neusoft.cloud_brain_diagnosis.controller;

import cn.hutool.json.JSONUtil;

import com.neusoft.cloud_brain_diagnosis.common.annotation.RequireLogin;
import com.neusoft.cloud_brain_diagnosis.common.context.UserContext;
import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import com.neusoft.cloud_brain_diagnosis.entity.CriticalValueWarning;
import com.neusoft.cloud_brain_diagnosis.entity.Examination;
import com.neusoft.cloud_brain_diagnosis.entity.ExaminationAiReview;
import com.neusoft.cloud_brain_diagnosis.feign.AiExaminationFeignClient;
import com.neusoft.cloud_brain_diagnosis.repository.CriticalValueWarningRepository;
import com.neusoft.cloud_brain_diagnosis.repository.ExaminationAiReviewRepository;
import com.neusoft.cloud_brain_diagnosis.repository.ExaminationRepository;
import com.neusoft.cloud_brain_diagnosis.service.ai.AiExaminationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
@RestController
@RequestMapping("/api/examination/ai")
@RequiredArgsConstructor
@Tag(name = "AI检验服务", description = "AI检验报告解读、危急值预警、检验结果智能审核")
public class ExaminationAiController {

    private final AiExaminationFeignClient examinationFeignClient;
    private final AiExaminationService examinationService;
    private final ExaminationRepository examinationRepository;
    private final CriticalValueWarningRepository criticalValueWarningRepository;
    private final ExaminationAiReviewRepository examinationAiReviewRepository;

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
        return examinationRepository.findById(id)
                .map(examination -> Result.success(buildPatientInterpretation(examination)))
                .orElseGet(() -> Result.error("检查报告不存在"));
    }

    private Map<String, Object> buildPatientInterpretation(Examination examination) {
        String itemName = safe(examination.getItemName());
        String resultText = safe(examination.getResult());
        List<Map<String, Object>> abnormalItems = parseAbnormalItems(resultText);

        Map<String, Object> result = new HashMap<>();
        result.put("examinationId", examination.getId());
        result.put("itemName", itemName);
        result.put("abnormalItems", abnormalItems);
        result.put("interpretationPatient", buildPlainInterpretation(itemName, resultText, abnormalItems));
        result.put("suggestions", buildSuggestion(itemName, abnormalItems));
        result.put("reviewReminder", buildReviewReminder(itemName, abnormalItems));
        return result;
    }

    private List<Map<String, Object>> parseAbnormalItems(String resultText) {
        List<Map<String, Object>> items = new ArrayList<>();
        if (resultText == null || resultText.isBlank()) {
            return items;
        }

        String[] segments = resultText.split("[；;\\n]");
        Pattern pattern = Pattern.compile("^\\s*([^:：()（）]+)(?:[（(]([^）)]+)[）)])?\\s*[:：]\\s*([^，,；;]+)(?:[，,]\\s*参考范围\\s*([^，,；;。]+))?.*?(危急偏高|危急偏低|偏高|偏低|升高|降低|正常)?\\s*。?$");
        for (String segment : segments) {
            String text = segment.trim();
            if (text.isEmpty()) continue;
            Matcher matcher = pattern.matcher(text);
            if (!matcher.find()) continue;

            String status = normalizeStatus(firstNonBlank(matcher.group(5), inferStatus(text)));
            if ("正常".equals(status) || status.isBlank()) continue;

            String name = matcher.group(1).trim();
            String code = safe(matcher.group(2));
            String rawValue = matcher.group(3).trim();
            String value = rawValue;
            String unit = "";
            Matcher valueMatcher = Pattern.compile("^(.+?)\\s+([^\\s]+)$").matcher(rawValue);
            if (valueMatcher.find()) {
                value = valueMatcher.group(1).trim();
                unit = valueMatcher.group(2).trim();
            }

            Map<String, Object> item = new HashMap<>();
            item.put("name", code.isBlank() ? name : name + "（" + code + "）");
            item.put("value", value);
            item.put("unit", unit);
            item.put("reference", safe(matcher.group(4), "未填写"));
            item.put("status", status);
            items.add(item);
        }
        return items;
    }

    private String buildPlainInterpretation(String itemName, String resultText, List<Map<String, Object>> abnormalItems) {
        if (itemName.contains("血常规")) {
            if (abnormalItems.isEmpty()) {
                return "本次血常规未见明显异常。白细胞、红细胞、血红蛋白和血小板等指标整体处于可接受范围，可结合症状继续观察。";
            }
            return "本次血常规存在部分异常指标，常见于感染、炎症反应或贫血等情况。需要结合发热、咳嗽、乏力、出血倾向等症状，由医生综合判断。";
        }
        if (itemName.contains("胸") || itemName.contains("X线") || itemName.contains("CT")) {
            if (resultText.contains("未见明显") || resultText.contains("正常")) {
                return "本次影像检查未提示明显实变影或严重异常，胸部情况总体平稳。若仍有咳嗽、胸闷或发热，需要结合症状继续观察。";
            }
            return "本次影像检查提示胸部可能存在炎症、纹理增多或其他影像改变，需要结合呼吸道症状和医生查体进一步判断。";
        }
        if (itemName.contains("C反应蛋白") || itemName.toUpperCase().contains("CRP")) {
            if (abnormalItems.isEmpty()) {
                return "C反应蛋白是炎症相关指标。本次结果未见明显升高，暂不支持明显急性炎症反应。";
            }
            return "C反应蛋白偏高，提示体内可能存在炎症或感染反应。它不能单独判断病因，需要结合血常规、体温和临床症状一起分析。";
        }
        if (itemName.contains("肝功能")) {
            return abnormalItems.isEmpty()
                    ? "本次肝功能主要指标未见明显异常，提示肝细胞损伤或胆红素代谢异常证据不明显。"
                    : "本次肝功能存在异常指标，可能与肝细胞损伤、胆汁代谢异常、药物影响或饮酒等因素有关。";
        }
        if (itemName.contains("肾功能")) {
            return abnormalItems.isEmpty()
                    ? "本次肾功能指标整体平稳，暂未提示明显肾功能受损。"
                    : "本次肾功能存在异常指标，可能与脱水、肾脏负担增加或基础肾病有关，需要医生结合病史判断。";
        }
        if (itemName.contains("血糖")) {
            return abnormalItems.isEmpty()
                    ? "本次血糖结果未见明显异常，可继续保持规律饮食和运动。"
                    : "本次血糖结果异常，需要结合空腹/餐后状态判断，并关注糖尿病或低血糖风险。";
        }
        if (abnormalItems.isEmpty()) {
            return "本次检查结果未见明显异常。建议结合自身症状和医生意见判断是否需要进一步处理。";
        }
        return "本次检查存在异常指标，需要结合症状、既往病史和医生查体综合判断。";
    }

    private String buildSuggestion(String itemName, List<Map<String, Object>> abnormalItems) {
        if (abnormalItems.isEmpty()) {
            return "若没有明显不适，可按医嘱观察；如症状持续、加重或出现新症状，请及时复诊。";
        }
        if (itemName.contains("胸") || itemName.contains("X线") || itemName.contains("CT")) {
            return "建议携带影像报告到呼吸内科或开单医生处复诊，由医生结合症状判断是否需要治疗或复查影像。";
        }
        if (itemName.contains("血常规") || itemName.contains("C反应蛋白")) {
            return "建议携带报告到内科或呼吸内科复诊，医生会结合体温、咳嗽咽痛等症状决定是否用药。";
        }
        return "建议携带报告到开单医生处复诊，由医生结合病情判断是否需要进一步检查或治疗。";
    }

    private String buildReviewReminder(String itemName, List<Map<String, Object>> abnormalItems) {
        if (abnormalItems.isEmpty()) {
            return "如无明显不适，可按医生安排常规复查；若症状变化，请提前就诊。";
        }
        if (itemName.contains("胸") || itemName.contains("X线") || itemName.contains("CT")) {
            return "若咳嗽、发热或胸闷持续，建议 1-2 周内按医生建议复查或进一步检查。";
        }
        if (itemName.contains("C反应蛋白")) {
            return "建议治疗或观察 3-5 天后复查 C反应蛋白，观察炎症是否下降。";
        }
        if (itemName.contains("血常规")) {
            return "建议 3-5 天后复查血常规，观察白细胞、血红蛋白等指标变化。";
        }
        return "建议按医生要求复查相关指标，观察异常项目是否恢复。";
    }

    private String inferStatus(String text) {
        if (text.contains("危急偏高")) return "危急偏高";
        if (text.contains("危急偏低")) return "危急偏低";
        if (text.contains("偏高") || text.contains("升高")) return "偏高";
        if (text.contains("偏低") || text.contains("降低")) return "偏低";
        if (text.contains("正常")) return "正常";
        return "";
    }

    private String normalizeStatus(String status) {
        return safe(status).replace("升高", "偏高").replace("降低", "偏低");
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : safe(second);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String safe(String value, String fallback) {
        String safeValue = safe(value);
        return safeValue.isBlank() ? fallback : safeValue;
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
    public Result<Page<CriticalValueWarning>> getCriticalList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        PageRequest pageRequest = PageRequest.of(
                Math.max(page - 1, 0),
                size,
                Sort.by(Sort.Direction.DESC, "createTime"));
        return Result.success(criticalValueWarningRepository
                .findByPatientIdOrderByCreateTimeDesc(UserContext.getUserId(), pageRequest));
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
        return Result.success(examinationService.reviewExamination(id, UserContext.getUserId()));
    }

    /**
     * AI流式审核检验结果
     */
    @PostMapping(value = "/review/stream/{id}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequireLogin(RoleEnum.LAB)
    @Operation(summary = "AI流式审核结果", description = "通过SSE逐字输出检验AI审核过程")
    public SseEmitter streamReviewExamination(@PathVariable Long id) {
        SseEmitter emitter = new SseEmitter(120_000L);
        Long labStaffId = UserContext.getUserId();
        CompletableFuture.runAsync(() -> {
            try {
                Examination exam = examinationRepository.findById(id).orElse(null);
                Map<String, Object> result = examinationService.reviewExamination(id, labStaffId);
                ExaminationAiReview review = examinationAiReviewRepository.findByExaminationId(id).orElse(null);
                String text = buildReviewStreamText(exam, review, result);
                emitter.send(SseEmitter.event().name("start").data("AI开始审核检验报告"));
                for (int i = 0; i < text.length(); i++) {
                    emitter.send(SseEmitter.event().name("delta").data(String.valueOf(text.charAt(i))));
                    Thread.sleep(18);
                }
                emitter.send(SseEmitter.event().name("done").data("审核完成"));
                emitter.complete();
            } catch (Exception e) {
                sendSseError(emitter, "AI流式审核失败：" + e.getMessage());
            }
        });
        return emitter;
    }

    /**
     * 待人工复核列表
     */
    @GetMapping("/manual-list")
    @RequireLogin(RoleEnum.LAB)
    @Operation(summary = "待人工复核", description = "需要人工复核的列表")
    public Result<Page<Map<String, Object>>> getManualList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(examinationService.getManualList(page, size).map(this::enrichReview));
    }

    /**
     * 审核记录列表
     */
    @GetMapping("/review-list")
    @RequireLogin(RoleEnum.LAB)
    @Operation(summary = "审核记录列表", description = "查询AI审核记录，可按结论筛选：pass/manual/reject")
    public Result<Page<Map<String, Object>>> getReviewList(
            @RequestParam(required = false) String reviewResult,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(examinationService.getReviewList(reviewResult, page, size).map(this::enrichReview));
    }

    /**
     * 审核详情
     */
    @GetMapping("/review-detail/{id}")
    @RequireLogin({RoleEnum.LAB, RoleEnum.DOCTOR})
    @Operation(summary = "审核详情", description = "审核记录详情")
    public Result<Map<String, Object>> getReviewDetail(@PathVariable Long id) {
        return Result.success(enrichReview(examinationService.getReviewDetail(id)));
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

    private Map<String, Object> enrichReview(ExaminationAiReview review) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", review.getId());
        map.put("examinationId", review.getExaminationId());
        map.put("patientId", review.getPatientId());
        map.put("labStaffId", review.getLabStaffId());
        map.put("reviewResult", review.getReviewResult());
        map.put("reviewScore", review.getReviewScore());
        map.put("abnormalItems", review.getAbnormalItems());
        map.put("logicIssues", review.getLogicIssues());
        map.put("historyCompare", review.getHistoryCompare());
        map.put("warnings", review.getWarnings());
        map.put("suggestions", review.getSuggestions());
        map.put("rawResponse", review.getRawResponse());
        map.put("reviewTime", review.getReviewTime());
        examinationRepository.findById(review.getExaminationId()).ifPresent(exam -> {
            map.put("itemName", safe(exam.getItemName(), "未关联检查项目"));
            map.put("patientName", safe(exam.getPatientName()));
            map.put("doctorName", trimDoctorName(exam.getDoctorName()));
            map.put("result", safe(exam.getResult()));
            map.put("examinationStatus", safe(exam.getStatus()));
        });
        return map;
    }

    private String buildReviewStreamText(Examination exam, ExaminationAiReview review, Map<String, Object> result) {
        String itemName = exam == null ? "未关联检查项目" : safe(exam.getItemName(), "未关联检查项目");
        String reportNo = exam == null ? "EX" : "EX" + String.format("%06d", exam.getId());
        String reviewResult = review == null ? safe(String.valueOf(result.get("reviewResult"))) : safe(review.getReviewResult());
        String conclusion = "pass".equals(reviewResult) ? "自动通过" : "reject".equals(reviewResult) ? "退回重测" : "人工复核";
        String abnormal = review == null ? summarizeAny(result.get("abnormalItems"), "未发现明显异常指标") : summarizeJsonText(review.getAbnormalItems(), "未发现明显异常指标");
        String logic = review == null ? summarizeAny(result.get("logicIssues"), "项目间逻辑关系未发现明显矛盾") : summarizeJsonText(review.getLogicIssues(), "项目间逻辑关系未发现明显矛盾");
        String history = review == null ? summarizeAny(result.get("historyCompare"), "暂无明显异常波动") : summarizeJsonText(review.getHistoryCompare(), "暂无明显异常波动");
        String warnings = review == null ? summarizeAny(result.get("warnings"), "暂无高风险警告") : summarizeJsonText(review.getWarnings(), "暂无高风险警告");
        String suggestions = review == null ? safe(String.valueOf(result.get("suggestions")), "按审核结论处理。") : safe(review.getSuggestions(), "按审核结论处理，必要时由检验师复核。");

        StringBuilder builder = new StringBuilder();
        builder.append("检查项目：").append(itemName).append("\n");
        builder.append("报告编号：").append(reportNo).append("\n");
        builder.append("审核结论：").append(conclusion).append("\n\n");
        builder.append("一、结论原因\n");
        if ("reject".equals(reviewResult)) {
            builder.append("本报告被退回重测，是因为 AI 发现结果存在明显不合理或风险较高的情况，不能直接发布。需要重新采样或重新检测，避免把异常机器值误当作真实病情。\n");
        } else if ("manual".equals(reviewResult)) {
            builder.append("本报告需要人工复核，是因为部分指标异常、历史波动或项目间逻辑关系需要检验师进一步确认。复核通过后才建议发布。\n");
        } else {
            builder.append("本报告自动通过，是因为主要指标处于参考范围内，项目间逻辑关系合理，未发现需要人工干预的高风险问题。\n");
        }
        builder.append("\n二、重点指标\n").append(toBulletLines(abnormal));
        builder.append("\n三、逻辑合理性校验\n").append(toBulletLines(logic));
        builder.append("\n四、历史结果对比\n").append(toBulletLines(history));
        builder.append("\n五、风险提示\n").append(toBulletLines(warnings));
        builder.append("\n六、处理建议\n- ").append(suggestions);
        return builder.toString();
    }

    private String summarizeAny(Object value, String emptyText) {
        if (value == null) return emptyText;
        return summarizeJsonText(JSONUtil.toJsonStr(value), emptyText);
    }

    private String toBulletLines(String text) {
        String cleaned = safe(text);
        if (cleaned.isBlank()) return "- 暂无\n";
        String[] parts = cleaned.split("；|\\n");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            String item = part.trim();
            if (!item.isBlank()) builder.append("- ").append(item).append("\n");
        }
        return builder.length() == 0 ? "- " + cleaned + "\n" : builder.toString();
    }
    private String summarizeJsonText(String value, String emptyText) {
        String text = safe(value);
        if (text.isBlank() || "[]".equals(text)) return emptyText;
        return text.replace("[{", "").replace("}]", "").replace("},{", "；").replace("\"", "").replace(":", "：").replace(",", "，");
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) return number.longValue();
        if (value == null) return null;
        try { return Long.parseLong(String.valueOf(value)); } catch (Exception e) { return null; }
    }

    private String trimDoctorName(String value) {
        String name = safe(value);
        return name.endsWith("医生") ? name.substring(0, name.length() - 2) : name;
    }

    private String jsonEscape(String value) {
        return safe(value).replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void sendSseError(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().name("error").data(message));
        } catch (IOException ignored) {
        } finally {
            emitter.complete();
        }
    }
}




