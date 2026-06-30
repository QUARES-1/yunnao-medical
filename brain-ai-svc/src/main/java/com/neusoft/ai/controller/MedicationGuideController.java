package com.neusoft.ai.controller;

import com.neusoft.ai.common.context.UserContext;
import com.neusoft.ai.common.result.Result;
import com.neusoft.ai.service.ai.AiMedicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/medication/ai")
@RequiredArgsConstructor
@Tag(name = "AI用药指导", description = "AI智能用药指导")
public class MedicationGuideController {

    private final AiMedicationService medicationService;

    @GetMapping("/guide/{prescriptionId}")
    @Operation(summary = "获取用药指导", description = "根据处方获取AI生成的用药指导")
    public Result<Map<String, Object>> getGuide(@PathVariable Long prescriptionId) {
        Long patientId = UserContext.getUserId();
        return Result.success(medicationService.getGuide(prescriptionId, patientId));
    }
}
