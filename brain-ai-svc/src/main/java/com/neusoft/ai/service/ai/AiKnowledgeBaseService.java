package com.neusoft.ai.service.ai;

import com.neusoft.ai.entity.AiKnowledgeBase;
import org.springframework.data.domain.Page;
import java.util.Map;

public interface AiKnowledgeBaseService {
    Page<AiKnowledgeBase> getKnowledgeList(String category, String keyword, Integer page, Integer size);
    String addKnowledge(AiKnowledgeBase knowledge);
    String updateKnowledge(AiKnowledgeBase knowledge);
    String deleteKnowledge(Long id);
    Map<String, Object> searchKnowledge(String question);
}
