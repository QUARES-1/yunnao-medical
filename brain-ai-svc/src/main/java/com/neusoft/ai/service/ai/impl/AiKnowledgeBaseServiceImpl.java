package com.neusoft.ai.service.ai.impl;

import com.neusoft.ai.common.exception.BusinessException;
import com.neusoft.ai.entity.AiKnowledgeBase;
import com.neusoft.ai.repository.AiKnowledgeBaseRepository;
import com.neusoft.ai.service.ai.AiKnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AiKnowledgeBaseServiceImpl implements AiKnowledgeBaseService {

    private final AiKnowledgeBaseRepository knowledgeBaseRepository;

    @Override
    public Page<AiKnowledgeBase> getKnowledgeList(String category, String keyword, Integer page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.ASC, "sort"));
        if (keyword != null && !keyword.isEmpty()) {
            return knowledgeBaseRepository.findByQuestionContainingOrKeywordsContaining(keyword, keyword, pageRequest);
        }
        if (category != null && !category.isEmpty()) {
            return knowledgeBaseRepository.findByCategory(category, pageRequest);
        }
        return knowledgeBaseRepository.findAll(pageRequest);
    }

    @Override
    @Transactional
    public String addKnowledge(AiKnowledgeBase knowledge) {
        knowledgeBaseRepository.save(knowledge);
        return "添加成功";
    }

    @Override
    @Transactional
    public String updateKnowledge(AiKnowledgeBase knowledge) {
        AiKnowledgeBase exist = knowledgeBaseRepository.findById(knowledge.getId())
                .orElseThrow(() -> new BusinessException("知识条目不存在"));
        if (knowledge.getCategory() != null) exist.setCategory(knowledge.getCategory());
        if (knowledge.getQuestion() != null) exist.setQuestion(knowledge.getQuestion());
        if (knowledge.getAnswer() != null) exist.setAnswer(knowledge.getAnswer());
        if (knowledge.getKeywords() != null) exist.setKeywords(knowledge.getKeywords());
        if (knowledge.getSort() != null) exist.setSort(knowledge.getSort());
        if (knowledge.getStatus() != null) exist.setStatus(knowledge.getStatus());
        knowledgeBaseRepository.save(exist);
        return "修改成功";
    }

    @Override
    @Transactional
    public String deleteKnowledge(Long id) {
        if (!knowledgeBaseRepository.existsById(id)) {
            throw new BusinessException("知识条目不存在");
        }
        knowledgeBaseRepository.deleteById(id);
        return "删除成功";
    }

    @Override
    public Map<String, Object> searchKnowledge(String question) {
        List<AiKnowledgeBase> list = knowledgeBaseRepository.findByStatus(1);
        for (AiKnowledgeBase kb : list) {
            if (kb.getKeywords() != null) {
                String[] keywords = kb.getKeywords().split(",");
                for (String keyword : keywords) {
                    if (question.contains(keyword.trim())) {
                        Map<String, Object> result = new HashMap<>();
                        result.put("matched", true);
                        result.put("question", kb.getQuestion());
                        result.put("answer", kb.getAnswer());
                        result.put("category", kb.getCategory());
                        return result;
                    }
                }
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("matched", false);
        return result;
    }
}
