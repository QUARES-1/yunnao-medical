package com.neusoft.cloud_brain_diagnosis.service.ai;

import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.common.util.AiApiUtil;
import com.neusoft.cloud_brain_diagnosis.entity.AiChatRecord;
import com.neusoft.cloud_brain_diagnosis.entity.AiKnowledgeBase;
import com.neusoft.cloud_brain_diagnosis.repository.AiChatRecordRepository;
import com.neusoft.cloud_brain_diagnosis.repository.AiKnowledgeBaseRepository;
import com.neusoft.cloud_brain_diagnosis.service.ai.impl.AiChatServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.mockito.ArgumentMatcher;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiChatServiceImplTest {

    @Mock private AiChatRecordRepository chatRecordRepository;
    @Mock private AiKnowledgeBaseRepository knowledgeBaseRepository;
    @Mock private AiApiUtil aiApiUtil;

    private AiChatServiceImpl chatService;

    @BeforeEach
    void setUp() {
        chatService = new AiChatServiceImpl(chatRecordRepository, knowledgeBaseRepository, aiApiUtil);
    }

    // ========== chat() - 知识库匹配 ==========

    @Test
    void chat_ShouldMatchKnowledgeBase_WhenScoreMeetsThreshold() {
        AiKnowledgeBase kb = new AiKnowledgeBase();
        kb.setId(1L);
        kb.setQuestion("如何挂号");
        kb.setAnswer("您可以通过微信公众号或现场挂号");
        kb.setKeywords("挂号,预约");
        kb.setStatus(1);
        kb.setCategory("挂号");

        when(knowledgeBaseRepository.findByStatus(1)).thenReturn(List.of(kb));
        when(knowledgeBaseRepository.findByCategory(anyString(), any())).thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));
        when(chatRecordRepository.save(any())).thenAnswer(inv -> {
            AiChatRecord r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        Map<String, Object> result = chatService.chat("我想挂号怎么办", "session-1", 1L, "patient");

        assertEquals("您可以通过微信公众号或现场挂号", result.get("answer"));
        assertEquals("knowledge", result.get("source"));
        verify(chatRecordRepository).save(any());
    }

    @Test
    void chat_ShouldReturnAiResponse_WhenNoKnowledgeMatched() {
        when(knowledgeBaseRepository.findByStatus(1)).thenReturn(Collections.emptyList());
        when(aiApiUtil.callAi(anyString(), anyString()))
                .thenReturn("{\"answer\":\"建议您前往导诊台咨询\",\"relatedQuestions\":[\"相关问题1\"]}");
        when(chatRecordRepository.save(any())).thenAnswer(inv -> {
            AiChatRecord r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        Map<String, Object> result = chatService.chat("不知道该看什么科", "session-2", 1L, "patient");

        assertEquals("建议您前往导诊台咨询", result.get("answer"));
        assertEquals("ai", result.get("source"));
        verify(aiApiUtil).callAi(anyString(), anyString());
    }

    @Test
    void chat_ShouldFallbackToAiResponse_WhenJsonParseFails() {
        when(knowledgeBaseRepository.findByStatus(1)).thenReturn(Collections.emptyList());
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn("AI返回的纯文本内容");
        when(chatRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = chatService.chat("测试问题", "session-3", 1L, "patient");

        assertEquals("AI返回的纯文本内容", result.get("answer"));
        assertEquals("ai", result.get("source"));
    }

    // ========== feedback() ==========

    @Test
    void feedback_ShouldUpdateRecord_WhenRecordExists() {
        AiChatRecord record = new AiChatRecord();
        record.setId(1L);

        when(chatRecordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(chatRecordRepository.save(any())).thenReturn(record);

        String result = chatService.feedback(1L, "helpful");

        assertEquals("反馈成功", result);
        verify(chatRecordRepository).save(argThat(r -> "helpful".equals(r.getFeedback())));
    }

    @Test
    void feedback_ShouldThrow_WhenRecordNotFound() {
        when(chatRecordRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> chatService.feedback(99L, "helpful"));
    }

    // ========== healthConsult() ==========

    @Test
    void healthConsult_ShouldReturnParsedResponse() {
        String aiResponse = "{\"answer\":\"建议多喝水\",\"relatedQuestions\":[\"问题1\"],\"recommendDepartment\":\"内科\",\"recommendDepartmentId\":1}";
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn(aiResponse);
        when(chatRecordRepository.save(any())).thenAnswer(inv -> {
            AiChatRecord r = inv.getArgument(0);
            r.setId(200L);
            return r;
        });

        Map<String, Object> result = chatService.healthConsult("有点头疼", 1L, false);

        assertEquals("建议多喝水", result.get("answer"));
        assertEquals("内科", result.get("recommendDepartment"));
        assertNotNull(result.get("id"));
    }

    @Test
    void healthConsult_ShouldIncludeHistory_WhenRequested() {
        when(aiApiUtil.callAi(argThat((String s) -> s.contains("患者ID")), anyString())).thenReturn("{\"answer\":\"建议\"}");
        when(chatRecordRepository.save(any())).thenAnswer(inv -> {
            AiChatRecord r = inv.getArgument(0);
            r.setId(200L);
            return r;
        });

        chatService.healthConsult("头疼", 1L, true);

        verify(aiApiUtil).callAi(argThat((String s) -> s.contains("患者ID：1") && s.contains("头疼")), anyString());
    }

    @Test
    void healthConsult_ShouldHandleParseError() {
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn("非JSON响应");
        when(chatRecordRepository.save(any())).thenAnswer(inv -> {
            AiChatRecord r = inv.getArgument(0);
            r.setId(200L);
            return r;
        });

        Map<String, Object> result = chatService.healthConsult("头疼", 1L, false);

        assertEquals("非JSON响应", result.get("answer"));
    }

    // ========== chat() - 知识库评分算法分支覆盖 ==========

    @Test
    void chat_ShouldReturnNull_WhenQuestionIsNull() {
        Map<String, Object> result = chatService.chat(null, "session", 1L, "patient");

        assertEquals("ai", result.get("source"));
    }

    @Test
    void chat_ShouldReturnNull_WhenQuestionIsBlank() {
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn("{\"answer\":\"空白问题\"}");
        when(chatRecordRepository.save(any())).thenAnswer(inv -> {
            AiChatRecord r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        Map<String, Object> result = chatService.chat("   ", "session", 1L, "patient");

        assertEquals("ai", result.get("source"));
    }

    @Test
    void chat_ShouldMatchKnowledge_WithRelatedQuestions() {
        AiKnowledgeBase kb = new AiKnowledgeBase();
        kb.setId(5L);
        kb.setQuestion("如何退号");
        kb.setAnswer("在个人中心退号");
        kb.setKeywords("退号,取消预约");
        kb.setStatus(1);
        kb.setCategory("挂号");
        kb.setSort(1);

        when(knowledgeBaseRepository.findByStatus(1)).thenReturn(List.of(kb));
        when(knowledgeBaseRepository.findByCategory(eq("挂号"), any())).thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(kb)));
        when(chatRecordRepository.save(any())).thenAnswer(inv -> {
            AiChatRecord r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        Map<String, Object> result = chatService.chat("怎么退号", "session-1", 1L, "patient");

        assertEquals("knowledge", result.get("source"));
        assertNotNull(result.get("relatedQuestions"));
    }

    @Test
    void chat_ShouldFallbackToAi_WhenNoKnowledgeMatches() {
        when(knowledgeBaseRepository.findByStatus(1)).thenReturn(Collections.emptyList());
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn("{\"answer\":\"建议咨询前台\"}");
        when(chatRecordRepository.save(any())).thenAnswer(inv -> {
            AiChatRecord r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        Map<String, Object> result = chatService.chat("xyzabc", "session", 1L, "patient");

        assertEquals("ai", result.get("source"));
        assertEquals("建议咨询前台", result.get("answer"));
    }

    @Test
    void chat_ShouldFallbackToAi_WhenScoreBelowThreshold() {
        // KB with score < 12 for the question
        AiKnowledgeBase kb = new AiKnowledgeBase();
        kb.setId(1L);
        kb.setQuestion("挂号流程");
        kb.setAnswer("通过公众号");
        kb.setKeywords("挂号");
        kb.setStatus(1);

        when(knowledgeBaseRepository.findByStatus(1)).thenReturn(List.of(kb));
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn("{\"answer\":\"建议\"}");
        when(chatRecordRepository.save(any())).thenAnswer(inv -> {
            AiChatRecord r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        // "退xyz" doesn't match "挂号" so score < 12, falls back to AI
        Map<String, Object> result = chatService.chat("退xyz", "session", 1L, "patient");

        assertEquals("ai", result.get("source"));
    }

    @Test
    void chat_ShouldMatchKnowledge_WhenQuestionTextExactlyMatches() {
        AiKnowledgeBase kb = new AiKnowledgeBase();
        kb.setId(2L);
        kb.setQuestion("体检");
        kb.setAnswer("体检在门诊三楼");
        kb.setKeywords("体检,检查");
        kb.setStatus(1);
        kb.setCategory("科室位置");
        kb.setSort(1);

        when(knowledgeBaseRepository.findByStatus(1)).thenReturn(List.of(kb));
        when(knowledgeBaseRepository.findByCategory(eq("科室位置"), any())).thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(kb)));
        when(chatRecordRepository.save(any())).thenAnswer(inv -> {
            AiChatRecord r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        Map<String, Object> result = chatService.chat("体检", "session", 1L, "patient");

        assertEquals("knowledge", result.get("source"));
        assertEquals("体检在门诊三楼", result.get("answer"));
    }

    @Test
    void chat_ShouldMatchKnowledge_WhenKeywordMatches() {
        AiKnowledgeBase kb = new AiKnowledgeBase();
        kb.setId(3L);
        kb.setQuestion("挂号在哪里");
        kb.setAnswer("公众号挂号");
        kb.setKeywords("挂号");
        kb.setStatus(1);
        kb.setCategory("挂号");

        when(knowledgeBaseRepository.findByStatus(1)).thenReturn(List.of(kb));
        when(knowledgeBaseRepository.findByCategory(eq("挂号"), any())).thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(kb)));
        when(chatRecordRepository.save(any())).thenAnswer(inv -> {
            AiChatRecord r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        Map<String, Object> result = chatService.chat("如何挂号", "session", 1L, "patient");

        assertEquals("knowledge", result.get("source"));
    }

    @Test
    void chat_ShouldNotMatchKnowledge_WhenKeywordsAreNull() {
        AiKnowledgeBase kb = new AiKnowledgeBase();
        kb.setId(4L);
        kb.setQuestion("测试问题");
        kb.setAnswer("答案");
        kb.setKeywords(null);
        kb.setStatus(1);

        when(knowledgeBaseRepository.findByStatus(1)).thenReturn(List.of(kb));
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn("{\"answer\":\"AI回答\"}");
        when(chatRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = chatService.chat("测试", "session", 1L, "patient");

        assertEquals("ai", result.get("source"));
    }

    @Test
    void chat_ShouldMatchBestKnowledge_WhenMultipleMatches() {
        AiKnowledgeBase lowScore = new AiKnowledgeBase();
        lowScore.setId(1L);
        lowScore.setQuestion("挂号");
        lowScore.setAnswer("公众号");
        lowScore.setKeywords("挂号");
        lowScore.setStatus(1);
        lowScore.setSort(1);

        AiKnowledgeBase highScore = new AiKnowledgeBase();
        highScore.setId(2L);
        highScore.setQuestion("如何挂号");
        highScore.setAnswer("公众号挂号");
        highScore.setKeywords("挂号,预约");
        highScore.setStatus(1);
        highScore.setSort(2);

        when(knowledgeBaseRepository.findByStatus(1)).thenReturn(List.of(lowScore, highScore));
        when(chatRecordRepository.save(any())).thenAnswer(inv -> {
            AiChatRecord r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        Map<String, Object> result = chatService.chat("如何挂号", "session", 1L, "patient");

        assertEquals("knowledge", result.get("source"));
        assertEquals("公众号挂号", result.get("answer"));
    }

    @Test
    void healthConsult_ShouldHandleNullResponse() {
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn(null);
        when(chatRecordRepository.save(any())).thenAnswer(inv -> {
            AiChatRecord r = inv.getArgument(0);
            r.setId(200L);
            return r;
        });

        Map<String, Object> result = chatService.healthConsult("头疼", 1L, false);

        assertNotNull(result.get("answer"));
    }

    @Test
    void chat_ShouldHandleNullUserIdAndType() {
        when(knowledgeBaseRepository.findByStatus(1)).thenReturn(Collections.emptyList());
        when(aiApiUtil.callAi(anyString(), anyString())).thenReturn("{\"answer\":\"回答\"}");
        when(chatRecordRepository.save(any())).thenAnswer(inv -> {
            AiChatRecord r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        Map<String, Object> result = chatService.chat("问题", "session", null, null);

        assertEquals("ai", result.get("source"));
        verify(chatRecordRepository).save(argThat(r -> r.getUserId() == null && r.getUserType() == null));
    }
}
