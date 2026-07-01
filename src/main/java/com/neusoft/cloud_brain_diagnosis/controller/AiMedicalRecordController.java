package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.annotation.RequireLogin;
import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import com.neusoft.cloud_brain_diagnosis.feign.AiMedicalRecordFeignClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/medical-record/ai")
@RequiredArgsConstructor
@Tag(name = "AI病历生成", description = "AI病历自动生成模块")
public class AiMedicalRecordController {

    private final AiMedicalRecordFeignClient medicalRecordFeignClient;

    /**
     * AI生成病历
     */
    @PostMapping("/generate")
    @RequireLogin(RoleEnum.DOCTOR)
    @Operation(summary = "AI生成病历", description = "医生输入对话/关键词，AI生成病历草稿")
    public Result<Map<String, Object>> generateRecord(@RequestBody Map<String, Object> request) {
        return medicalRecordFeignClient.generateRecord(request);
    }

    /**
     * 生成记录列表
     */
    @GetMapping("/generate-list")
    @RequireLogin(RoleEnum.DOCTOR)
    @Operation(summary = "生成记录列表", description = "医生的历史生成记录")
    public Result<Map<String, Object>> getGenerateList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return medicalRecordFeignClient.getGenerateList(page, size);
    }

    /**
     * 生成详情
     */
    @GetMapping("/generate/{id}")
    @RequireLogin(RoleEnum.DOCTOR)
    @Operation(summary = "生成详情", description = "生成记录详情")
    public Result<Map<String, Object>> getGenerateDetail(@PathVariable Long id) {
        return medicalRecordFeignClient.getGenerateDetail(id);
    }
}
