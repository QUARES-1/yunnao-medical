package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.annotation.RequireLogin;
import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import com.neusoft.cloud_brain_diagnosis.feign.AiOtherFeignClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/triage")
@RequiredArgsConstructor
@Tag(name = "AI智能分诊", description = "诊前智能分诊模块：输入症状智能推荐科室和医生")
public class TriageController {

    private final AiOtherFeignClient otherFeignClient;

    /**
     * 智能分诊：输入症状，返回推荐科室和医生
     */
    @PostMapping("/consult")
    @Operation(summary = "智能分诊", description = "公开接口，输入症状描述，返回推荐科室和医生列表")
    public Result<Map<String, Object>> consult(@RequestBody Map<String, Object> request) {
        return otherFeignClient.triageConsult(request);
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
        return otherFeignClient.getTriageHistory(page, size);
    }

    /**
     * 分诊详情
     */
    @GetMapping("/detail/{id}")
    @RequireLogin
    @Operation(summary = "分诊详情", description = "查看分诊详情")
    public Result<Map<String, Object>> getDetail(@PathVariable Long id) {
        return otherFeignClient.getTriageDetail(id);
    }
}
