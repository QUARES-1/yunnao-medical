package com.neusoft.ai.controller;

import com.neusoft.ai.common.context.UserContext;
import com.neusoft.ai.common.result.Result;
import com.neusoft.ai.entity.MedicalRecordAiGenerate;
import com.neusoft.ai.service.ai.AiMedicalRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/medical-record/ai")
@RequiredArgsConstructor
@Tag(name = "AI病历生成", description = "AI病历自动生成模块")
public class AiMedicalRecordController {

    private final AiMedicalRecordService medicalRecordService;

    @PostMapping("/generate")
    @Operation(summary = "AI生成病历", description = "医生输入患者信息和症状，AI生成规范病历")
    public Result<Map<String, Object>> generateRecord(@RequestBody Map<String, Object> request) {
        Long patientId = request.get("patientId") != null ? ((Number) request.get("patientId")).longValue() : null;
        String inputText = (String) request.get("inputText");
        String inputType = (String) request.getOrDefault("inputType", "text");
        Long doctorId = UserContext.getUserId();
        return Result.success(medicalRecordService.generateRecord(patientId, inputText, inputType, doctorId));
    }

    @GetMapping("/generate-list")
    @Operation(summary = "生成记录列表", description = "医生的AI病历生成历史")
    public Result<Page<MedicalRecordAiGenerate>> getGenerateList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long doctorId = UserContext.getUserId();
        return Result.success(medicalRecordService.getGenerateList(doctorId, page, size));
    }

    @GetMapping("/generate/{id}")
    @Operation(summary = "生成记录详情", description = "查看AI病历生成详情")
    public Result<MedicalRecordAiGenerate> getGenerateDetail(@PathVariable Long id) {
        return Result.success(medicalRecordService.getGenerateDetail(id));
    }
}
