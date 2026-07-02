package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.common.util.JwtUtil;
import com.neusoft.cloud_brain_diagnosis.entity.AiKnowledgeBase;
import com.neusoft.cloud_brain_diagnosis.feign.AiOtherFeignClient;
import com.neusoft.cloud_brain_diagnosis.service.ai.AiKnowledgeBaseService;
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

@WebMvcTest(value = AiKnowledgeBaseController.class, properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration," +
                "org.springframework.boot.autoconfigure.websocket.reactive.WebSocketReactiveAutoConfiguration"
})
class AiKnowledgeBaseControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private AiOtherFeignClient otherFeignClient;
    @MockBean private AiKnowledgeBaseService knowledgeBaseService;
    @MockBean private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        when(jwtUtil.validateToken(anyString())).thenReturn(true);
        when(jwtUtil.getRoleFromToken(anyString())).thenReturn(RoleEnum.ADMIN.getCode());
    }

    // ========== getList() ==========

    @Test
    void getList_ShouldReturnPage() throws Exception {
        AiKnowledgeBase kb = new AiKnowledgeBase();
        kb.setId(1L);
        kb.setQuestion("如何挂号");
        kb.setCategory("挂号");

        when(knowledgeBaseService.getKnowledgeList(isNull(), isNull(), eq(1), eq(10)))
                .thenReturn(new PageImpl<>(List.of(kb)));

        mockMvc.perform(get("/api/admin/ai/knowledge/list")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].question").value("如何挂号"));
    }

    @Test
    void getList_ShouldFilterByCategory() throws Exception {
        when(knowledgeBaseService.getKnowledgeList(eq("挂号"), isNull(), eq(1), eq(10)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/admin/ai/knowledge/list")
                        .param("category", "挂号")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
    }

    @Test
    void getList_ShouldFilterByKeyword() throws Exception {
        when(knowledgeBaseService.getKnowledgeList(isNull(), eq("挂号"), eq(1), eq(10)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/admin/ai/knowledge/list")
                        .param("keyword", "挂号")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
    }

    // ========== add() ==========

    @Test
    void add_ShouldCreateKnowledge() throws Exception {
        when(knowledgeBaseService.addKnowledge(any(AiKnowledgeBase.class)))
                .thenReturn("添加成功");

        mockMvc.perform(post("/api/admin/ai/knowledge/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"测试问题\",\"answer\":\"测试答案\",\"category\":\"测试\"}")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("添加成功"));
    }

    // ========== update() ==========

    @Test
    void update_ShouldUpdateKnowledge() throws Exception {
        when(knowledgeBaseService.updateKnowledge(any(AiKnowledgeBase.class)))
                .thenReturn("修改成功");

        mockMvc.perform(put("/api/admin/ai/knowledge/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"question\":\"更新后问题\"}")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("修改成功"));
    }

    @Test
    void update_ShouldThrow_WhenNotFound() throws Exception {
        when(knowledgeBaseService.updateKnowledge(any(AiKnowledgeBase.class)))
                .thenThrow(new BusinessException("知识条目不存在"));

        mockMvc.perform(put("/api/admin/ai/knowledge/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":99}")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ========== delete() ==========

    @Test
    void delete_ShouldDeleteKnowledge() throws Exception {
        when(knowledgeBaseService.deleteKnowledge(1L)).thenReturn("删除成功");

        mockMvc.perform(delete("/api/admin/ai/knowledge/delete/1")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("删除成功"));
    }

    @Test
    void delete_ShouldThrow_WhenNotFound() throws Exception {
        when(knowledgeBaseService.deleteKnowledge(99L))
                .thenThrow(new BusinessException("知识条目不存在"));

        mockMvc.perform(delete("/api/admin/ai/knowledge/delete/99")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }
}
