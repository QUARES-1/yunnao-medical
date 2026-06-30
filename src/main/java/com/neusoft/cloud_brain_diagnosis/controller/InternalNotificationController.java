package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import com.neusoft.cloud_brain_diagnosis.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 内部接口 — 供 AI 微服务回调，不对外开放
 * 用于 AI 处方审核后的 WebSocket 通知推送
 */
@RestController
@RequestMapping("/internal/notification")
@RequiredArgsConstructor
public class InternalNotificationController {

    private final NotificationService notificationService;

    @PostMapping("/high-risk")
    public Result<Void> notifyHighRisk(@RequestParam Long doctorId,
                                       @RequestParam Long reviewId,
                                       @RequestBody List<Map<String, Object>> warnings,
                                       @RequestParam String suggestions) {
        notificationService.notifyHighRiskMedication(doctorId, reviewId, warnings, suggestions);
        return Result.success(null);
    }

    @PostMapping("/medium-risk")
    public Result<Void> notifyMediumRisk(@RequestParam Long doctorId,
                                         @RequestParam Long reviewId,
                                         @RequestBody List<Map<String, Object>> warnings,
                                         @RequestParam String suggestions) {
        notificationService.notifyMediumRiskMedication(doctorId, reviewId, warnings, suggestions);
        return Result.success(null);
    }
}
