package com.neusoft.cloud_brain_diagnosis.feign;

import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Feign 客户端 — 调用 AI 微服务的检验/检查 AI 接口
 */
@FeignClient(name = "brain-ai-svc", contextId = "AiExaminationFeignClient", url = "${ai.service.url:http://localhost:8081}")
public interface AiExaminationFeignClient {

    @PostMapping("/api/examination/ai/interpret")
    Result<Map<String, Object>> interpret(@RequestBody Map<String, Object> request);

    @GetMapping("/api/examination/ai/interpret-patient/{patientId}")
    Result<Map<String, Object>> getPatientInterpretation(@PathVariable("patientId") Long patientId,
                                                          @RequestParam(defaultValue = "1") Integer page,
                                                          @RequestParam(defaultValue = "10") Integer size);

    @GetMapping("/api/examination/ai/interpret-pro/{id}")
    Result<Map<String, Object>> getProInterpretation(@PathVariable("id") Long id);

    @GetMapping("/api/examination/ai/critical/list")
    Result<Map<String, Object>> getCriticalList(@RequestParam(required = false) String status,
                                                 @RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer size);

    @GetMapping("/api/examination/ai/critical/detect/{examinationId}")
    Result<Map<String, Object>> detectCriticalValue(@PathVariable("examinationId") Long examinationId);

    @PostMapping("/api/examination/ai/critical/confirm/{id}")
    Result<Map<String, Object>> confirmWarning(@PathVariable("id") Long id);

    @PostMapping("/api/examination/ai/critical/process/{id}")
    Result<Map<String, Object>> processWarning(@PathVariable("id") Long id, @RequestParam String note);

    @GetMapping("/api/examination/ai/critical/history/{patientId}")
    Result<Map<String, Object>> getCriticalHistory(@PathVariable("patientId") Long patientId,
                                                    @RequestParam(defaultValue = "1") Integer page,
                                                    @RequestParam(defaultValue = "10") Integer size);

    @PostMapping("/api/examination/ai/review")
    Result<Map<String, Object>> reviewExamination(@RequestBody Map<String, Object> request);

    @GetMapping("/api/examination/ai/review/{id}")
    Result<Map<String, Object>> getReviewDetail(@PathVariable("id") Long id);

    @GetMapping("/api/examination/ai/review-stats")
    Result<Map<String, Object>> getReviewStats();

    @GetMapping("/api/examination/ai/manual-list")
    Result<Map<String, Object>> getManualList(@RequestParam(defaultValue = "1") Integer page,
                                              @RequestParam(defaultValue = "10") Integer size);

    @GetMapping("/api/examination/ai/review-list")
    Result<Map<String, Object>> getReviewList(@RequestParam(required = false) String reviewResult,
                                              @RequestParam(defaultValue = "1") Integer page,
                                              @RequestParam(defaultValue = "10") Integer size);

    @PostMapping("/api/examination/ai/manual-confirm/{id}")
    Result<Map<String, Object>> manualConfirm(@PathVariable("id") Long id);

    @PostMapping("/api/examination/ai/reject/{id}")
    Result<Map<String, Object>> reject(@PathVariable("id") Long id, @RequestParam String reason);
}
