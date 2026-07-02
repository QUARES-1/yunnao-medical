package com.neusoft.cloud_brain_diagnosis.websocket;

import com.neusoft.cloud_brain_diagnosis.common.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DoctorNotificationWebSocketHandlerTest {

    @Mock
    private JwtUtil jwtUtil;

    private DoctorNotificationWebSocketHandler handler;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        Map<Long, WebSocketSession> sessions =
                (Map<Long, WebSocketSession>) ReflectionTestUtils.getField(DoctorNotificationWebSocketHandler.class, "SESSION_MAP");
        sessions.clear();

        handler = new DoctorNotificationWebSocketHandler();
        ReflectionTestUtils.setField(handler, "jwtUtil", jwtUtil);
    }

    @Test
    void afterConnectionEstablished_ShouldStoreValidSession() throws Exception {
        WebSocketSession session = validSession(7L, "token");
        when(jwtUtil.validateToken("token", 7L)).thenReturn(true);

        handler.afterConnectionEstablished(session);

        assertEquals(1, handler.getOnlineCount());
    }

    @Test
    void afterConnectionEstablished_ShouldCloseInvalidSession() throws Exception {
        WebSocketSession session = validSession(8L, "bad");
        when(jwtUtil.validateToken("bad", 8L)).thenReturn(false);

        handler.afterConnectionEstablished(session);

        verify(session).close(CloseStatus.POLICY_VIOLATION);
        assertEquals(0, handler.getOnlineCount());
    }

    @Test
    void afterConnectionEstablished_ShouldCloseOldSessionForSameDoctor() throws Exception {
        WebSocketSession oldSession = validSession(9L, "token");
        WebSocketSession newSession = validSession(9L, "token");
        when(jwtUtil.validateToken("token", 9L)).thenReturn(true);

        handler.afterConnectionEstablished(oldSession);
        handler.afterConnectionEstablished(newSession);

        verify(oldSession).close(CloseStatus.NORMAL);
        assertEquals(1, handler.getOnlineCount());
    }

    @Test
    void afterConnectionEstablished_ShouldCloseWhenDoctorIdIsMissing() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getUri()).thenReturn(URI.create("ws://localhost/ws/doctor?token=token"));

        handler.afterConnectionEstablished(session);

        verify(session).close(CloseStatus.POLICY_VIOLATION);
    }

    @Test
    void handleTextMessage_ShouldReplyPongForPing() throws Exception {
        WebSocketSession session = validSession(10L, "token");

        handler.handleTextMessage(session, new TextMessage("ping"));

        verify(session).sendMessage(argThat(message -> "pong".equals(message.getPayload())));
    }

    @Test
    void handleTextMessage_ShouldIgnoreNonPingPayload() throws Exception {
        WebSocketSession session = validSession(10L, "token");

        handler.handleTextMessage(session, new TextMessage("hello"));

        verify(session, never()).sendMessage(any());
    }

    @Test
    void sendToDoctor_ShouldSendMessageWhenSessionIsOpen() throws Exception {
        WebSocketSession session = validSession(11L, "token");
        when(jwtUtil.validateToken("token", 11L)).thenReturn(true);
        handler.afterConnectionEstablished(session);

        boolean sent = handler.sendToDoctor(11L, "{\"type\":\"TEST\"}");

        assertTrue(sent);
        verify(session).sendMessage(argThat((TextMessage message) -> message.getPayload().contains("TEST")));
    }

    @Test
    void sendToDoctor_ShouldReturnFalseWhenDoctorOffline() {
        boolean sent = handler.sendToDoctor(404L, "message");

        assertFalse(sent);
    }

    @Test
    void sendToDoctor_ShouldRemoveSessionWhenSendFails() throws Exception {
        WebSocketSession session = validSession(12L, "token");
        when(jwtUtil.validateToken("token", 12L)).thenReturn(true);
        doThrow(new IOException("broken")).when(session).sendMessage(any(TextMessage.class));
        handler.afterConnectionEstablished(session);

        boolean sent = handler.sendToDoctor(12L, "message");

        assertFalse(sent);
        assertEquals(0, handler.getOnlineCount());
    }

    @Test
    void afterConnectionClosed_ShouldOnlyRemoveCurrentSession() throws Exception {
        WebSocketSession session = validSession(13L, "token");
        when(jwtUtil.validateToken("token", 13L)).thenReturn(true);
        handler.afterConnectionEstablished(session);

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        assertEquals(0, handler.getOnlineCount());
    }

    @Test
    void handleTransportError_ShouldNotThrowWhenUriIsInvalid() {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getUri()).thenReturn(URI.create("ws://localhost/ws/doctor/not-a-number?token=token"));

        assertDoesNotThrow(() -> handler.handleTransportError(session, new RuntimeException("boom")));
    }

    private WebSocketSession validSession(Long doctorId, String token) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getUri()).thenReturn(URI.create("ws://localhost/ws/doctor/" + doctorId + "?token=" + token));
        lenient().when(session.isOpen()).thenReturn(true);
        return session;
    }
}
