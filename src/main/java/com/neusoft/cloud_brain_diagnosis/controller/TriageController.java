package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.annotation.RequireLogin;
import com.neusoft.cloud_brain_diagnosis.common.context.UserContext;
import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import com.neusoft.cloud_brain_diagnosis.entity.TriageRecord;
import com.neusoft.cloud_brain_diagnosis.service.ai.AiTriageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/triage")
@RequiredArgsConstructor
@Tag(name = "AI智能分诊", description = "诊前智能分诊模块：输入症状智能推荐科室和医生")
public class TriageController {

    private final AiTriageService triageService;

    /**
     * 智能分诊：输入症状，返回推荐科室和医生
     */
    @PostMapping("/consult")
    @Operation(summary = "智能分诊", description = "公开接口，输入症状描述，返回推荐科室和医生列表")
    public Result<Map<String, Object>> consult(@RequestBody Map<String, Object> request) {
        String chiefComplaint = (String) request.get("chiefComplaint");
        Long patientId = request.get("patientId") != null
                ? ((Number) request.get("patientId")).longValue() : null;
        return Result.success(triageService.consult(chiefComplaint, patientId));
    }

    /**
     * 分诊记录列表（患者）
     */
    @GetMapping("/patient/list")
    @RequireLogin(RoleEnum.PATIENT)
    @Operation(summary = "我的分诊历史", description = "患者查询自己的分诊历史记录")
    public Result<Page<TriageRecord>> getPatientList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long patientId = UserContext.getUserId();
        return Result.success(triageService.getPatientList(patientId, page, size));
    }

    /**
     * 分诊详情
     */
    @GetMapping("/detail/{id}")
    @Operation(summary = "分诊详情", description = "公开接口，查看分诊记录详情")
    public Result<TriageRecord> getDetail(@PathVariable Long id) {
        return Result.success(triageService.getDetail(id));
    }
}
