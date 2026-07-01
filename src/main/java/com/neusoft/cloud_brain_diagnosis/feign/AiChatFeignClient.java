package com.neusoft.cloud_brain_diagnosis.feign;

import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Feign 客户端 — 调用 AI 微服务的就医辅助接口
 */
@FeignClient(name = "brain-ai-svc", contextId = "AiChatFeignClient", url = "${ai.service.url:http://localhost:8081}")
public interface AiChatFeignClient {

    @PostMapping("/api/ai/chat")
    Result<Map<String, Object>> chat(@RequestBody Map<String, Object> request);

    @GetMapping("/api/ai/chat/history")
    Result<Map<String, Object>> getChatHistory(@RequestParam(defaultValue = "1") Integer page,
                                               @RequestParam(defaultValue = "10") Integer size);

    @PostMapping("/api/ai/chat/feedback/{id}")
    Result<String> feedback(@PathVariable("id") Long id, @RequestParam String feedback);

    @PostMapping("/api/ai/health-consult")
    Result<Map<String, Object>> healthConsult(@RequestBody Map<String, Object> request);

    @GetMapping("/api/ai/consult/history")
    Result<Map<String, Object>> getConsultHistory(@RequestParam(defaultValue = "1") Integer page,
                                                  @RequestParam(defaultValue = "10") Integer size);
}
