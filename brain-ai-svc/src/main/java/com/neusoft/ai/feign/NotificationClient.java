package com.neusoft.ai.feign;

import com.neusoft.ai.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * Feign 客户端 — 调用主服务发送通知
 */
@FeignClient(name = "brain-main-svc", url = "${main.service.url:http://localhost:8080}")
public interface NotificationClient {

    @PostMapping("/internal/notification/high-risk")
    Result<Void> notifyHighRisk(@RequestParam("doctorId") Long doctorId,
                                @RequestParam("reviewId") Long reviewId,
                                @RequestBody List<Map<String, Object>> warnings,
                                @RequestParam("suggestions") String suggestions);

    @PostMapping("/internal/notification/medium-risk")
    Result<Void> notifyMediumRisk(@RequestParam("doctorId") Long doctorId,
                                  @RequestParam("reviewId") Long reviewId,
                                  @RequestBody List<Map<String, Object>> warnings,
                                  @RequestParam("suggestions") String suggestions);
}
