package com.neusoft.ai.feign;

import com.neusoft.ai.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * Feign 客户端 — 调用主服务获取药品信息
 */
@FeignClient(name = "brain-main-svc", url = "${main.service.url:http://localhost:8080}")
public interface MedicineClient {

    @GetMapping("/api/medicine/list")
    Result<Map<String, Object>> getMedicineList(@RequestParam("page") Integer page,
                                                 @RequestParam("size") Integer size);
}
