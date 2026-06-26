package com.neusoft.cloud_brain_diagnosis.controller;

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
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/medical-record/ai")
@RequiredArgsConstructor
@Tag(name = "AI病历生成", description = "AI病历自动生成模块")
public class AiMedicalRecordController {

    private final AiMedicalRecordService medicalRecordService;

    /**
     * AI生成病历
     */
    @PostMapping("/generate")
    @RequireLogin(RoleEnum.DOCTOR)
    @Operation(summary = "AI生成病历", description = "医生输入对话/关键词，AI生成病历草稿")
    public Result<Map<String, Object>> generateRecord(@RequestBody Map<String, Object> request) {
        Long patientId = request.get("patientId") != null ? ((Number) request.get("patientId")).longValue() : null;
        String inputText = (String) request.get("inputText");
        String inputType = (String) request.getOrDefault("inputType", "keyword");
        Long doctorId = UserContext.getUserId();
        return Result.success(medicalRecordService.generateRecord(patientId, inputText, inputType, doctorId));
    }

    /**
     * 生成记录列表
     */
    @GetMapping("/generate-list")
    @RequireLogin(RoleEnum.DOCTOR)
    @Operation(summary = "生成记录列表", description = "医生的历史生成记录")
    public Result<Page<MedicalRecordAiGenerate>> getGenerateList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long doctorId = UserContext.getUserId();
        return Result.success(medicalRecordService.getGenerateList(doctorId, page, size));
    }

    /**
     * 生成详情
     */
    @GetMapping("/generate/{id}")
    @RequireLogin(RoleEnum.DOCTOR)
    @Operation(summary = "生成详情", description = "生成记录详情")
    public Result<MedicalRecordAiGenerate> getGenerateDetail(@PathVariable Long id) {
        return Result.success(medicalRecordService.getGenerateDetail(id));
    }
}
