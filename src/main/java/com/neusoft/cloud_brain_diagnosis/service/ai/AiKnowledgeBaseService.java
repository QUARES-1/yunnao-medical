package com.neusoft.cloud_brain_diagnosis.service.ai;

import com.neusoft.cloud_brain_diagnosis.entity.AiKnowledgeBase;
import org.springframework.data.domain.Page;

import java.util.Map;

public interface AiKnowledgeBaseService {
    /**
     * 获取知识库列表
     */
    Page<AiKnowledgeBase> getKnowledgeList(String category, String keyword, Integer page, Integer size);

    /**
     * 新增知识库条目
     */
    String addKnowledge(AiKnowledgeBase knowledge);

    /**
     * 修改知识库条目
     */
    String updateKnowledge(AiKnowledgeBase knowledge);

    /**
     * 删除知识库条目
     */
    String deleteKnowledge(Long id);

    /**
     * 搜索知识库（根据关键词匹配）
     */
    Map<String, Object> searchKnowledge(String question);
}
