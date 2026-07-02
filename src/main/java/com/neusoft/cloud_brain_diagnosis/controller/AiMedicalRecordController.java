package com.neusoft.cloud_brain_diagnosis.controller;

import cn.hutool.json.JSONUtil;
import com.neusoft.cloud_brain_diagnosis.common.annotation.RequireLogin;
import com.neusoft.cloud_brain_diagnosis.common.context.UserContext;
import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import com.neusoft.cloud_brain_diagnosis.entity.MedicalRecordAiGenerate;
import com.neusoft.cloud_brain_diagnosis.service.ai.AiMedicalRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/medical-record/ai")
@RequiredArgsConstructor
@Tag(name = "AI病历生成", description = "AI病历自动生成模块")
public class AiMedicalRecordController {

    private final AiMedicalRecordService aiMedicalRecordService;

    /**
     * AI生成病历：普通 JSON 方式，给不支持 SSE 的前端兜底使用。
     */
    @PostMapping("/generate")
    @RequireLogin(RoleEnum.DOCTOR)
    @Operation(summary = "AI生成病历", description = "医生输入口述内容或关键词，AI生成主诉、现病史、既往史、体格检查、诊断和治疗意见")
    public Result<Map<String, Object>> generateRecord(@RequestBody Map<String, Object> request) {
        Long doctorId = UserContext.getUserId();
        Long patientId = toLong(request.get("patientId"));
        String inputText = String.valueOf(request.getOrDefault("inputText", ""));
        String inputType = String.valueOf(request.getOrDefault("inputType", "symptom"));
        return Result.success(aiMedicalRecordService.generateRecord(patientId, inputText, inputType, doctorId));
    }

    /**
     * AI生成病历：真正 SSE 流式输出。
     * 前端逐字接收 event:data，格式为 {field, char}，生成完成后接收 event:complete。
     */
    @PostMapping(value = "/generate-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequireLogin(RoleEnum.DOCTOR)
    @Operation(summary = "AI流式生成病历", description = "通过 SSE 将 AI 生成的病历字段逐字推送给医生端")
    public SseEmitter generateRecordStream(@RequestBody Map<String, Object> request) {
        SseEmitter emitter = new SseEmitter(120_000L);
        Long doctorId = UserContext.getUserId();
        Long patientId = toLong(request.get("patientId"));
        String inputText = String.valueOf(request.getOrDefault("inputText", ""));
        String inputType = String.valueOf(request.getOrDefault("inputType", "symptom"));

        new Thread(() -> {
            try {
                if (inputText == null || inputText.trim().isEmpty()) {
                    sendError(emitter, "请输入患者症状、口述内容或关键词");
                    return;
                }

                Map<String, Object> result = aiMedicalRecordService.generateRecord(patientId, inputText, inputType, doctorId);
                Map<String, String> fieldLabels = new LinkedHashMap<>();
                fieldLabels.put("chiefComplaint", "主诉");
                fieldLabels.put("presentIllness", "现病史");
                fieldLabels.put("pastHistory", "既往史");
                fieldLabels.put("physicalExamination", "体格检查");
                fieldLabels.put("diagnosis", "诊断结果");
                fieldLabels.put("treatment", "治疗意见");

                for (String field : fieldLabels.keySet()) {
                    String value = String.valueOf(result.getOrDefault(field, ""));
                    if (value == null || "null".equals(value)) value = "";
                    for (int i = 0; i < value.length(); i++) {
                        Map<String, Object> delta = new HashMap<>();
                        delta.put("field", field);
                        delta.put("char", String.valueOf(value.charAt(i)));
                        emitter.send(SseEmitter.event().name("data").data(JSONUtil.toJsonStr(delta)));
                        Thread.sleep(12L);
                    }
                }

                Map<String, Object> complete = new HashMap<>();
                complete.put("status", "complete");
                complete.put("id", result.get("id"));
                emitter.send(SseEmitter.event().name("complete").data(JSONUtil.toJsonStr(complete)));
                emitter.complete();
            } catch (Exception e) {
                sendError(emitter, e.getMessage() == null ? "AI病历流式生成失败" : e.getMessage());
            }
        }, "ai-medical-record-sse").start();

        return emitter;
    }

    /**
     * 生成记录列表
     */
    @GetMapping("/generate-list")
    @RequireLogin(RoleEnum.DOCTOR)
    @Operation(summary = "生成记录列表", description = "医生的历史 AI 病历生成记录")
    public Result<Page<MedicalRecordAiGenerate>> getGenerateList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(aiMedicalRecordService.getGenerateList(UserContext.getUserId(), page, size));
    }

    /**
     * 生成详情
     */
    @GetMapping("/generate/{id}")
    @RequireLogin(RoleEnum.DOCTOR)
    @Operation(summary = "生成详情", description = "AI 病历生成记录详情")
    public Result<MedicalRecordAiGenerate> getGenerateDetail(@PathVariable Long id) {
        return Result.success(aiMedicalRecordService.getGenerateDetail(id));
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.longValue();
        try {
            String text = String.valueOf(value).trim();
            if (text.isEmpty() || "null".equalsIgnoreCase(text)) return null;
            return Long.parseLong(text);
        } catch (Exception e) {
            return null;
        }
    }

    private void sendError(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().name("error").data(message));
        } catch (IOException ignored) {
        } finally {
            emitter.complete();
        }
    }
}