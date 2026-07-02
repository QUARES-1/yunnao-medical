package com.neusoft.cloud_brain_diagnosis.service.ai;

import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.entity.AiKnowledgeBase;
import com.neusoft.cloud_brain_diagnosis.repository.AiKnowledgeBaseRepository;
import com.neusoft.cloud_brain_diagnosis.service.ai.impl.AiKnowledgeBaseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiKnowledgeBaseServiceImplTest {

    @Mock private AiKnowledgeBaseRepository knowledgeBaseRepository;

    private AiKnowledgeBaseServiceImpl knowledgeBaseService;

    @BeforeEach
    void setUp() {
        knowledgeBaseService = new AiKnowledgeBaseServiceImpl(knowledgeBaseRepository);
    }

    // ========== getKnowledgeList() ==========

    @Test
    void getKnowledgeList_ShouldReturnAll_WhenNoParams() {
        Page<AiKnowledgeBase> page = new PageImpl<>(List.of(new AiKnowledgeBase()));
        when(knowledgeBaseRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<AiKnowledgeBase> result = knowledgeBaseService.getKnowledgeList(null, null, 1, 10);

        assertEquals(1, result.getContent().size());
    }

    @Test
    void getKnowledgeList_ShouldFilterByKeyword_WhenProvided() {
        Page<AiKnowledgeBase> page = new PageImpl<>(List.of(new AiKnowledgeBase()));
        when(knowledgeBaseRepository.findByQuestionContainingOrKeywordsContaining(eq("挂号"), eq("挂号"), any(Pageable.class)))
                .thenReturn(page);

        Page<AiKnowledgeBase> result = knowledgeBaseService.getKnowledgeList(null, "挂号", 1, 10);

        assertEquals(1, result.getContent().size());
    }

    @Test
    void getKnowledgeList_ShouldFilterByCategory_WhenProvided() {
        Page<AiKnowledgeBase> page = new PageImpl<>(List.of(new AiKnowledgeBase()));
        when(knowledgeBaseRepository.findByCategory(eq("挂号"), any(Pageable.class)))
                .thenReturn(page);

        Page<AiKnowledgeBase> result = knowledgeBaseService.getKnowledgeList("挂号", null, 1, 10);

        assertEquals(1, result.getContent().size());
    }

    // ========== addKnowledge() ==========

    @Test
    void addKnowledge_ShouldSaveAndReturn() {
        AiKnowledgeBase knowledge = new AiKnowledgeBase();
        knowledge.setQuestion("如何挂号");
        knowledge.setAnswer("通过公众号挂号");

        when(knowledgeBaseRepository.save(any())).thenReturn(knowledge);

        String result = knowledgeBaseService.addKnowledge(knowledge);

        assertEquals("添加成功", result);
        verify(knowledgeBaseRepository).save(knowledge);
    }

    // ========== updateKnowledge() ==========

    @Test
    void updateKnowledge_ShouldUpdateExistingRecord() {
        AiKnowledgeBase existing = new AiKnowledgeBase();
        existing.setId(1L);
        existing.setQuestion("原问题");

        when(knowledgeBaseRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(knowledgeBaseRepository.save(any())).thenReturn(existing);

        AiKnowledgeBase update = new AiKnowledgeBase();
        update.setId(1L);
        update.setQuestion("新问题");
        update.setAnswer("新答案");

        String result = knowledgeBaseService.updateKnowledge(update);

        assertEquals("修改成功", result);
        verify(knowledgeBaseRepository).save(argThat(k -> "新问题".equals(k.getQuestion())));
    }

    @Test
    void updateKnowledge_ShouldThrow_WhenNotFound() {
        when(knowledgeBaseRepository.findById(99L)).thenReturn(Optional.empty());

        AiKnowledgeBase update = new AiKnowledgeBase();
        update.setId(99L);

        assertThrows(BusinessException.class, () -> knowledgeBaseService.updateKnowledge(update));
    }

    // ========== deleteKnowledge() ==========

    @Test
    void deleteKnowledge_ShouldDelete_WhenExists() {
        when(knowledgeBaseRepository.existsById(1L)).thenReturn(true);

        String result = knowledgeBaseService.deleteKnowledge(1L);

        assertEquals("删除成功", result);
        verify(knowledgeBaseRepository).deleteById(1L);
    }

    @Test
    void deleteKnowledge_ShouldThrow_WhenNotFound() {
        when(knowledgeBaseRepository.existsById(99L)).thenReturn(false);

        assertThrows(BusinessException.class, () -> knowledgeBaseService.deleteKnowledge(99L));
    }

    // ========== searchKnowledge() ==========

    @Test
    void searchKnowledge_ShouldReturnMatched_WhenKeywordFound() {
        AiKnowledgeBase kb = new AiKnowledgeBase();
        kb.setId(1L);
        kb.setQuestion("如何挂号");
        kb.setAnswer("通过公众号");
        kb.setKeywords("挂号,预约");
        kb.setCategory("挂号");
        kb.setStatus(1);

        when(knowledgeBaseRepository.findByStatus(1)).thenReturn(List.of(kb));

        Map<String, Object> result = knowledgeBaseService.searchKnowledge("我想挂号怎么办");

        assertEquals(true, result.get("matched"));
        assertEquals("如何挂号", result.get("question"));
        assertEquals("通过公众号", result.get("answer"));
    }

    @Test
    void searchKnowledge_ShouldReturnNotMatched_WhenNoMatch() {
        when(knowledgeBaseRepository.findByStatus(1)).thenReturn(Collections.emptyList());

        Map<String, Object> result = knowledgeBaseService.searchKnowledge("未知问题");

        assertEquals(false, result.get("matched"));
    }

    @Test
    void searchKnowledge_ShouldHandleMultipleKeywords() {
        AiKnowledgeBase kb = new AiKnowledgeBase();
        kb.setId(1L);
        kb.setQuestion("体检在哪");
        kb.setAnswer("体检中心在三楼");
        kb.setKeywords("体检,检查");
        kb.setCategory("科室位置");
        kb.setStatus(1);

        when(knowledgeBaseRepository.findByStatus(1)).thenReturn(List.of(kb));

        Map<String, Object> result = knowledgeBaseService.searchKnowledge("体检");

        assertEquals(true, result.get("matched"));
    }

    // ========== Branch & Edge Case Coverage ==========

    @Test
    void getKnowledgeList_ShouldFilterByKeyword_AndCategory_WhenBothProvided() {
        Page<AiKnowledgeBase> page = new PageImpl<>(List.of(new AiKnowledgeBase()));
        when(knowledgeBaseRepository.findByQuestionContainingOrKeywordsContaining(eq("挂号"), eq("挂号"), any(Pageable.class)))
                .thenReturn(page);

        Page<AiKnowledgeBase> result = knowledgeBaseService.getKnowledgeList("科室", "挂号", 1, 10);

        assertEquals(1, result.getContent().size());
    }

    @Test
    void updateKnowledge_ShouldUpdateAllFields() {
        AiKnowledgeBase exist = new AiKnowledgeBase();
        exist.setId(1L);
        exist.setCategory("旧类");
        exist.setQuestion("旧问题");
        exist.setAnswer("旧答案");

        AiKnowledgeBase update = new AiKnowledgeBase();
        update.setId(1L);
        update.setCategory("新类");
        update.setQuestion("新问题");
        update.setAnswer("新答案");
        update.setKeywords("新关键词");
        update.setSort(5);
        update.setStatus(0);

        when(knowledgeBaseRepository.findById(1L)).thenReturn(Optional.of(exist));
        when(knowledgeBaseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = knowledgeBaseService.updateKnowledge(update);

        assertEquals("修改成功", result);
        assertEquals("新类", exist.getCategory());
        assertEquals("新问题", exist.getQuestion());
        assertEquals("新答案", exist.getAnswer());
        assertEquals("新关键词", exist.getKeywords());
        assertEquals(5, exist.getSort());
        assertEquals(0, exist.getStatus());
    }

    @Test
    void updateKnowledge_ShouldOnlyUpdateNonNullFields() {
        AiKnowledgeBase exist = new AiKnowledgeBase();
        exist.setId(1L);
        exist.setCategory("旧类");
        exist.setQuestion("旧问题");
        exist.setAnswer("旧答案");
        exist.setKeywords("旧关键词");
        exist.setSort(3);
        exist.setStatus(1);

        AiKnowledgeBase update = new AiKnowledgeBase();
        update.setId(1L);
        update.setQuestion("只更新问题");

        when(knowledgeBaseRepository.findById(1L)).thenReturn(Optional.of(exist));
        when(knowledgeBaseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        knowledgeBaseService.updateKnowledge(update);

        assertEquals("旧类", exist.getCategory());
        assertEquals("只更新问题", exist.getQuestion());
        assertEquals("旧答案", exist.getAnswer());
    }

    @Test
    void deleteKnowledge_ShouldSucceed() {
        when(knowledgeBaseRepository.existsById(1L)).thenReturn(true);

        String result = knowledgeBaseService.deleteKnowledge(1L);

        assertEquals("删除成功", result);
        verify(knowledgeBaseRepository).deleteById(1L);
    }

    @Test
    void searchKnowledge_ShouldReturnMatchedResult() {
        AiKnowledgeBase kb = new AiKnowledgeBase();
        kb.setId(1L);
        kb.setQuestion("怎么退号");
        kb.setAnswer("在个人中心退号");
        kb.setKeywords("退号,取消");
        kb.setCategory("挂号");
        kb.setStatus(1);

        when(knowledgeBaseRepository.findByStatus(1)).thenReturn(List.of(kb));

        Map<String, Object> result = knowledgeBaseService.searchKnowledge("退号");

        assertEquals(true, result.get("matched"));
        assertEquals("怎么退号", result.get("question"));
        assertEquals("在个人中心退号", result.get("answer"));
        assertEquals("挂号", result.get("category"));
    }

    @Test
    void searchKnowledge_ShouldReturnNotMatched_WhenKeywordsNull() {
        AiKnowledgeBase kb = new AiKnowledgeBase();
        kb.setId(1L);
        kb.setQuestion("问题");
        kb.setAnswer("答案");
        kb.setKeywords(null);
        kb.setStatus(1);

        when(knowledgeBaseRepository.findByStatus(1)).thenReturn(List.of(kb));

        Map<String, Object> result = knowledgeBaseService.searchKnowledge("任何问题");

        assertEquals(false, result.get("matched"));
    }

    @Test
    void searchKnowledge_ShouldReturnNotMatched_WhenKeywordsBlank() {
        AiKnowledgeBase kb = new AiKnowledgeBase();
        kb.setId(1L);
        kb.setQuestion("退号问题");
        kb.setAnswer("答案");
        kb.setKeywords(null);
        kb.setStatus(1);

        when(knowledgeBaseRepository.findByStatus(1)).thenReturn(List.of(kb));

        Map<String, Object> result = knowledgeBaseService.searchKnowledge("完全不相关的查询词");

        assertEquals(false, result.get("matched"));
    }

    @Test
    void searchKnowledge_ShouldTrimKeyword() {
        AiKnowledgeBase kb = new AiKnowledgeBase();
        kb.setId(1L);
        kb.setQuestion("挂号问题");
        kb.setAnswer("答案");
        kb.setKeywords("挂号");
        kb.setStatus(1);

        when(knowledgeBaseRepository.findByStatus(1)).thenReturn(List.of(kb));

        Map<String, Object> result = knowledgeBaseService.searchKnowledge("  挂号  ");

        assertEquals(true, result.get("matched"));
    }
}
