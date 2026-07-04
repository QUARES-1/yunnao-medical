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

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/triage")
@RequiredArgsConstructor
@Tag(name = "AI智能分诊", description = "诊前智能分诊模块：输入症状智能推荐科室和医生")
public class TriageController {

    private final AiTriageService triageService;

    /**
     * 智能分诊：输入症状，调用当前后端内置 AI 服务生成推荐结果。
     * 不再转发到 8081，避免 AI 微服务未启动时患者端直接不可用。
     */
    @PostMapping("/consult")
    @Operation(summary = "智能分诊", description = "公开接口，输入症状描述，调用真实 AI 返回推荐科室和医生列表")
    public Result<Map<String, Object>> consult(@RequestBody Map<String, Object> request) {
        String chiefComplaint = firstText(request, "chiefComplaint", "symptoms", "content", "question");
        Long patientId = toLong(request.get("patientId"));
        if (patientId == null) {
            patientId = UserContext.getUserId();
        }
        if (chiefComplaint == null || chiefComplaint.trim().isEmpty()) {
            return Result.error("请先输入症状描述");
        }
        return Result.success(triageService.consult(chiefComplaint.trim(), patientId));
    }

    /**
     * 分诊历史
     */
    @GetMapping("/history")
    @RequireLogin(RoleEnum.PATIENT)
    @Operation(summary = "分诊历史", description = "患者的分诊历史记录")
    public Result<Map<String, Object>> getHistory(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<TriageRecord> records = triageService.getPatientList(UserContext.getUserId(), page, size);
        return Result.success(pageToMap(records));
    }

    /**
     * 分诊详情
     */
    @GetMapping("/detail/{id}")
    @RequireLogin
    @Operation(summary = "分诊详情", description = "查看分诊详情")
    public Result<Map<String, Object>> getDetail(@PathVariable Long id) {
        TriageRecord record = triageService.getDetail(id);
        Map<String, Object> data = new HashMap<>();
        data.put("id", record.getId());
        data.put("patientId", record.getPatientId());
        data.put("chiefComplaint", record.getChiefComplaint());
        data.put("recommendDepartmentId", record.getRecommendDepartmentId());
        data.put("recommendDepartment", record.getRecommendDepartment());
        data.put("recommendDoctorIds", record.getRecommendDoctorIds());
        data.put("aiAnalysis", record.getAiAnalysis());
        data.put("confidence", record.getConfidence());
        data.put("status", record.getStatus());
        data.put("rawResponse", record.getRawResponse());
        data.put("createTime", record.getCreateTime());
        return Result.success(data);
    }

    private static String firstText(Map<String, Object> request, String... keys) {
        for (String key : keys) {
            Object value = request.get(key);
            if (value != null && !String.valueOf(value).trim().isEmpty()) {
                return String.valueOf(value);
            }
        }
        return null;
    }

    private static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Map<String, Object> pageToMap(Page<?> page) {
        Map<String, Object> data = new HashMap<>();
        data.put("records", page.getContent());
        data.put("list", page.getContent());
        data.put("content", page.getContent());
        data.put("total", page.getTotalElements());
        data.put("page", page.getNumber() + 1);
        data.put("size", page.getSize());
        data.put("pages", page.getTotalPages());
        return data;
    }
}
