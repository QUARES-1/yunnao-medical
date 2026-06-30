package com.neusoft.ai.service.ai.impl;

import cn.hutool.json.JSONUtil;
import com.neusoft.ai.common.exception.BusinessException;
import com.neusoft.ai.common.util.AiApiUtil;
import com.neusoft.ai.entity.PrescriptionAiReview;
import com.neusoft.ai.feign.DoctorClient;
import com.neusoft.ai.feign.NotificationClient;
import com.neusoft.ai.repository.PrescriptionAiReviewRepository;
import com.neusoft.ai.service.ai.AiPrescriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AiPrescriptionServiceImpl implements AiPrescriptionService {

    private final PrescriptionAiReviewRepository reviewRepository;
    private final AiApiUtil aiApiUtil;
    private final PromptTemplateServiceImpl promptTemplateService;
    private final DoctorClient doctorClient;
    private final NotificationClient notificationClient;

    @Override
    @Transactional
    public Map<String, Object> checkPrescription(Map<String, Object> request, Long doctorId) {
        Long patientId = request.get("patientId") != null ? ((Number) request.get("patientId")).longValue() : null;
        Integer patientAge = request.get("patientAge") != null ? ((Number) request.get("patientAge")).intValue() : null;
        String patientGender = (String) request.get("patientGender");
        List<Map<String, Object>> drugs = (List<Map<String, Object>>) request.get("drugs");

        String departmentName = getDoctorDepartmentName(doctorId);

        String patientInfo = "患者信息：年龄" + patientAge + "岁，性别" + patientGender;
        StringBuilder drugsDesc = new StringBuilder();
        if (drugs != null) {
            for (Map<String, Object> drug : drugs) {
                drugsDesc.append("- ").append(drug.get("name")).append(" ")
                        .append(drug.get("specification")).append(" ")
                        .append(drug.get("dosage")).append("\n");
            }
        }

        Map<String, String> templates = promptTemplateService.getPrescriptionReviewTemplate(
                departmentName, patientInfo, drugsDesc.toString());
        String prompt = templates.get("user");
        String systemPrompt = templates.get("system");

        String aiResponse = aiApiUtil.callAi(prompt, systemPrompt);

        Map<String, Object> result = new HashMap<>();
        String reviewResult = "warning";
        int reviewScore = 80;
        String warnings = "";
        String suggestions = "";
        String drugInteractions = "";
        String allergyRisks = "";
        String dosageIssues = "";

        try {
            cn.hutool.json.JSONObject json = JSONUtil.parseObj(aiResponse);
            reviewResult = json.getStr("reviewResult", "warning");
            reviewScore = json.getInt("reviewScore", 80);
            suggestions = json.getStr("suggestions", "");

            if (json.getJSONArray("warnings") != null) {
                warnings = json.getJSONArray("warnings").toString();
            }
            if (json.getJSONArray("drugInteractions") != null) {
                drugInteractions = json.getJSONArray("drugInteractions").toString();
            }
            if (json.getJSONArray("allergyRisks") != null) {
                allergyRisks = json.getJSONArray("allergyRisks").toString();
            }
            if (json.getJSONArray("dosageIssues") != null) {
                dosageIssues = json.getJSONArray("dosageIssues").toString();
            }
        } catch (Exception ignored) {}

        PrescriptionAiReview review = new PrescriptionAiReview();
        review.setDoctorId(doctorId);
        review.setPatientId(patientId);
        review.setPatientAge(patientAge);
        review.setPatientGender(patientGender);
        review.setDrugsJson(drugs != null ? JSONUtil.toJsonStr(drugs) : null);
        review.setReviewResult(reviewResult);
        review.setReviewScore(reviewScore);
        review.setWarnings(warnings);
        review.setSuggestions(suggestions);
        review.setDrugInteractions(drugInteractions);
        review.setAllergyRisks(allergyRisks);
        review.setDosageIssues(dosageIssues);
        review.setRawResponse(aiResponse);
        reviewRepository.save(review);

        // 高风险/中风险 → 通过 Feign 回调主服务发送 WebSocket 通知
        if ("reject".equals(reviewResult) || reviewScore < 60) {
            try {
                List<Map<String, Object>> warningList = parseWarningList(warnings);
                notificationClient.notifyHighRisk(doctorId, review.getId(), warningList, suggestions);
            } catch (Exception ignored) {}
        } else if ("warning".equals(reviewResult) && reviewScore < 80) {
            try {
                List<Map<String, Object>> warningList = parseWarningList(warnings);
                notificationClient.notifyMediumRisk(doctorId, review.getId(), warningList, suggestions);
            } catch (Exception ignored) {}
        }

        result.put("id", review.getId());
        result.put("reviewResult", reviewResult);
        result.put("reviewScore", reviewScore);
        try {
            if (!warnings.isEmpty()) {
                result.put("warnings", JSONUtil.parseArray(warnings));
            } else {
                result.put("warnings", new ArrayList<>());
            }
            if (!drugInteractions.isEmpty()) {
                result.put("drugInteractions", JSONUtil.parseArray(drugInteractions));
            } else {
                result.put("drugInteractions", new ArrayList<>());
            }
            if (!allergyRisks.isEmpty()) {
                result.put("allergyRisks", JSONUtil.parseArray(allergyRisks));
            } else {
                result.put("allergyRisks", new ArrayList<>());
            }
            if (!dosageIssues.isEmpty()) {
                result.put("dosageIssues", JSONUtil.parseArray(dosageIssues));
            } else {
                result.put("dosageIssues", new ArrayList<>());
            }
        } catch (Exception e) {
            result.put("warnings", new ArrayList<>());
            result.put("drugInteractions", new ArrayList<>());
            result.put("allergyRisks", new ArrayList<>());
            result.put("dosageIssues", new ArrayList<>());
        }
        result.put("suggestions", suggestions);

        return result;
    }

    @Override
    public Page<PrescriptionAiReview> getReviewList(Long doctorId, Integer page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "reviewTime"));
        if (doctorId != null) {
            return reviewRepository.findByDoctorIdOrderByReviewTimeDesc(doctorId, pageRequest);
        }
        return reviewRepository.findByOrderByReviewTimeDesc(pageRequest);
    }

    @Override
    public PrescriptionAiReview getReviewDetail(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new BusinessException("审核记录不存在"));
    }

    private String getDoctorDepartmentName(Long doctorId) {
        if (doctorId == null) return null;
        try {
            var result = doctorClient.getDoctor(doctorId);
            if (result != null && result.getData() != null) {
                return (String) result.getData().get("departmentName");
            }
        } catch (Exception ignored) {}
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseWarningList(String warningsJson) {
        if (warningsJson == null || warningsJson.isBlank()) {
            return new ArrayList<>();
        }
        try {
            cn.hutool.json.JSONArray arr = JSONUtil.parseArray(warningsJson);
            List<Map<String, Object>> result = new ArrayList<>();
            for (int i = 0; i < arr.size(); i++) {
                result.add((Map<String, Object>) arr.get(i));
            }
            return result;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
