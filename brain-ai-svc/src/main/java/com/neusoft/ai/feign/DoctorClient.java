package com.neusoft.ai.feign;

import com.neusoft.ai.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * Feign 客户端 — 调用主服务获取医生信息
 */
@FeignClient(name = "brain-main-svc", url = "${main.service.url:http://localhost:8080}")
public interface DoctorClient {

    @GetMapping("/api/doctor/detail/{id}")
    Result<Map<String, Object>> getDoctor(@PathVariable("id") Long id);

    @GetMapping("/api/doctor/list")
    Result<List<Map<String, Object>>> getDoctorsByDepartment(@RequestParam("departmentId") Long departmentId);
}
