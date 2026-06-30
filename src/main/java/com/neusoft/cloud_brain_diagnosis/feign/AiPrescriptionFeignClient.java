package com.neusoft.cloud_brain_diagnosis.feign;

import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Feign 客户端 — 调用 AI 微服务的处方审核接口
 */
@FeignClient(name = "brain-ai-svc", contextId = "AiPrescriptionFeignClient", url = "${ai.service.url:http://localhost:8081}")
public interface AiPrescriptionFeignClient {

    @PostMapping("/api/prescription/ai/check")
    Result<Map<String, Object>> checkPrescription(@RequestBody Map<String, Object> request);

    @GetMapping("/api/prescription/ai/review-list")
    Result<Map<String, Object>> getReviewList(@RequestParam(defaultValue = "1") Integer page,
                                              @RequestParam(defaultValue = "10") Integer size);

    @GetMapping("/api/prescription/ai/review/{id}")
    Result<Map<String, Object>> getReviewDetail(@PathVariable("id") Long id);
}
