package com.neusoft.cloud_brain_diagnosis.service.ai;

import com.neusoft.cloud_brain_diagnosis.entity.AiChatRecord;
import org.springframework.data.domain.Page;

import java.util.Map;

public interface AiChatService {
    /**
     * 患者提问
     */
    Map<String, Object> chat(String question, String sessionId, Long userId, String userType);

    /**
     * 获取问答历史
     */
    Page<AiChatRecord> getChatHistory(Long userId, String userType, Integer page, Integer size);

    /**
     * 获取所有问答日志（管理员）
     */
    Page<AiChatRecord> getChatLogs(Integer page, Integer size);

    /**
     * 反馈评价
     */
    String feedback(Long recordId, String feedback);

    /**
     * 健康咨询（可结合历史病历）
     */
    Map<String, Object> healthConsult(String question, Long patientId, Boolean includeHistory);
}
