package com.neusoft.cloud_brain_diagnosis.service.ai.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.common.util.AiApiUtil;
import com.neusoft.cloud_brain_diagnosis.entity.FollowUpPlan;
import com.neusoft.cloud_brain_diagnosis.entity.FollowUpRecord;
import com.neusoft.cloud_brain_diagnosis.repository.FollowUpPlanRepository;
import com.neusoft.cloud_brain_diagnosis.repository.FollowUpRecordRepository;
import com.neusoft.cloud_brain_diagnosis.service.ai.AiFollowUpService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AiFollowUpServiceImpl implements AiFollowUpService {

    private final FollowUpPlanRepository planRepository;
    private final FollowUpRecordRepository recordRepository;
    private final AiApiUtil aiApiUtil;

    @Override
    @Transactional
    public FollowUpPlan createPlan(Map<String, Object> request, Long doctorId) {
        FollowUpPlan plan = new FollowUpPlan();
        plan.setPatientId(request.get("patientId") != null ? ((Number) request.get("patientId")).longValue() : null);
        plan.setDoctorId(doctorId);
        plan.setRegistrationId(request.get("registrationId") != null ? ((Number) request.get("registrationId")).longValue() : null);
        plan.setDisease((String) request.get("disease"));
        plan.setPlanType((String) request.get("planType"));
        plan.setTotalTimes(request.get("totalTimes") != null ? ((Number) request.get("totalTimes")).intValue() : 3);
        plan.setStatus("ongoing");
        return planRepository.save(plan);
    }

    @Override
    public Page<FollowUpPlan> getPatientPlans(Long patientId, Integer page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        return planRepository.findByPatientIdOrderByCreateTimeDesc(patientId, pageRequest);
    }

    @Override
    public Page<FollowUpRecord> getPendingRecords(Long patientId, Integer page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        return recordRepository.findByPatientIdAndStatus(patientId, "pending", pageRequest);
    }

    @Override
    @Transactional
    public String submitRecord(Long id, String answerJson, Long patientId) {
        FollowUpRecord record = recordRepository.findById(id)
                .orElseThrow(() -> new BusinessException("随访记录不存在"));
        record.setAnswerJson(answerJson);
        record.setStatus("completed");

        // AI分析随访结果
        String prompt = "请分析以下患者的随访问卷结果：\n" + answerJson
                + "\n原始问卷：" + record.getQuestionnaireJson();
        String systemPrompt = "你是一名医生，请分析患者的随访恢复情况，判断是否正常。"
                + "请按JSON格式返回：{\"analysis\":\"分析结果\",\"abnormal\":true/false}";

        String aiResponse = aiApiUtil.callAi(prompt, systemPrompt);
        try {
            JSONObject json = JSONUtil.parseObj(aiResponse);
            record.setAiAnalysis(json.getStr("analysis", aiResponse));
            boolean abnormal = json.getBool("abnormal", false);
            record.setAbnormalFlag(abnormal ? 1 : 0);
            if (abnormal) {
                record.setStatus("abnormal");
            }
        } catch (Exception e) {
            record.setAiAnalysis(aiResponse);
        }

        recordRepository.save(record);

        // 更新计划完成次数
        FollowUpPlan plan = planRepository.findById(record.getPlanId()).orElse(null);
        if (plan != null) {
            plan.setCompletedTimes(plan.getCompletedTimes() + 1);
            if (plan.getCompletedTimes() >= plan.getTotalTimes()) {
                plan.setStatus("completed");
            }
            planRepository.save(plan);
        }

        return "提交成功";
    }

    @Override
    public Map<String, Object> getDetail(Long id) {
        FollowUpRecord record = recordRepository.findById(id)
                .orElseThrow(() -> new BusinessException("随访记录不存在"));
        FollowUpPlan plan = planRepository.findById(record.getPlanId()).orElse(null);

        Map<String, Object> result = new HashMap<>();
        result.put("id", record.getId());
        result.put("planId", record.getPlanId());
        result.put("patientId", record.getPatientId());
        result.put("followUpTime", record.getFollowUpTime());
        result.put("status", record.getStatus());
        result.put("abnormalFlag", record.getAbnormalFlag());
        result.put("aiAnalysis", record.getAiAnalysis());
        result.put("doctorRemark", record.getDoctorRemark());
        result.put("createTime", record.getCreateTime());

        if (plan != null) {
            result.put("disease", plan.getDisease());
            result.put("planType", plan.getPlanType());
        }

        try {
            if (record.getQuestionnaireJson() != null) {
                result.put("questionnaire", JSONUtil.parseArray(record.getQuestionnaireJson()));
            }
            if (record.getAnswerJson() != null) {
                result.put("answers", JSONUtil.parseObj(record.getAnswerJson()));
            }
        } catch (Exception ignored) {}

        return result;
    }

    @Override
    public Page<FollowUpPlan> getDoctorList(Long doctorId, Integer page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        return planRepository.findByDoctorIdOrderByCreateTimeDesc(doctorId, pageRequest);
    }

    @Override
    @Transactional
    public String doctorReply(Long id, String remark, Long doctorId) {
        FollowUpRecord record = recordRepository.findById(id)
                .orElseThrow(() -> new BusinessException("随访记录不存在"));
        record.setDoctorRemark(remark);
        recordRepository.save(record);
        return "回复成功";
    }
}
