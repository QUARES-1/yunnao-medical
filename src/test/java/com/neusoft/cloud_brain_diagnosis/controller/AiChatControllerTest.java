package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.util.JwtUtil;
import com.neusoft.cloud_brain_diagnosis.entity.AiChatRecord;
import com.neusoft.cloud_brain_diagnosis.feign.AiChatFeignClient;
import com.neusoft.cloud_brain_diagnosis.service.ai.AiChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = AiChatController.class, properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration," +
                "org.springframework.boot.autoconfigure.websocket.reactive.WebSocketReactiveAutoConfiguration"
})
class AiChatControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private AiChatFeignClient chatFeignClient;
    @MockBean private AiChatService chatService;
    @MockBean private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        when(jwtUtil.validateToken(anyString())).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(1L);
    }

    // ========== chat() ==========

    @Test
    void chat_ShouldReturnAnswer() throws Exception {
        when(chatService.chat(eq("如何挂号"), any(), isNull(), isNull()))
                .thenReturn(Map.of("answer", "您可以通过公众号挂号", "source", "knowledge"));

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"如何挂号\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answer").value("您可以通过公众号挂号"))
                .andExpect(jsonPath("$.data.source").value("knowledge"));
    }

    @Test
    void chat_ShouldPassSessionId() throws Exception {
        when(chatService.chat(eq("挂号"), eq("session-123"), isNull(), isNull()))
                .thenReturn(Map.of("answer", "答案", "source", "ai"));

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"挂号\",\"sessionId\":\"session-123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answer").value("答案"));
    }

    // ========== getChatHistory() ==========

    @Test
    void getChatHistory_ShouldReturnPage() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PATIENT.getCode());

        AiChatRecord record = new AiChatRecord();
        record.setId(1L);
        record.setQuestion("测试问题");
        record.setAnswer("测试答案");

        when(chatService.getChatHistory(eq(1L), eq("patient"), eq(1), eq(10)))
                .thenReturn(new PageImpl<>(List.of(record)));

        mockMvc.perform(get("/api/ai/chat/history")
                        .header("Authorization", "Bearer patient-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].question").value("测试问题"));
    }

    // ========== feedback() ==========

    @Test
    void feedback_ShouldUpdateRecord() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PATIENT.getCode());
        when(chatService.feedback(eq(1L), eq("helpful"))).thenReturn("反馈成功");

        mockMvc.perform(post("/api/ai/chat/feedback/1")
                        .param("feedback", "helpful")
                        .header("Authorization", "Bearer patient-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("反馈成功"));
    }

    // ========== healthConsult() ==========

    @Test
    void healthConsult_ShouldReturnConsultResult() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PATIENT.getCode());

        when(chatService.healthConsult(eq("头疼"), eq(1L), eq(false)))
                .thenReturn(Map.of(
                        "answer", "建议多休息",
                        "recommendDepartment", "内科",
                        "recommendDepartmentId", 1,
                        "id", 100L
                ));

        mockMvc.perform(post("/api/ai/health-consult")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"头疼\",\"includeHistory\":false}")
                        .header("Authorization", "Bearer patient-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answer").value("建议多休息"))
                .andExpect(jsonPath("$.data.recommendDepartment").value("内科"));
    }

    @Test
    void healthConsult_ShouldIncludeHistory_WhenRequested() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PATIENT.getCode());

        when(chatService.healthConsult(eq("头晕"), eq(1L), eq(true)))
                .thenReturn(Map.of("answer", "建议检查血压"));

        mockMvc.perform(post("/api/ai/health-consult")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"头晕\",\"includeHistory\":true}")
                        .header("Authorization", "Bearer patient-token"))
                .andExpect(status().isOk());
    }

    // ========== getConsultHistory() ==========

    @Test
    void getConsultHistory_ShouldReturnPage() throws Exception {
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.PATIENT.getCode());

        AiChatRecord record = new AiChatRecord();
        record.setId(2L);
        record.setCategory("health");

        when(chatService.getChatHistory(eq(1L), eq("patient"), eq(1), eq(10)))
                .thenReturn(new PageImpl<>(List.of(record)));

        mockMvc.perform(get("/api/ai/consult/history")
                        .header("Authorization", "Bearer patient-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(2));
    }
}
