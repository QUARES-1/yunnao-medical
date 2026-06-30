package com.neusoft.cloud_brain_diagnosis.service;

import cn.hutool.json.JSONObject;
import com.neusoft.cloud_brain_diagnosis.websocket.DoctorNotificationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 通知服务 — 通过 WebSocket 向医生端推送实时消息
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final DoctorNotificationWebSocketHandler webSocketHandler;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 推送高风险用药告警
     *
     * @param doctorId    目标医生ID
     * @param reviewId    审核记录ID
     * @param warnings    告警列表
     * @param suggestions 修改建议
     */
    public void notifyHighRiskMedication(Long doctorId, Long reviewId,
                                         List<Map<String, Object>> warnings,
                                         String suggestions) {
        JSONObject payload = new JSONObject();
        payload.set("type", "HIGH_RISK_MEDICATION");
        payload.set("title", "⚠ 高风险用药告警");
        payload.set("content", "AI处方审核发现高风险用药，请及时处理。");

        JSONObject data = new JSONObject();
        data.set("reviewId", reviewId);
        data.set("reviewResult", "reject");
        data.set("warnings", warnings);
        data.set("suggestions", suggestions);
        data.set("timestamp", LocalDateTime.now().format(DTF));
        payload.set("data", data);

        boolean sent = webSocketHandler.sendToDoctor(doctorId, payload.toString());
        if (sent) {
            log.info("[通知] 高风险用药告警已推送给医生 {}，审核记录ID: {}", doctorId, reviewId);
        }
    }

    /**
     * 推送中等风险用药提醒
     *
     * @param doctorId    目标医生ID
     * @param reviewId    审核记录ID
     * @param warnings    告警列表
     * @param suggestions 修改建议
     */
    public void notifyMediumRiskMedication(Long doctorId, Long reviewId,
                                           List<Map<String, Object>> warnings,
                                           String suggestions) {
        JSONObject payload = new JSONObject();
        payload.set("type", "MEDIUM_RISK_MEDICATION");
        payload.set("title", "⚡ 用药提醒");
        payload.set("content", "AI处方审核发现中等风险问题，建议关注。");

        JSONObject data = new JSONObject();
        data.set("reviewId", reviewId);
        data.set("reviewResult", "warning");
        data.set("warnings", warnings);
        data.set("suggestions", suggestions);
        data.set("timestamp", LocalDateTime.now().format(DTF));
        payload.set("data", data);

        boolean sent = webSocketHandler.sendToDoctor(doctorId, payload.toString());
        if (sent) {
            log.info("[通知] 中风险用药提醒已推送给医生 {}，审核记录ID: {}", doctorId, reviewId);
        }
    }

    /**
     * 推送常规通知（预留扩展）
     */
    public void notify(Long doctorId, String type, String title, String content, Map<String, Object> extraData) {
        JSONObject payload = new JSONObject();
        payload.set("type", type);
        payload.set("title", title);
        payload.set("content", content);
        if (extraData != null) {
            payload.set("data", extraData);
        }
        payload.set("timestamp", LocalDateTime.now().format(DTF));

        webSocketHandler.sendToDoctor(doctorId, payload.toString());
    }
}
