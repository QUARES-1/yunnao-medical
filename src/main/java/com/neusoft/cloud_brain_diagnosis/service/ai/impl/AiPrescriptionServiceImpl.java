package com.neusoft.cloud_brain_diagnosis.service.ai.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.common.util.AiApiUtil;
import com.neusoft.cloud_brain_diagnosis.entity.PrescriptionAiReview;
import com.neusoft.cloud_brain_diagnosis.entity.Prescription;
import com.neusoft.cloud_brain_diagnosis.repository.PrescriptionAiReviewRepository;
import com.neusoft.cloud_brain_diagnosis.repository.PrescriptionRepository;
import com.neusoft.cloud_brain_diagnosis.service.ai.AiPrescriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AiPrescriptionServiceImpl implements AiPrescriptionService {

    private final PrescriptionAiReviewRepository reviewRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final AiApiUtil aiApiUtil;

    @Override
    @Transactional
    public Map<String, Object> checkPrescription(Map<String, Object> request, Long doctorId) {
        Long patientId = request.get("patientId") != null ? ((Number) request.get("patientId")).longValue() : null;
        Integer patientAge = request.get("patientAge") != null ? ((Number) request.get("patientAge")).intValue() : null;
        String patientGender = (String) request.get("patientGender");
        List<Map<String, Object>> drugs = (List<Map<String, Object>>) request.get("drugs");

        // 1. 组装提示词
        StringBuilder prompt = new StringBuilder();
        prompt.append("请审核以下处方：\n");
        prompt.append("患者信息：年龄").append(patientAge).append("岁，性别").append(patientGender).append("\n");
        prompt.append("药品列表：\n");
        if (drugs != null) {
            for (Map<String, Object> drug : drugs) {
                prompt.append("- ").append(drug.get("name")).append(" ")
                        .append(drug.get("specification")).append(" ")
                        .append(drug.get("dosage")).append("\n");
            }
        }

        String systemPrompt = "你是一名经验丰富的临床药师，请严格审核处方中的药品。"
                + "检查要点：药物相互作用、配伍禁忌、剂量合理性、过敏风险、重复用药。"
                + "请按以下JSON格式返回审核结果："
                + "{\"reviewResult\":\"pass/warning/reject\",\"reviewScore\":0-100,"
                + "\"warnings\":[{\"level\":\"low/medium/high\",\"content\":\"警告内容\"}],"
                + "\"suggestions\":\"修改建议\","
                + "\"drugInteractions\":[{\"drug1\":\"药品A\",\"drug2\":\"药品B\",\"level\":\"low/moderate/high\",\"description\":\"相互作用描述\"}],"
                + "\"allergyRisks\":[],"
                + "\"dosageIssues\":[{\"drug\":\"药品名\",\"issue\":\"问题\",\"suggestion\":\"建议\"}]}";

        String aiResponse = aiApiUtil.callAi(prompt.toString(), systemPrompt);

        // 2. 解析AI返回
        Map<String, Object> result = new HashMap<>();
        String reviewResult = "warning";
        int reviewScore = 80;
        String warnings = "";
        String suggestions = "";
        String drugInteractions = "";
        String allergyRisks = "";
        String dosageIssues = "";

        try {
            JSONObject json = JSONUtil.parseObj(aiResponse);
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
        } catch (Exception e) {
            // 解析失败使用默认值
        }

        // 3. 保存审核记录
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

        // 4. 构建返回
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
}
