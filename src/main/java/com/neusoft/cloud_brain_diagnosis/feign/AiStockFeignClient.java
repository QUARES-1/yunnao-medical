package com.neusoft.cloud_brain_diagnosis.feign;

import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Feign 客户端 — 调用 AI 微服务的库存预测接口
 */
@FeignClient(name = "brain-ai-svc", contextId = "AiStockFeignClient", url = "${ai.service.url:http://localhost:8081}")
public interface AiStockFeignClient {

    @PostMapping("/api/medicine/ai/stock-forecast/generate")
    Result<Map<String, Object>> generateForecast(@RequestBody Map<String, Object> request);

    @GetMapping("/api/medicine/ai/stock-forecast/{id}")
    Result<Map<String, Object>> getForecastDetail(@PathVariable("id") Long id);

    @GetMapping("/api/medicine/ai/stock-forecast/list")
    Result<Map<String, Object>> getForecastList(@RequestParam(required = false) String forecastType,
                                                 @RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer size);
}
