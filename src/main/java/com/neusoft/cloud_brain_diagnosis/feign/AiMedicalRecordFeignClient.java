package com.neusoft.cloud_brain_diagnosis.feign;

import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Feign 客户端 — 调用 AI 微服务的病历生成接口
 */
@FeignClient(name = "brain-ai-svc", contextId = "AiMedicalRecordFeignClient", url = "${ai.service.url:http://localhost:8081}")
public interface AiMedicalRecordFeignClient {

    @PostMapping("/api/medical-record/ai/generate")
    Result<Map<String, Object>> generateRecord(@RequestBody Map<String, Object> request);

    @GetMapping("/api/medical-record/ai/generate-list")
    Result<Map<String, Object>> getGenerateList(@RequestParam(defaultValue = "1") Integer page,
                                                @RequestParam(defaultValue = "10") Integer size);

    @GetMapping("/api/medical-record/ai/generate/{id}")
    Result<Map<String, Object>> getGenerateDetail(@PathVariable("id") Long id);
}
