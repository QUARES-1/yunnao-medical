package com.neusoft.ai.repository;

import com.neusoft.ai.entity.AiKnowledgeBase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AiKnowledgeBaseRepository extends JpaRepository<AiKnowledgeBase, Long> {
    List<AiKnowledgeBase> findByStatus(Integer status);
    Page<AiKnowledgeBase> findByCategory(String category, Pageable pageable);
    Page<AiKnowledgeBase> findByQuestionContainingOrKeywordsContaining(String question, String keywords, Pageable pageable);
}
