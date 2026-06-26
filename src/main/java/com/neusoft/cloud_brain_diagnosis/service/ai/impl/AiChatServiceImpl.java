package com.neusoft.cloud_brain_diagnosis.service.ai.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.common.util.AiApiUtil;
import com.neusoft.cloud_brain_diagnosis.entity.AiChatRecord;
import com.neusoft.cloud_brain_diagnosis.entity.AiKnowledgeBase;
import com.neusoft.cloud_brain_diagnosis.repository.AiChatRecordRepository;
import com.neusoft.cloud_brain_diagnosis.repository.AiKnowledgeBaseRepository;
import com.neusoft.cloud_brain_diagnosis.service.ai.AiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private final AiChatRecordRepository chatRecordRepository;
    private final AiKnowledgeBaseRepository knowledgeBaseRepository;
    private final AiApiUtil aiApiUtil;

    @Override
    @Transactional
    public Map<String, Object> chat(String question, String sessionId, Long userId, String userType) {
        String answer;
        String source;

        // 1. 优先匹配知识库
        List<AiKnowledgeBase> kbList = knowledgeBaseRepository.findByStatus(1);
        boolean matched = false;
        for (AiKnowledgeBase kb : kbList) {
            if (kb.getKeywords() != null) {
                String[] keywords = kb.getKeywords().split(",");
                for (String keyword : keywords) {
                    if (question.contains(keyword.trim())) {
                        answer = kb.getAnswer();
                        source = "knowledge";
                        matched = true;
                        // 保存记录
                        saveChatRecord(question, answer, source, sessionId, userId, userType);
                        Map<String, Object> result = new HashMap<>();
                        result.put("answer", answer);
                        result.put("source", source);
                        result.put("relatedQuestions", findRelatedQuestions(kb.getCategory()));
                        return result;
                    }
                }
            }
        }

        // 2. 未匹配则调用AI
        String prompt = "患者问题：" + question;
        String systemPrompt = "你是一家医院的就医智能助手，请回答患者关于挂号、取药、就诊流程等方面的问题。"
                + "请按JSON格式返回：{\"answer\":\"你的回答\",\"relatedQuestions\":[\"相关问题1\",\"相关问题2\"]}";

        String aiResponse = aiApiUtil.callAi(prompt, systemPrompt);
        try {
            JSONObject json = JSONUtil.parseObj(aiResponse);
            answer = json.getStr("answer", aiResponse);
        } catch (Exception e) {
            answer = aiResponse;
        }
        source = "ai";

        // 3. 保存问答记录
        saveChatRecord(question, answer, source, sessionId, userId, userType);

        Map<String, Object> result = new HashMap<>();
        result.put("answer", answer);
        result.put("source", source);
        return result;
    }

    private void saveChatRecord(String question, String answer, String source, String sessionId, Long userId, String userType) {
        AiChatRecord record = new AiChatRecord();
        record.setQuestion(question);
        record.setAnswer(answer);
        record.setSource(source);
        record.setSessionId(sessionId);
        record.setUserId(userId);
        record.setUserType(userType);
        record.setCategory("general");
        chatRecordRepository.save(record);
    }

    private List<String> findRelatedQuestions(String category) {
        List<String> related = new ArrayList<>();
        if (category != null) {
            List<AiKnowledgeBase> list = knowledgeBaseRepository.findByCategory(category, Pageable.unpaged()).getContent();
            // 随机选2个相关问题
            for (AiKnowledgeBase kb : list) {
                if (related.size() < 2) {
                    related.add(kb.getQuestion());
                }
            }
        }
        return related;
    }

    @Override
    public Page<AiChatRecord> getChatHistory(Long userId, String userType, Integer page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        return chatRecordRepository.findByUserIdAndUserTypeOrderByCreateTimeDesc(userId, userType, pageRequest);
    }

    @Override
    public Page<AiChatRecord> getChatLogs(Integer page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        return chatRecordRepository.findByOrderByCreateTimeDesc(pageRequest);
    }

    @Override
    @Transactional
    public String feedback(Long recordId, String feedback) {
        AiChatRecord record = chatRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException("记录不存在"));
        record.setFeedback(feedback);
        chatRecordRepository.save(record);
        return "反馈成功";
    }

    @Override
    @Transactional
    public Map<String, Object> healthConsult(String question, Long patientId, Boolean includeHistory) {
        String prompt = "患者咨询健康问题：" + question;
        if (Boolean.TRUE.equals(includeHistory) && patientId != null) {
            prompt += "\n患者ID：" + patientId + "（请结合患者可能的病史给出个性化建议）";
        }
        String systemPrompt = "你是一名专业的健康顾问医生。请根据患者的问题给出专业的健康建议，"
                + "如果问题严重请推荐对应科室。请按JSON格式返回："
                + "{\"answer\":\"详细的健康建议\",\"relatedQuestions\":[\"相关问题\"],\"recommendDepartment\":\"科室名称\",\"recommendDepartmentId\":1}";

        String aiResponse = aiApiUtil.callAi(prompt, systemPrompt);

        Map<String, Object> result = new HashMap<>();
        try {
            JSONObject json = JSONUtil.parseObj(aiResponse);
            result.put("answer", json.getStr("answer", aiResponse));
            result.put("relatedQuestions", json.getJSONArray("relatedQuestions") != null
                    ? json.getJSONArray("relatedQuestions").toList(String.class) : new ArrayList<>());
            result.put("recommendDepartment", json.getStr("recommendDepartment"));
            result.put("recommendDepartmentId", json.getLong("recommendDepartmentId"));
        } catch (Exception e) {
            result.put("answer", aiResponse);
        }

        // 保存健康咨询记录
        AiChatRecord record = new AiChatRecord();
        record.setQuestion(question);
        record.setAnswer((String) result.get("answer"));
        record.setSource("ai");
        record.setUserId(patientId);
        record.setUserType("patient");
        record.setCategory("health");
        chatRecordRepository.save(record);
        result.put("id", record.getId());

        return result;
    }
}
