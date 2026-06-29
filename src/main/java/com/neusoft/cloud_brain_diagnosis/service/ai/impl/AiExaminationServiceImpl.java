package com.neusoft.cloud_brain_diagnosis.service.ai.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.common.util.AiApiUtil;
import com.neusoft.cloud_brain_diagnosis.entity.*;
import com.neusoft.cloud_brain_diagnosis.repository.*;
import com.neusoft.cloud_brain_diagnosis.service.ai.AiExaminationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AiExaminationServiceImpl implements AiExaminationService {

    private final ExaminationAiInterpretationRepository interpretationRepository;
    private final CriticalValueWarningRepository criticalValueRepository;
    private final ExaminationAiReviewRepository reviewRepository;
    private final ExaminationRepository examinationRepository;
    private final PatientRepository patientRepository;
    private final AiApiUtil aiApiUtil;

    // ========== 检验报告解读 ==========

    @Override
    @Transactional
    public Map<String, Object> interpret(Long examinationId) {
        Examination exam = examinationRepository.findById(examinationId)
                .orElseThrow(() -> new BusinessException("检查记录不存在"));

        String prompt = "请分析以下检验结果：\n" + exam.getResult();
        String systemPrompt = "你是一名经验丰富的检验科医生，请分析检验结果中的异常指标。"
                + "请按JSON格式返回："
                + "{\"abnormalItems\":[{\"name\":\"指标名\",\"value\":\"值\",\"unit\":\"单位\",\"reference\":\"参考范围\",\"status\":\"偏高/偏低/正常\"}],"
                + "\"interpretationPro\":\"专业版解读\",\"interpretationPatient\":\"通俗版解读\","
                + "\"suggestions\":\"建议\",\"reviewReminder\":\"复查提示\"}";

        String aiResponse = aiApiUtil.callAi(prompt, systemPrompt);

        Map<String, Object> result = new HashMap<>();
        try {
            JSONObject json = JSONUtil.parseObj(aiResponse);
            ExaminationAiInterpretation interpretation = new ExaminationAiInterpretation();
            interpretation.setExaminationId(examinationId);
            interpretation.setPatientId(exam.getPatientId());

            if (json.getJSONArray("abnormalItems") != null) {
                interpretation.setAbnormalItems(json.getJSONArray("abnormalItems").toString());
            }
            interpretation.setInterpretationPro(json.getStr("interpretationPro", ""));
            interpretation.setInterpretationPatient(json.getStr("interpretationPatient", ""));
            interpretation.setSuggestions(json.getStr("suggestions", ""));
            interpretation.setReviewReminder(json.getStr("reviewReminder", ""));
            interpretation.setRawResponse(aiResponse);
            interpretationRepository.save(interpretation);

            result.put("id", interpretation.getId());
            result.put("examinationId", examinationId);
            result.put("abnormalItems", json.getJSONArray("abnormalItems") != null
                    ? json.getJSONArray("abnormalItems").toList(Map.class) : new ArrayList<>());
            result.put("interpretation", json.getStr("interpretationPatient", ""));
            result.put("suggestions", json.getStr("suggestions", ""));
            result.put("reviewReminder", json.getStr("reviewReminder", ""));
        } catch (Exception e) {
            // 保存原始内容
            ExaminationAiInterpretation interpretation = new ExaminationAiInterpretation();
            interpretation.setExaminationId(examinationId);
            interpretation.setInterpretationPatient(aiResponse);
            interpretationRepository.save(interpretation);
            result.put("interpretation", aiResponse);
        }

        return result;
    }

    @Override
    public ExaminationAiInterpretation getPatientInterpretation(Long examinationId) {
        return interpretationRepository.findByExaminationId(examinationId)
                .orElseThrow(() -> new BusinessException("未生成解读报告"));
    }

    @Override
    public ExaminationAiInterpretation getProInterpretation(Long examinationId) {
        return interpretationRepository.findByExaminationId(examinationId)
                .orElseThrow(() -> new BusinessException("未生成解读报告"));
    }

    // ========== 危急值预警 ==========

    @Override
    public Page<CriticalValueWarning> getCriticalList(Long userId, String role, Integer page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        if ("doctor".equals(role)) {
            return criticalValueRepository.findByDoctorIdOrderByCreateTimeDesc(userId, pageRequest);
        }
        if ("patient".equals(role)) {
            return criticalValueRepository.findByPatientIdOrderByCreateTimeDesc(userId, pageRequest);
        }
        return criticalValueRepository.findByStatusOrderByCreateTimeDesc("pending", pageRequest);
    }

    @Override
    @Transactional
    public CriticalValueWarning detectCriticalValue(Long examinationId) {
        Examination exam = examinationRepository.findById(examinationId)
                .orElseThrow(() -> new BusinessException("检查记录不存在"));
        if (criticalValueRepository.existsByExaminationId(examinationId)) {
            return null;
        }
        String result = Optional.ofNullable(exam.getResult()).orElse("");
        List<String> hits = new ArrayList<>();
        detect(result, hits, "(?:血红蛋白|HGB)[^0-9]{0,12}([0-9.]+)", 60, false, "血红蛋白严重偏低，存在重度贫血风险");
        detect(result, hits, "(?:血糖|GLU|FPG)[^0-9]{0,12}([0-9.]+)", 16.7, true, "血糖严重偏高，存在高血糖急症风险");
        detect(result, hits, "(?:血糖|GLU|FPG)[^0-9]{0,12}([0-9.]+)", 2.8, false, "血糖严重偏低，存在低血糖风险");
        detect(result, hits, "(?:血钾|K\\+?)[^0-9]{0,12}([0-9.]+)", 2.8, false, "血钾严重偏低，存在心律失常风险");
        detect(result, hits, "(?:血钾|K\\+?)[^0-9]{0,12}([0-9.]+)", 6.2, true, "血钾严重偏高，存在心律失常风险");
        detect(result, hits, "(?:肌钙蛋白|cTnI|TnI)[^0-9]{0,12}([0-9.]+)", 0.5, true, "肌钙蛋白显著升高，警惕急性心肌损伤");
        if (hits.isEmpty()) return null;

        Patient patient = patientRepository.findById(exam.getPatientId()).orElse(null);
        CriticalValueWarning warning = new CriticalValueWarning();
        warning.setExaminationId(exam.getId());
        warning.setPatientId(exam.getPatientId());
        warning.setPatientName(exam.getPatientName());
        warning.setPatientPhone(patient == null ? null : patient.getPhone());
        warning.setDoctorId(exam.getDoctorId());
        warning.setDoctorName(exam.getDoctorName());
        warning.setCriticalItems(String.join("；", hits));
        warning.setWarningLevel("critical");
        warning.setStatus("pending");
        warning.setLabRemark("AI已标记加急，请优先复核并电话通知开单医生");
        warning.setSmsSent(0);
        return criticalValueRepository.save(warning);
    }

    private void detect(String text, List<String> hits, String regex, double threshold,
                        boolean greater, String message) {
        Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text);
        if (!matcher.find()) return;
        try {
            double value = Double.parseDouble(matcher.group(1));
            if ((greater && value >= threshold) || (!greater && value <= threshold)) {
                hits.add(message + "（检测值：" + value + "）");
            }
        } catch (NumberFormatException ignored) {
        }
    }

    @Override
    @Transactional
    public String confirmWarning(Long id, Long doctorId) {
        CriticalValueWarning warning = criticalValueRepository.findById(id)
                .orElseThrow(() -> new BusinessException("预警记录不存在"));
        warning.setStatus("confirmed");
        warning.setDoctorConfirmTime(java.time.LocalDateTime.now());
        warning.setDoctorId(doctorId);
        criticalValueRepository.save(warning);
        return "确认成功";
    }

    @Override
    @Transactional
    public String processWarning(Long id, String remark, Long doctorId) {
        CriticalValueWarning warning = criticalValueRepository.findById(id)
                .orElseThrow(() -> new BusinessException("预警记录不存在"));
        warning.setStatus("processed");
        warning.setDoctorRemark(remark);
        warning.setDoctorId(doctorId);
        criticalValueRepository.save(warning);
        return "处理成功";
    }

    @Override
    public Page<CriticalValueWarning> getCriticalHistory(Integer page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        return criticalValueRepository.findByOrderByCreateTimeDesc(pageRequest);
    }

    // ========== 检验AI审核 ==========

    @Override
    @Transactional
    public Map<String, Object> reviewExamination(Long examinationId, Long labStaffId) {
        Examination exam = examinationRepository.findById(examinationId)
                .orElseThrow(() -> new BusinessException("检查记录不存在"));

        String prompt = "请审核以下检验结果：\n" + exam.getResult();
        String systemPrompt = "你是一名检验科审核医生，请对检验结果进行AI审核。"
                + "审核要点：指标是否在参考范围内、逻辑合理性（如白细胞总数与分类计数是否匹配）、对比历史结果。"
                + "请按JSON格式返回："
                + "{\"reviewResult\":\"pass/manual/reject\",\"reviewScore\":0-100,"
                + "\"abnormalItems\":[{\"name\":\"指标\",\"value\":\"值\",\"reference\":\"参考\",\"status\":\"偏高/偏低\",\"level\":\"moderate/severe\"}],"
                + "\"logicIssues\":[{\"level\":\"info\",\"content\":\"描述\"}],"
                + "\"historyCompare\":[{\"item\":\"指标\",\"lastValue\":\"上次\",\"currentValue\":\"本次\",\"change\":\"变化\",\"level\":\"normal/significant\"}],"
                + "\"warnings\":[{\"level\":\"low/medium/high\",\"content\":\"警告\"}],"
                + "\"suggestions\":\"建议\"}";

        String aiResponse = aiApiUtil.callAi(prompt, systemPrompt);

        Map<String, Object> result = new HashMap<>();
        try {
            JSONObject json = JSONUtil.parseObj(aiResponse);
            ExaminationAiReview review = reviewRepository.findByExaminationId(examinationId)
                    .orElseGet(ExaminationAiReview::new);
            review.setExaminationId(examinationId);
            review.setPatientId(exam.getPatientId());
            review.setLabStaffId(labStaffId);
            review.setReviewResult(json.getStr("reviewResult", "manual"));
            review.setReviewScore(json.getInt("reviewScore", 70));

            if (json.getJSONArray("abnormalItems") != null)
                review.setAbnormalItems(json.getJSONArray("abnormalItems").toString());
            if (json.getJSONArray("logicIssues") != null)
                review.setLogicIssues(json.getJSONArray("logicIssues").toString());
            if (json.getJSONArray("historyCompare") != null)
                review.setHistoryCompare(json.getJSONArray("historyCompare").toString());
            if (json.getJSONArray("warnings") != null)
                review.setWarnings(json.getJSONArray("warnings").toString());

            review.setSuggestions(json.getStr("suggestions", ""));
            review.setRawResponse(aiResponse);
            reviewRepository.save(review);

            result.put("id", review.getId());
            result.put("examinationId", examinationId);
            result.put("reviewResult", review.getReviewResult());
            result.put("reviewScore", review.getReviewScore());
            result.put("abnormalItems", json.getJSONArray("abnormalItems") != null
                    ? json.getJSONArray("abnormalItems").toList(Map.class) : new ArrayList<>());
            result.put("suggestions", json.getStr("suggestions", ""));
        } catch (Exception e) {
            result.put("error", "AI审核失败");
        }
        return result;
    }

    @Override
    public Page<ExaminationAiReview> getManualList(Integer page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "reviewTime"));
        return reviewRepository.findByReviewResultOrderByReviewTimeDesc("manual", pageRequest);
    }

    @Override
    public Page<ExaminationAiReview> getReviewList(String reviewResult, Integer page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "reviewTime"));
        if (reviewResult == null || reviewResult.trim().isEmpty() || "全部".equals(reviewResult.trim())) {
            return reviewRepository.findByOrderByReviewTimeDesc(pageRequest);
        }
        return reviewRepository.findByReviewResultOrderByReviewTimeDesc(reviewResult.trim(), pageRequest);
    }

    @Override
    public ExaminationAiReview getReviewDetail(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new BusinessException("审核记录不存在"));
    }

    @Override
    @Transactional
    public String manualConfirm(Long id) {
        ExaminationAiReview review = reviewRepository.findById(id)
                .orElseThrow(() -> new BusinessException("审核记录不存在"));
        review.setReviewResult("pass");
        reviewRepository.save(review);
        return "确认成功";
    }

    @Override
    @Transactional
    public String reject(Long id, String reason) {
        ExaminationAiReview review = reviewRepository.findById(id)
                .orElseThrow(() -> new BusinessException("审核记录不存在"));
        review.setReviewResult("reject");
        review.setSuggestions(reason);
        reviewRepository.save(review);
        return "已退回重测";
    }

    @Override
    public Map<String, Object> getReviewStats() {
        List<ExaminationAiReview> all = reviewRepository.findAll();
        long total = all.size();
        long passCount = all.stream().filter(r -> "pass".equals(r.getReviewResult())).count();
        long manualCount = all.stream().filter(r -> "manual".equals(r.getReviewResult())).count();
        long rejectCount = all.stream().filter(r -> "reject".equals(r.getReviewResult())).count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("passCount", passCount);
        stats.put("manualCount", manualCount);
        stats.put("rejectCount", rejectCount);
        stats.put("passRate", total > 0 ? Math.round(passCount * 100.0 / total) : 0);
        return stats;
    }
}
