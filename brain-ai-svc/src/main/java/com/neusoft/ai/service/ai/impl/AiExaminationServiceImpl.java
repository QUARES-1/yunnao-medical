package com.neusoft.ai.service.ai.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.neusoft.ai.common.exception.BusinessException;
import com.neusoft.ai.common.util.AiApiUtil;
import com.neusoft.ai.entity.*;
import com.neusoft.ai.repository.*;
import com.neusoft.ai.service.ai.AiExaminationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AiExaminationServiceImpl implements AiExaminationService {

    private final ExaminationAiInterpretationRepository interpretationRepository;
    private final ExaminationAiReviewRepository reviewRepository;
    private final CriticalValueWarningRepository criticalValueRepository;
    private final AiApiUtil aiApiUtil;

    @Override
    @Transactional
    public Map<String, Object> interpret(Long examinationId, Long patientId) {
        String prompt = "请对以下检查结果进行解读：检查ID=" + examinationId;
        String systemPrompt = "你是一名经验丰富的检验科医生，请对检查结果进行专业解读。"
                + "请按JSON格式返回：{\"interpretation\":\"解读内容\",\"keyFindings\":[],\"suggestions\":[\"建议1\"]}";

        String aiResponse = aiApiUtil.callAi(prompt, systemPrompt);

        String interpretation = "";
        String keyFindings = "";
        String suggestions = "";
        try {
            JSONObject json = JSONUtil.parseObj(aiResponse);
            interpretation = json.getStr("interpretation", aiResponse);
            if (json.getJSONArray("keyFindings") != null) {
                keyFindings = json.getJSONArray("keyFindings").toString();
            }
            if (json.getJSONArray("suggestions") != null) {
                suggestions = json.getJSONArray("suggestions").toString();
            }
        } catch (Exception e) {
            interpretation = aiResponse;
        }

        ExaminationAiInterpretation entity = new ExaminationAiInterpretation();
        entity.setExaminationId(examinationId);
        entity.setPatientId(patientId);
        entity.setInterpretation(interpretation);
        entity.setKeyFindings(keyFindings);
        entity.setSuggestions(suggestions);
        entity.setRawResponse(aiResponse);
        interpretationRepository.save(entity);

        Map<String, Object> result = new HashMap<>();
        result.put("id", entity.getId());
        result.put("interpretation", interpretation);
        try {
            if (!keyFindings.isEmpty()) result.put("keyFindings", JSONUtil.parseArray(keyFindings));
            if (!suggestions.isEmpty()) result.put("suggestions", JSONUtil.parseArray(suggestions));
        } catch (Exception ignored) {}
        return result;
    }

    @Override
    public Page<ExaminationAiInterpretation> getPatientInterpretation(Long patientId, Integer page, Integer size) {
        PageRequest pr = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        return interpretationRepository.findByPatientIdOrderByCreateTimeDesc(patientId, pr);
    }

    @Override
    public Map<String, Object> getProInterpretation(Long id) {
        ExaminationAiInterpretation interp = interpretationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("解读不存在"));
        Map<String, Object> result = new HashMap<>();
        result.put("id", interp.getId());
        result.put("interpretation", interp.getInterpretation());
        result.put("keyFindings", interp.getKeyFindings());
        result.put("suggestions", interp.getSuggestions());
        result.put("createTime", interp.getCreateTime());
        return result;
    }

    @Override
    public Page<Map<String, Object>> getCriticalList(String status, Integer page, Integer size) {
        PageRequest pr = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "detectedTime"));
        Page<CriticalValueWarning> warningPage;
        if (status != null && !status.isEmpty()) {
            warningPage = criticalValueRepository.findByStatusOrderByDetectedTimeDesc(status, pr);
        } else {
            warningPage = criticalValueRepository.findByOrderByDetectedTimeDesc(pr);
        }
        return warningPage.map(w -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", w.getId());
            map.put("examinationId", w.getExaminationId());
            map.put("patientId", w.getPatientId());
            map.put("itemName", w.getItemName());
            map.put("itemValue", w.getItemValue());
            map.put("alertLevel", w.getAlertLevel());
            map.put("status", w.getStatus());
            map.put("detectedTime", w.getDetectedTime());
            return map;
        });
    }

    @Override
    @Transactional
    public Map<String, Object> detectCriticalValue(Long examinationId) {
        String prompt = "请分析以下检查结果是否存在危急值：检查ID=" + examinationId;
        String systemPrompt = "你是一名检验科医生，请判断是否存在危急值。"
                + "请按JSON格式返回：{\"hasCriticalValue\":false,\"criticalItems\":[],\"analysis\":\"分析\"}";
        String aiResponse = aiApiUtil.callAi(prompt, systemPrompt);

        boolean hasCritical = false;
        String analysis = "";
        try {
            JSONObject json = JSONUtil.parseObj(aiResponse);
            hasCritical = json.getBool("hasCriticalValue", false);
            analysis = json.getStr("analysis", "");
        } catch (Exception e) {
            analysis = aiResponse;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("hasCriticalValue", hasCritical);
        result.put("analysis", analysis);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> confirmWarning(Long warningId) {
        CriticalValueWarning warning = criticalValueRepository.findById(warningId)
                .orElseThrow(() -> new BusinessException("预警记录不存在"));
        warning.setStatus("confirmed");
        warning.setConfirmTime(LocalDateTime.now());
        criticalValueRepository.save(warning);
        Map<String, Object> result = new HashMap<>();
        result.put("id", warning.getId());
        result.put("status", "confirmed");
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> processWarning(Long warningId, String note) {
        CriticalValueWarning warning = criticalValueRepository.findById(warningId)
                .orElseThrow(() -> new BusinessException("预警记录不存在"));
        warning.setStatus("processed");
        warning.setNote(note);
        warning.setProcessTime(LocalDateTime.now());
        criticalValueRepository.save(warning);
        Map<String, Object> result = new HashMap<>();
        result.put("id", warning.getId());
        result.put("status", "processed");
        return result;
    }

    @Override
    public Page<Map<String, Object>> getCriticalHistory(Long patientId, Integer page, Integer size) {
        PageRequest pr = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "detectedTime"));
        List<CriticalValueWarning> list = criticalValueRepository
                .findByPatientIdAndDetectedTimeAfter(patientId, LocalDateTime.now().minusYears(5));
        // Manually paginate
        int start = (int) pr.getOffset();
        int end = Math.min(start + pr.getPageSize(), list.size());
        List<Map<String, Object>> content = new ArrayList<>();
        for (int i = start; i < end; i++) {
            CriticalValueWarning w = list.get(i);
            Map<String, Object> map = new HashMap<>();
            map.put("id", w.getId());
            map.put("itemName", w.getItemName());
            map.put("itemValue", w.getItemValue());
            map.put("alertLevel", w.getAlertLevel());
            map.put("status", w.getStatus());
            map.put("detectedTime", w.getDetectedTime());
            content.add(map);
        }
        return new org.springframework.data.domain.PageImpl<>(content, pr, list.size());
    }

    @Override
    @Transactional
    public Map<String, Object> reviewExamination(Map<String, Object> request, Long doctorId) {
        Long examinationId = request.get("examinationId") != null ? ((Number) request.get("examinationId")).longValue() : null;
        String resultText = (String) request.get("result");

        String prompt = "请审核以下检验结果：\n" + (resultText != null ? resultText : "检查ID=" + examinationId);
        String systemPrompt = "你是一名经验丰富的检验科医生，请审核检验结果的质量和准确性。"
                + "请按JSON格式返回：{\"reviewResult\":\"pass/warning/reject\",\"reviewScore\":0-100,\"warnings\":[],\"suggestions\":\"\"}";
        String aiResponse = aiApiUtil.callAi(prompt, systemPrompt);

        ExaminationAiReview review = new ExaminationAiReview();
        review.setExaminationId(examinationId);
        review.setDoctorId(doctorId);
        try {
            JSONObject json = JSONUtil.parseObj(aiResponse);
            review.setReviewResult(json.getStr("reviewResult", "pass"));
            review.setReviewScore(json.getInt("reviewScore", 80));
            if (json.getJSONArray("warnings") != null) {
                review.setWarnings(json.getJSONArray("warnings").toString());
            }
            review.setSuggestions(json.getStr("suggestions", ""));
        } catch (Exception e) {
            review.setReviewResult("pass");
            review.setReviewScore(80);
        }
        review.setRawResponse(aiResponse);
        review.setStatus("ai_reviewed");
        reviewRepository.save(review);

        Map<String, Object> result = new HashMap<>();
        result.put("id", review.getId());
        result.put("reviewResult", review.getReviewResult());
        result.put("reviewScore", review.getReviewScore());
        result.put("suggestions", review.getSuggestions());
        return result;
    }

    @Override
    public Page<Map<String, Object>> getManualList(Long doctorId, Integer page, Integer size) {
        PageRequest pr = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        return reviewRepository.findByDoctorIdOrderByCreateTimeDesc(doctorId, pr)
                .map(r -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", r.getId());
                    map.put("examinationId", r.getExaminationId());
                    map.put("reviewResult", r.getReviewResult());
                    map.put("reviewScore", r.getReviewScore());
                    map.put("status", r.getStatus());
                    map.put("createTime", r.getCreateTime());
                    return map;
                });
    }

    @Override
    public Page<Map<String, Object>> getReviewList(Long doctorId, Integer page, Integer size) {
        return getManualList(doctorId, page, size);
    }

    @Override
    public Map<String, Object> getReviewDetail(Long id) {
        ExaminationAiReview review = reviewRepository.findById(id)
                .orElseThrow(() -> new BusinessException("审核记录不存在"));
        Map<String, Object> result = new HashMap<>();
        result.put("id", review.getId());
        result.put("examinationId", review.getExaminationId());
        result.put("reviewResult", review.getReviewResult());
        result.put("reviewScore", review.getReviewScore());
        result.put("warnings", review.getWarnings());
        result.put("suggestions", review.getSuggestions());
        result.put("status", review.getStatus());
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> manualConfirm(Long reviewId) {
        ExaminationAiReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException("审核记录不存在"));
        review.setStatus("confirmed");
        reviewRepository.save(review);
        Map<String, Object> result = new HashMap<>();
        result.put("id", review.getId());
        result.put("status", "confirmed");
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> reject(Long reviewId) {
        ExaminationAiReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException("审核记录不存在"));
        review.setStatus("rejected");
        reviewRepository.save(review);
        Map<String, Object> result = new HashMap<>();
        result.put("id", review.getId());
        result.put("status", "rejected");
        return result;
    }

    @Override
    public Map<String, Object> getReviewStats() {
        List<ExaminationAiReview> all = reviewRepository.findAll();
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", all.size());
        stats.put("passCount", all.stream().filter(r -> "pass".equals(r.getReviewResult())).count());
        stats.put("warningCount", all.stream().filter(r -> "warning".equals(r.getReviewResult())).count());
        stats.put("rejectCount", all.stream().filter(r -> "reject".equals(r.getReviewResult())).count());
        return stats;
    }
}
