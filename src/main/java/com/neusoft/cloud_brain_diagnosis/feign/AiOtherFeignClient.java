package com.neusoft.cloud_brain_diagnosis.feign;

import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Feign 客户端 — 调用 AI 微服务的分诊/随访/质检/知识库等接口
 */
@FeignClient(name = "brain-ai-svc", contextId = "AiOtherFeignClient", url = "${ai.service.url:http://localhost:8081}")
public interface AiOtherFeignClient {

    // ========== 分诊 ==========
    @PostMapping("/api/triage/consult")
    Result<Map<String, Object>> triageConsult(@RequestBody Map<String, Object> request);

    @GetMapping("/api/triage/history")
    Result<Map<String, Object>> getTriageHistory(@RequestParam(defaultValue = "1") Integer page,
                                                  @RequestParam(defaultValue = "10") Integer size);

    // ========== 随访 ==========
    @PostMapping("/api/follow-up/plan/create")
    Result<Map<String, Object>> createFollowUpPlan(@RequestBody Map<String, Object> request);

    @GetMapping("/api/follow-up/patient/plans")
    Result<Map<String, Object>> getPatientPlans(@RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer size);

    @GetMapping("/api/follow-up/detail/{id}")
    Result<Map<String, Object>> getFollowUpDetail(@PathVariable("id") Long id);

    @GetMapping("/api/follow-up/doctor/list")
    Result<Map<String, Object>> getDoctorFollowUpList(@RequestParam(defaultValue = "1") Integer page,
                                                       @RequestParam(defaultValue = "10") Integer size);

    // ========== 质检 ==========
    @GetMapping("/api/doctor/quality-check/my-list")
    Result<Map<String, Object>> getMyQualityList(@RequestParam(defaultValue = "1") Integer page,
                                                  @RequestParam(defaultValue = "10") Integer size);

    @PostMapping("/api/doctor/quality-check/rectify/{id}")
    Result<String> rectify(@PathVariable("id") Long id, @RequestParam String remark);

    // ========== 知识库 ==========
    @GetMapping("/api/admin/ai/knowledge/list")
    Result<Map<String, Object>> getKnowledgeList(@RequestParam(required = false) String category,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(defaultValue = "1") Integer page,
                                                  @RequestParam(defaultValue = "10") Integer size);

    @PostMapping("/api/admin/ai/knowledge/add")
    Result<String> addKnowledge(@RequestBody Map<String, Object> knowledge);

    @PutMapping("/api/admin/ai/knowledge/update")
    Result<String> updateKnowledge(@RequestBody Map<String, Object> knowledge);

    @DeleteMapping("/api/admin/ai/knowledge/delete/{id}")
    Result<String> deleteKnowledge(@PathVariable("id") Long id);
}
