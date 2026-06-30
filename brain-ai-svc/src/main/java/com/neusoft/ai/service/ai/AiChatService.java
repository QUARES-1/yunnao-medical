package com.neusoft.ai.service.ai;

import com.neusoft.ai.entity.AiChatRecord;
import org.springframework.data.domain.Page;
import java.util.Map;

public interface AiChatService {
    Map<String, Object> chat(String question, String sessionId, Long userId, String userType);
    Page<AiChatRecord> getChatHistory(Long userId, String userType, Integer page, Integer size);
    Page<AiChatRecord> getChatLogs(Integer page, Integer size);
    String feedback(Long recordId, String feedback);
    Map<String, Object> healthConsult(String question, Long patientId, Boolean includeHistory);
}
