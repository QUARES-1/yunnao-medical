package com.neusoft.cloud_brain_diagnosis.repository;

import com.neusoft.cloud_brain_diagnosis.entity.AiKnowledgeBase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiKnowledgeBaseRepository extends JpaRepository<AiKnowledgeBase, Long> {
    List<AiKnowledgeBase> findByStatus(Integer status);
    Page<AiKnowledgeBase> findByCategory(String category, Pageable pageable);
    Page<AiKnowledgeBase> findByQuestionContainingOrKeywordsContaining(String question, String keywords, Pageable pageable);
    List<AiKnowledgeBase> findByCategoryAndStatus(String category, Integer status);
}
