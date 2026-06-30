package com.neusoft.ai.feign;

import com.neusoft.ai.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * Feign 客户端 — 调用主服务获取患者信息
 */
@FeignClient(name = "brain-main-svc", url = "${main.service.url:http://localhost:8080}")
public interface PatientClient {

    @GetMapping("/api/patient/detail/{id}")
    Result<Map<String, Object>> getPatient(@PathVariable("id") Long id);
}
