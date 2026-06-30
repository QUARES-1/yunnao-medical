package com.neusoft.ai.service.ai.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.neusoft.ai.common.exception.BusinessException;
import com.neusoft.ai.common.util.AiApiUtil;
import com.neusoft.ai.entity.QualityCheckDetail;
import com.neusoft.ai.entity.QualityCheckRecord;
import com.neusoft.ai.repository.QualityCheckDetailRepository;
import com.neusoft.ai.repository.QualityCheckRecordRepository;
import com.neusoft.ai.service.ai.AiQualityService;
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
public class AiQualityServiceImpl implements AiQualityService {

    private final QualityCheckRecordRepository checkRecordRepository;
    private final QualityCheckDetailRepository checkDetailRepository;
    private final AiApiUtil aiApiUtil;

    @Override
    @Transactional
    public Map<String, Object> startQualityCheck(String checkType, Integer sampleSize) {
        if (sampleSize == null || sampleSize <= 0) sampleSize = 10;

        String prompt = "请对" + sampleSize + "份病历进行质量检查（模拟数据）：\n";
        prompt += "样本1：主诉：咳嗽3天，诊断：上呼吸道感染\n";
        prompt += "样本2：主诉：头痛1周，诊断：偏头痛\n";

        String systemPrompt = "你是一名医疗质控专家，请检查上述病历的质量。"
                + "评估标准：完整性、规范性、逻辑合理性。"
                + "请按JSON格式返回："
                + "{\"totalCount\":总数,\"passCount\":合格数,\"avgScore\":平均分,"
                + "\"problemSummary\":\"问题汇总\",\"improvementSuggestions\":\"改进建议\","
                + "\"details\":[{\"index\":1,\"score\":85,\"problems\":[{\"level\":\"minor\",\"field\":\"现病史\",\"content\":\"缺少发病诱因\"}],\"suggestions\":\"建议补充\"}]}";

        String aiResponse = aiApiUtil.callAi(prompt.toString(), systemPrompt);

        QualityCheckRecord record = new QualityCheckRecord();
        record.setCheckType(checkType);
        record.setCheckDate(java.time.LocalDateTime.now());
        record.setTotalCount(sampleSize);
        record.setAvgScore(BigDecimal.valueOf(80));

        try {
            JSONObject json = JSONUtil.parseObj(aiResponse);
            record.setPassCount(json.getInt("passCount", sampleSize));
            record.setAvgScore(BigDecimal.valueOf(json.getDouble("avgScore", 80.0)));
            record.setProblemSummary(json.getStr("problemSummary", ""));
            record.setImprovementSuggestions(json.getStr("improvementSuggestions", ""));
            record.setCheckerType("ai");

            JSONArray details = json.getJSONArray("details");
            if (details != null) {
                for (int i = 0; i < details.size(); i++) {
                    JSONObject detail = details.getJSONObject(i);
                    QualityCheckDetail qd = new QualityCheckDetail();
                    qd.setTargetId((long) (i + 1));
                    qd.setTargetType(checkType);
                    qd.setScore(detail.getInt("score", 80));
                    if (detail.getJSONArray("problems") != null) {
                        qd.setProblems(detail.getJSONArray("problems").toString());
                    }
                    qd.setSuggestions(detail.getStr("suggestions", ""));
                    qd.setStatus("pending");
                    checkDetailRepository.save(qd);
                }
            }
        } catch (Exception ignored) {}

        record.setCheckerType("ai");
        checkRecordRepository.save(record);

        Map<String, Object> result = new HashMap<>();
        result.put("id", record.getId());
        result.put("checkType", checkType);
        result.put("totalCount", sampleSize);
        result.put("passCount", record.getPassCount());
        result.put("avgScore", record.getAvgScore());
        return result;
    }

    @Override
    public Page<QualityCheckRecord> getCheckList(Integer page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        return checkRecordRepository.findByOrderByCreateTimeDesc(pageRequest);
    }

    @Override
    public QualityCheckRecord getCheckDetail(Long id) {
        return checkRecordRepository.findById(id)
                .orElseThrow(() -> new BusinessException("质检记录不存在"));
    }

    @Override
    public Page<QualityCheckDetail> getCheckDetails(Long recordId, Integer page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        return checkDetailRepository.findByRecordIdOrderByCreateTimeDesc(recordId, pageRequest);
    }

    @Override
    public Map<String, Object> getDoctorStats() {
        List<QualityCheckDetail> all = checkDetailRepository.findAll();
        Map<Long, List<QualityCheckDetail>> grouped = new HashMap<>();
        for (QualityCheckDetail d : all) {
            grouped.computeIfAbsent(d.getDoctorId(), k -> new ArrayList<>()).add(d);
        }
        List<Map<String, Object>> stats = new ArrayList<>();
        for (Map.Entry<Long, List<QualityCheckDetail>> entry : grouped.entrySet()) {
            Map<String, Object> stat = new HashMap<>();
            stat.put("doctorId", entry.getKey());
            stat.put("doctorName", entry.getValue().get(0).getDoctorName());
            stat.put("totalCheck", entry.getValue().size());
            stat.put("avgScore", entry.getValue().stream().mapToInt(QualityCheckDetail::getScore).average().orElse(0));
            stat.put("pendingCount", entry.getValue().stream().filter(d -> "pending".equals(d.getStatus())).count());
            stats.add(stat);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("stats", stats);
        result.put("total", stats.size());
        return result;
    }

    @Override
    public Page<QualityCheckDetail> getMyQualityList(Long doctorId, Integer page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        return checkDetailRepository.findByDoctorIdOrderByCreateTimeDesc(doctorId, pageRequest);
    }

    @Override
    @Transactional
    public String rectify(Long id, String remark, Long doctorId) {
        QualityCheckDetail detail = checkDetailRepository.findById(id)
                .orElseThrow(() -> new BusinessException("质检明细不存在"));
        detail.setStatus("rectified");
        detail.setRectifyRemark(remark);
        checkDetailRepository.save(detail);
        return "整改已提交";
    }
}
