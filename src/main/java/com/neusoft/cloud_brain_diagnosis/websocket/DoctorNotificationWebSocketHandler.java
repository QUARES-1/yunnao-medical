package com.neusoft.cloud_brain_diagnosis.websocket;

import com.neusoft.cloud_brain_diagnosis.common.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 处理器 — 医生端实时通知
 * <p>
 * 医生端连接地址: ws://host:port/ws/doctor/{doctorId}?token=xxx
 * 连接时通过 PathVariable 获取 doctorId，通过 QueryParam token 鉴权。
 */
@Slf4j
public class DoctorNotificationWebSocketHandler extends TextWebSocketHandler {

    /**
     * 在线医生连接池: doctorId -> WebSocketSession
     */
    private static final Map<Long, WebSocketSession> SESSION_MAP = new ConcurrentHashMap<>();

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long doctorId = extractDoctorId(session);
        String token = extractToken(session);

        if (doctorId == null || token == null || !jwtUtil.validateToken(token, doctorId)) {
            log.warn("[WebSocket] 连接鉴权失败，doctorId={}, token={}", doctorId, token);
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        // 关闭旧连接（同一医生多端登录时只保留最新连接）
        WebSocketSession oldSession = SESSION_MAP.put(doctorId, session);
        if (oldSession != null && oldSession.isOpen()) {
            try {
                oldSession.close(CloseStatus.NORMAL);
            } catch (IOException e) {
                // ignore
            }
        }
        log.info("[WebSocket] 医生 {} 已连接，当前在线: {}", doctorId, SESSION_MAP.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 医生端目前只需要接收推送，不需要发送消息。收到消息仅做心跳响应
        Long doctorId = extractDoctorId(session);
        String payload = message.getPayload();
        if ("ping".equals(payload)) {
            session.sendMessage(new TextMessage("pong"));
        } else {
            log.debug("[WebSocket] 收到医生 {} 的消息: {}", doctorId, payload);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long doctorId = extractDoctorId(session);
        if (doctorId != null) {
            // 只清理当前这条连接，避免多端登录时误删其它连接
            SESSION_MAP.remove(doctorId, session);
            log.info("[WebSocket] 医生 {} 已断开 ({}), 当前在线: {}", doctorId, status, SESSION_MAP.size());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        Long doctorId = extractDoctorId(session);
        log.error("[WebSocket] 医生 {} 传输错误: {}", doctorId, exception.getMessage());
    }

    /**
     * 向指定医生发送通知
     *
     * @param doctorId 医生ID
     * @param message  JSON格式的消息内容
     * @return true 发送成功，false 医生不在线
     */
    public boolean sendToDoctor(Long doctorId, String message) {
        WebSocketSession session = SESSION_MAP.get(doctorId);
        if (session == null || !session.isOpen()) {
            log.debug("[WebSocket] 医生 {} 不在线，跳过推送", doctorId);
            return false;
        }
        try {
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                    log.info("[WebSocket] 已向医生 {} 推送通知", doctorId);
                    return true;
                }
            }
        } catch (IOException e) {
            log.error("[WebSocket] 向医生 {} 推送失败: {}", doctorId, e.getMessage());
            SESSION_MAP.remove(doctorId, session);
        }
        return false;
    }

    /**
     * 获取当前在线医生数
     */
    public int getOnlineCount() {
        return SESSION_MAP.size();
    }

    // ==================== 辅助方法 ====================

    private Long extractDoctorId(WebSocketSession session) {
        try {
            String path = session.getUri() != null ? session.getUri().getPath() : "";
            // path: /ws/doctor/{doctorId}
            String[] segments = path.split("/");
            if (segments.length >= 4) {
                return Long.parseLong(segments[3]);
            }
        } catch (Exception e) {
            log.warn("[WebSocket] 解析 doctorId 失败: {}", e.getMessage());
        }
        return null;
    }

    private String extractToken(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) return null;
        String query = uri.getQuery();
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            if (pair.length == 2 && "token".equals(pair[0])) {
                return pair[1];
            }
        }
        return null;
    }
}
