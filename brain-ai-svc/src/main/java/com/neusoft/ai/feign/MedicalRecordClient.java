package com.neusoft.ai.feign;

import com.neusoft.ai.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

/**
 * Feign 客户端 — 调用主服务获取病历数据
 */
@FeignClient(name = "brain-main-svc", url = "${main.service.url:http://localhost:8080}")
public interface MedicalRecordClient {

    @GetMapping("/api/medical-record/registration/{regId}")
    Result<Map<String, Object>> getByRegistration(@PathVariable("regId") Long regId);

    @GetMapping("/api/medical-record/doctor/list")
    Result<Map<String, Object>> getDoctorList(@org.springframework.web.bind.annotation.RequestParam(defaultValue = "1") Integer page,
                                              @org.springframework.web.bind.annotation.RequestParam(defaultValue = "100") Integer size);

    @PostMapping("/api/medical-record/list-all")
    Result<List<Map<String, Object>>> listAll();
}
