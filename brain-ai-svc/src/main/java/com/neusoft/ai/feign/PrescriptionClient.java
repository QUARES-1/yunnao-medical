package com.neusoft.ai.feign;

import com.neusoft.ai.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * Feign 客户端 — 调用主服务获取处方数据
 */
@FeignClient(name = "brain-main-svc", url = "${main.service.url:http://localhost:8080}")
public interface PrescriptionClient {

    @GetMapping("/api/prescription/detail/{id}")
    Result<Map<String, Object>> getDetail(@PathVariable("id") Long id);

    @GetMapping("/api/prescription/patient/list")
    Result<List<Map<String, Object>>> getPatientList(@RequestParam("patientId") Long patientId);

    @PostMapping("/api/prescription/list-all")
    Result<List<Map<String, Object>>> listAll();
}
