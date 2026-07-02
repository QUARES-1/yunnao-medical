package com.neusoft.cloud_brain_diagnosis.service.ai.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.common.util.AiApiUtil;
import com.neusoft.cloud_brain_diagnosis.entity.Doctor;
import com.neusoft.cloud_brain_diagnosis.entity.PrescriptionAiReview;
import com.neusoft.cloud_brain_diagnosis.repository.DoctorRepository;
import com.neusoft.cloud_brain_diagnosis.repository.PrescriptionAiReviewRepository;
import com.neusoft.cloud_brain_diagnosis.repository.PrescriptionRepository;
import com.neusoft.cloud_brain_diagnosis.service.NotificationService;
import com.neusoft.cloud_brain_diagnosis.service.ai.AiPrescriptionService;
import com.neusoft.cloud_brain_diagnosis.service.ai.PromptTemplateService;
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
    private final PrescriptionRepository prescriptionRepository;
    private final AiApiUtil aiApiUtil;
    private final PromptTemplateService promptTemplateService;
    private final DoctorRepository doctorRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public Map<String, Object> checkPrescription(Map<String, Object> request, Long doctorId) {
        Long patientId = request.get("patientId") != null ? ((Number) request.get("patientId")).longValue() : null;
        Integer patientAge = request.get("patientAge") != null ? ((Number) request.get("patientAge")).intValue() : null;
        String patientGender = (String) request.get("patientGender");
        String allergyHistory = request.get("allergyHistory") == null ? "" : String.valueOf(request.get("allergyHistory"));
        List<Map<String, Object>> drugs = (List<Map<String, Object>>) request.get("drugs");

        // 获取医生科室信息，用于选择科室专属提示词模板
        String departmentName = getDoctorDepartmentName(doctorId);

        // 1. 组装患者信息和药品描述
        String patientInfo = "患者信息：年龄" + patientAge + "岁，性别" + patientGender + "，过敏史：" + (allergyHistory.isBlank() ? "未提供" : allergyHistory);
        StringBuilder drugsDesc = new StringBuilder();
        if (drugs != null) {
            for (Map<String, Object> drug : drugs) {
                Object drugName = drug.get("name") != null ? drug.get("name") : drug.get("medicineName");
                drugsDesc.append("- ").append(drugName).append(" ")
                        .append(drug.get("specification")).append(" ")
                        .append(drug.get("dosage")).append("\n");
                if (!drug.containsKey("name") && drugName != null) {
                    drug.put("name", drugName);
                }
            }
        }

        // 2. 使用科室专属模板
        Map<String, String> templates = promptTemplateService.getPrescriptionReviewTemplate(
                departmentName, patientInfo, drugsDesc.toString());
        String prompt = templates.get("user");
        String systemPrompt = templates.get("system");

        String aiResponse = aiApiUtil.callAi(prompt, systemPrompt);

        // 3. 解析AI返回
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

        // 4. 明确禁忌规则兜底：例如青霉素过敏患者使用阿莫西林，必须标为过敏风险。
        JSONArray allergyRiskArray = allergyRisks == null || allergyRisks.isBlank()
                ? new JSONArray()
                : JSONUtil.parseArray(allergyRisks);
        if (hasPenicillinAllergyRisk(allergyHistory, drugs, allergyRiskArray)) {
            allergyRisks = allergyRiskArray.toString();
            reviewResult = "reject";
            reviewScore = Math.min(reviewScore, 55);
            suggestions = (suggestions == null || suggestions.isBlank() ? "" : suggestions + "；")
                    + "患者存在青霉素类药物过敏史，处方中含阿莫西林/青霉素类药物，建议立即替换为非青霉素类抗菌药并重新审核。";
        }

        // 5. 保存审核记录
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

        // 5. 高风险 → WebSocket 实时告警推送
        if ("reject".equals(reviewResult) || reviewScore < 60) {
            List<Map<String, Object>> warningList = parseWarningList(warnings);
            notificationService.notifyHighRiskMedication(doctorId, review.getId(), warningList, suggestions);
        } else if ("warning".equals(reviewResult) && reviewScore < 80) {
            // 中等风险也推送提醒
            List<Map<String, Object>> warningList = parseWarningList(warnings);
            notificationService.notifyMediumRiskMedication(doctorId, review.getId(), warningList, suggestions);
        }

        // 6. 构建返回
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


    /**
     * 硬规则识别典型过敏禁忌，避免 AI 漏判影响医生端演示和真实业务安全。
     */
    private boolean hasPenicillinAllergyRisk(String allergyHistory, List<Map<String, Object>> drugs, JSONArray allergyRiskArray) {
        if (allergyHistory == null || drugs == null) {
            return false;
        }
        String allergy = allergyHistory.toLowerCase(Locale.ROOT);
        boolean penicillinAllergy = allergy.contains("青霉素") || allergy.contains("penicillin");
        if (!penicillinAllergy) {
            return false;
        }
        boolean added = false;
        for (Map<String, Object> drug : drugs) {
            Object drugNameObj = drug.get("name") != null ? drug.get("name") : drug.get("medicineName");
            String drugName = drugNameObj == null ? "" : String.valueOf(drugNameObj);
            String normalized = drugName.toLowerCase(Locale.ROOT);
            if (drugName.contains("阿莫西林") || drugName.contains("青霉素") || normalized.contains("amoxicillin") || normalized.contains("penicillin")) {
                JSONObject risk = new JSONObject();
                risk.set("drug", drugName);
                risk.set("level", "high");
                risk.set("issue", "患者有青霉素类药物过敏史，当前药品属于青霉素类/β-内酰胺类相关药物，存在明确过敏风险。");
                risk.set("suggestion", "禁止直接发药，建议医生更换非青霉素类替代药物后重新开方。");
                allergyRiskArray.add(risk);
                added = true;
            }
        }
        return added;
    }
    /**
     * 获取医生的科室名称
     */
    private String getDoctorDepartmentName(Long doctorId) {
        if (doctorId == null) return null;
        try {
            return doctorRepository.findById(doctorId)
                    .map(Doctor::getDepartmentName)
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 解析 JSON 告警字符串为 List，兼容泛型擦除
     */
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
