package com.neusoft.ai.service.ai.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.neusoft.ai.common.exception.BusinessException;
import com.neusoft.ai.common.util.AiApiUtil;
import com.neusoft.ai.entity.AiChatRecord;
import com.neusoft.ai.entity.AiKnowledgeBase;
import com.neusoft.ai.repository.AiChatRecordRepository;
import com.neusoft.ai.repository.AiKnowledgeBaseRepository;
import com.neusoft.ai.service.ai.AiChatService;
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

        AiKnowledgeBase matchedKnowledge = findBestKnowledge(question);
        if (matchedKnowledge != null) {
            answer = matchedKnowledge.getAnswer();
            source = "knowledge";
            saveChatRecord(question, answer, source, sessionId, userId, userType);
            Map<String, Object> result = new HashMap<>();
            result.put("answer", answer);
            result.put("source", source);
            result.put("relatedQuestions", findRelatedQuestions(matchedKnowledge.getCategory(), matchedKnowledge.getId()));
            return result;
        }

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

    private AiKnowledgeBase findBestKnowledge(String question) {
        if (question == null || question.trim().isEmpty()) return null;
        String normalizedQuestion = normalize(question);
        List<AiKnowledgeBase> kbList = knowledgeBaseRepository.findByStatus(1);
        AiKnowledgeBase best = null;
        int bestScore = 0;

        for (AiKnowledgeBase kb : kbList) {
            int score = scoreKnowledge(normalizedQuestion, kb);
            if (score > bestScore || (score == bestScore && best != null && compareKnowledge(kb, best) < 0)) {
                bestScore = score;
                best = kb;
            }
        }
        return bestScore >= 12 ? best : null;
    }

    private int scoreKnowledge(String normalizedQuestion, AiKnowledgeBase kb) {
        int score = 0;
        String questionText = normalize(kb.getQuestion());
        String keywordsText = normalize(kb.getKeywords());

        if (!questionText.isEmpty() && normalizedQuestion.contains(questionText)) score += 60;

        String[] keywords = Optional.ofNullable(kb.getKeywords()).orElse("").split("[,，、;；\\s]+");
        for (String rawKeyword : keywords) {
            String keyword = normalize(rawKeyword);
            if (keyword.isEmpty()) continue;
            if (normalizedQuestion.contains(keyword)) {
                score += keyword.length() >= 2 ? 12 + keyword.length() : 4;
            }
        }
        if (!questionText.isEmpty()) {
            for (String token : splitTokens(questionText)) {
                if (normalizedQuestion.contains(token)) {
                    score += Math.min(10, token.length() + 3);
                }
            }
        }
        if (mentionsOnlyOneOf(normalizedQuestion, "彩超", "ct") && containsBoth(questionText + keywordsText, "彩超", "ct")) {
            score -= 18;
        }
        if (containsAny(normalizedQuestion, "在哪", "哪里", "几楼", "楼", "位置", "怎么走")
                && containsAny(questionText + keywordsText, "在哪", "哪里", "几楼", "楼", "位置", "导诊")) {
            score += 8;
        }
        return score;
    }

    private int compareKnowledge(AiKnowledgeBase left, AiKnowledgeBase right) {
        int leftSort = Optional.ofNullable(left.getSort()).orElse(0);
        int rightSort = Optional.ofNullable(right.getSort()).orElse(0);
        if (leftSort != rightSort) return Integer.compare(rightSort, leftSort);
        int leftKeywordCount = Optional.ofNullable(left.getKeywords()).orElse("").split("[,，、;；\\s]+").length;
        int rightKeywordCount = Optional.ofNullable(right.getKeywords()).orElse("").split("[,，、;；\\s]+").length;
        if (leftKeywordCount != rightKeywordCount) return Integer.compare(leftKeywordCount, rightKeywordCount);
        return Long.compare(Optional.ofNullable(left.getId()).orElse(Long.MAX_VALUE),
                Optional.ofNullable(right.getId()).orElse(Long.MAX_VALUE));
    }

    private String normalize(String text) {
        if (text == null) return "";
        return text.toLowerCase(Locale.ROOT)
                .replace("ＣＴ", "ct").replace("ｃｔ", "ct")
                .replaceAll("[\\p{Punct}\\s，。！？、；：,.!?;:：（）()【】\\[\\]《》<>“”\"']", "");
    }

    private List<String> splitTokens(String text) {
        if (text == null || text.isEmpty()) return Collections.emptyList();
        List<String> tokens = new ArrayList<>();
        String[] parts = text.split("[,，、;；\\s]+");
        for (String part : parts) {
            String token = normalize(part);
            if (token.length() >= 2) tokens.add(token);
        }
        return tokens;
    }

    private boolean containsAny(String text, String... words) {
        for (String word : words) {
            if (text.contains(normalize(word))) return true;
        }
        return false;
    }

    private boolean containsBoth(String text, String first, String second) {
        return text.contains(normalize(first)) && text.contains(normalize(second));
    }

    private boolean mentionsOnlyOneOf(String text, String first, String second) {
        boolean hasFirst = text.contains(normalize(first));
        boolean hasSecond = text.contains(normalize(second));
        return hasFirst ^ hasSecond;
    }

    private List<String> findRelatedQuestions(String category, Long currentKnowledgeId) {
        List<String> related = new ArrayList<>();
        if (category != null) {
            List<AiKnowledgeBase> list = knowledgeBaseRepository.findByCategory(category, Pageable.unpaged()).getContent();
            for (AiKnowledgeBase kb : list) {
                if (Objects.equals(kb.getId(), currentKnowledgeId)) continue;
                if (related.size() < 2) related.add(kb.getQuestion());
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
