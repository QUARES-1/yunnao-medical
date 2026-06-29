package com.neusoft.cloud_brain_diagnosis.service.ai.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.common.util.AiApiUtil;
import com.neusoft.cloud_brain_diagnosis.entity.MedicalRecord;
import com.neusoft.cloud_brain_diagnosis.entity.QualityCheckRecord;
import com.neusoft.cloud_brain_diagnosis.entity.QualityCheckDetail;
import com.neusoft.cloud_brain_diagnosis.repository.MedicalRecordRepository;
import com.neusoft.cloud_brain_diagnosis.repository.QualityCheckDetailRepository;
import com.neusoft.cloud_brain_diagnosis.repository.QualityCheckRecordRepository;
import com.neusoft.cloud_brain_diagnosis.service.ai.AiQualityService;
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
    private final MedicalRecordRepository medicalRecordRepository;
    private final AiApiUtil aiApiUtil;

    @Override
    @Transactional
    public Map<String, Object> startQualityCheck(String checkType, Integer sampleSize) {
        if (sampleSize == null || sampleSize <= 0) sampleSize = 10;

        // 随机抽取病历或处方
        List<MedicalRecord> samples = new ArrayList<>();
        if ("medical_record".equals(checkType)) {
            List<MedicalRecord> all = medicalRecordRepository.findAll();
            if (all.size() > sampleSize) {
                Collections.shuffle(all);
                samples = all.subList(0, sampleSize);
            } else {
                samples = all;
            }
        }

        StringBuilder prompt = new StringBuilder("请对以下" + sampleSize + "份病历进行质量检查：\n");
        for (int i = 0; i < samples.size(); i++) {
            MedicalRecord mr = samples.get(i);
            prompt.append("\n--- 病历").append(i + 1).append(" ---\n");
            prompt.append("主诉：").append(mr.getChiefComplaint()).append("\n");
            prompt.append("现病史：").append(mr.getPresentIllness()).append("\n");
            prompt.append("既往史：").append(mr.getPastHistory()).append("\n");
            prompt.append("体格检查：").append(mr.getPhysicalExamination()).append("\n");
            prompt.append("诊断：").append(mr.getDiagnosis()).append("\n");
            prompt.append("治疗意见：").append(mr.getTreatment()).append("\n");
        }

        String systemPrompt = "你是一名医疗质控专家，请检查上述病历的质量。"
                + "评估标准：完整性、规范性、逻辑合理性。"
                + "请按JSON格式返回："
                + "{\"totalCount\":总数,\"passCount\":合格数,\"avgScore\":平均分,"
                + "\"problemSummary\":\"问题汇总\",\"improvementSuggestions\":\"改进建议\","
                + "\"details\":[{\"index\":1,\"score\":85,\"problems\":[{\"level\":\"minor\",\"field\":\"现病史\",\"content\":\"缺少发病诱因\"}],\"suggestions\":\"建议补充\"}]}";

        String aiResponse = aiApiUtil.callAi(prompt.toString(), systemPrompt);

        // 保存质检记录
        QualityCheckRecord record = new QualityCheckRecord();
        record.setCheckType(checkType);
        record.setCheckDate(java.time.LocalDateTime.now());
        record.setTotalCount(samples.size());
        record.setAvgScore(BigDecimal.valueOf(80));

        try {
            JSONObject json = JSONUtil.parseObj(aiResponse);
            record.setPassCount(json.getInt("passCount", samples.size()));
            record.setAvgScore(BigDecimal.valueOf(json.getDouble("avgScore", 80.0)));
            record.setProblemSummary(json.getStr("problemSummary", ""));
            record.setImprovementSuggestions(json.getStr("improvementSuggestions", ""));
            record.setCheckerType("ai");

            JSONArray details = json.getJSONArray("details");
            if (details != null) {
                for (int i = 0; i < details.size() && i < samples.size(); i++) {
                    JSONObject detail = details.getJSONObject(i);
                    QualityCheckDetail qd = new QualityCheckDetail();
                    qd.setTargetId(samples.get(i).getId());
                    qd.setTargetType(checkType);
                    qd.setDoctorId(samples.get(i).getDoctorId());
                    qd.setDoctorName(samples.get(i).getDoctorName());
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
        result.put("totalCount", samples.size());
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
