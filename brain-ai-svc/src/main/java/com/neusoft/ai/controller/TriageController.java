package com.neusoft.ai.controller;

import com.neusoft.ai.common.context.UserContext;
import com.neusoft.ai.common.result.Result;
import com.neusoft.ai.entity.TriageRecord;
import com.neusoft.ai.service.ai.AiTriageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/triage")
@RequiredArgsConstructor
@Tag(name = "AI智能分诊", description = "AI分诊导诊服务")
public class TriageController {

    private final AiTriageService triageService;

    @PostMapping("/consult")
    @Operation(summary = "AI分诊", description = "根据症状推荐科室和医生")
    public Result<Map<String, Object>> consult(@RequestBody Map<String, Object> request) {
        String chiefComplaint = (String) request.get("chiefComplaint");
        Long patientId = UserContext.getUserId();
        return Result.success(triageService.consult(chiefComplaint, patientId));
    }

    @GetMapping("/history")
    @Operation(summary = "分诊历史", description = "患者的分诊记录")
    public Result<Page<TriageRecord>> getHistory(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long patientId = UserContext.getUserId();
        return Result.success(triageService.getPatientList(patientId, page, size));
    }

    @GetMapping("/detail/{id}")
    @Operation(summary = "分诊详情")
    public Result<TriageRecord> getDetail(@PathVariable Long id) {
        return Result.success(triageService.getDetail(id));
    }
}
