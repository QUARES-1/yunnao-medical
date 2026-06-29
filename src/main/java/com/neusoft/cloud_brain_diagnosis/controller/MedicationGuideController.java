package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.annotation.RequireLogin;
import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import com.neusoft.cloud_brain_diagnosis.service.ai.AiMedicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/medication/guide")
@RequiredArgsConstructor
@Tag(name = "AI用药指导", description = "AI个性化用药指导模块")
public class MedicationGuideController {

    private final AiMedicationService medicationService;

    /**
     * 生成用药指导
     */
    @PostMapping("/generate")
    @RequireLogin({RoleEnum.PHARMACY, RoleEnum.DOCTOR, RoleEnum.PATIENT})
    @Operation(summary = "生成用药指导", description = "根据处方生成AI个性化用药指导")
    public Result<Map<String, Object>> generateGuide(@RequestBody Map<String, Object> request) {
        Long prescriptionId = request.get("prescriptionId") != null
                ? ((Number) request.get("prescriptionId")).longValue() : null;
        return Result.success(medicationService.generateGuide(prescriptionId));
    }

    /**
     * 查看用药指导
     */
    @GetMapping("/{prescriptionId}")
    @Operation(summary = "查看用药指导", description = "患者/药房/医生查看用药指导")
    public Result<Map<String, Object>> getGuide(@PathVariable Long prescriptionId) {
        return Result.success(medicationService.getGuide(prescriptionId));
    }

    /**
     * 标记已打印
     */
    @PostMapping("/print/{id}")
    @RequireLogin({RoleEnum.PHARMACY, RoleEnum.PATIENT})
    @Operation(summary = "标记已打印", description = "药房标记用药指导已打印")
    public Result<String> markPrinted(@PathVariable Long id) {
        return Result.success(medicationService.markPrinted(id));
    }
}
