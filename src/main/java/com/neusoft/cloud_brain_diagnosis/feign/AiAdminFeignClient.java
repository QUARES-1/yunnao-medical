package com.neusoft.cloud_brain_diagnosis.feign;

import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Feign 客户端 — 调用 AI 微服务的运营/质检/管理端接口
 */
@FeignClient(name = "brain-ai-svc", contextId = "AiAdminFeignClient", url = "${ai.service.url:http://localhost:8081}")
public interface AiAdminFeignClient {

    @PostMapping("/api/admin/ai/operation-report/generate")
    Result<Map<String, Object>> generateReport(@RequestBody Map<String, Object> request);

    @GetMapping("/api/admin/ai/operation-report/{id}")
    Result<Map<String, Object>> getReportDetail(@PathVariable("id") Long id);

    @GetMapping("/api/admin/ai/operation-report/list")
    Result<Map<String, Object>> getReportList(@RequestParam(required = false) String reportType,
                                               @RequestParam(defaultValue = "1") Integer page,
                                               @RequestParam(defaultValue = "10") Integer size);

    @GetMapping("/api/admin/ai/operation-overview")
    Result<Map<String, Object>> getOperationOverview();

    @PostMapping("/api/admin/ai/quality-check/start")
    Result<Map<String, Object>> startQualityCheck(@RequestBody Map<String, Object> request);

    @GetMapping("/api/admin/ai/quality-check/list")
    Result<Map<String, Object>> getCheckList(@RequestParam(defaultValue = "1") Integer page,
                                              @RequestParam(defaultValue = "10") Integer size);

    @GetMapping("/api/admin/ai/quality-check/{id}")
    Result<Map<String, Object>> getCheckDetail(@PathVariable("id") Long id);

    @GetMapping("/api/admin/ai/quality-check/doctor-stats")
    Result<Map<String, Object>> getDoctorStats();

    @GetMapping("/api/admin/ai/chat-log")
    Result<Map<String, Object>> getChatLogs(@RequestParam(defaultValue = "1") Integer page,
                                             @RequestParam(defaultValue = "10") Integer size);
}
