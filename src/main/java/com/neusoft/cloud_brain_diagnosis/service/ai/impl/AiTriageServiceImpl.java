package com.neusoft.cloud_brain_diagnosis.service.ai.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.common.util.AiApiUtil;
import com.neusoft.cloud_brain_diagnosis.entity.Doctor;
import com.neusoft.cloud_brain_diagnosis.entity.TriageRecord;
import com.neusoft.cloud_brain_diagnosis.repository.DoctorRepository;
import com.neusoft.cloud_brain_diagnosis.repository.TriageRecordRepository;
import com.neusoft.cloud_brain_diagnosis.service.ai.AiTriageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiTriageServiceImpl implements AiTriageService {

    private final TriageRecordRepository triageRecordRepository;
    private final DoctorRepository doctorRepository;
    private final AiApiUtil aiApiUtil;

    @Override
    @Transactional
    public Map<String, Object> consult(String chiefComplaint, Long patientId) {
        // 1. 组装提示词
        String prompt = "患者症状：" + chiefComplaint;
        String systemPrompt = "你是一名经验丰富的全科分诊医生。请根据患者症状推荐最适合的科室和医生。"
                + "请按以下JSON格式返回：{\"recommendDepartment\":\"科室名称\",\"recommendDepartmentId\":科室ID,\"recommendDoctorIds\":\"医生ID,用逗号分隔\",\"analysis\":\"分析结论\",\"confidence\":置信度0-100}";

        // 2. 调用AI
        String aiResponse = aiApiUtil.callAi(prompt, systemPrompt);

        // 3. 解析结果
        Map<String, Object> result = new HashMap<>();
        String recommendDepartment = "内科";
        Long recommendDepartmentId = 1L;
        String recommendDoctorIds = "";
        String analysis = "";
        int confidence = 80;

        try {
            JSONObject json = JSONUtil.parseObj(aiResponse);
            recommendDepartment = json.getStr("recommendDepartment", "内科");
            recommendDepartmentId = json.getLong("recommendDepartmentId", 1L);
            recommendDoctorIds = json.getStr("recommendDoctorIds", "");
            analysis = json.getStr("analysis", "");
            confidence = json.getInt("confidence", 80);
        } catch (Exception e) {
            // 解析失败使用默认值
            analysis = aiResponse;
        }

        DepartmentMatch normalizedDepartment = normalizeDepartment(chiefComplaint, recommendDepartment, recommendDepartmentId);
        recommendDepartment = normalizedDepartment.name();
        recommendDepartmentId = normalizedDepartment.id();

        // 4. 匹配推荐医生
        List<Map<String, Object>> recommendDoctors = new ArrayList<>();
        if (recommendDoctorIds != null && !recommendDoctorIds.isEmpty()) {
            List<Long> doctorIds = Arrays.stream(recommendDoctorIds.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
            for (Long doctorId : doctorIds) {
                doctorRepository.findById(doctorId).ifPresent(doc -> {
                    Map<String, Object> docMap = new HashMap<>();
                    docMap.put("id", doc.getId());
                    docMap.put("name", doc.getName());
                    docMap.put("title", doc.getTitle());
                    docMap.put("specialty", doc.getSpecialty());
                    recommendDoctors.add(docMap);
                });
            }
        }
        // 如果没有匹配到医生，按科室推荐
        if (recommendDoctors.isEmpty()) {
            List<Doctor> doctors = doctorRepository.findByDepartmentId(recommendDepartmentId);
            for (Doctor doc : doctors) {
                Map<String, Object> docMap = new HashMap<>();
                docMap.put("id", doc.getId());
                docMap.put("name", doc.getName());
                docMap.put("title", doc.getTitle());
                docMap.put("specialty", doc.getSpecialty());
                recommendDoctors.add(docMap);
            }
        }

        // 5. 保存分诊记录
        TriageRecord record = new TriageRecord();
        record.setPatientId(patientId);
        record.setChiefComplaint(chiefComplaint);
        record.setRecommendDepartment(recommendDepartment);
        record.setRecommendDepartmentId(recommendDepartmentId);
        record.setRecommendDoctorIds(recommendDoctorIds);
        record.setAiAnalysis(analysis);
        record.setConfidence(confidence);
        record.setRawResponse(aiResponse);
        record.setStatus("success");
        triageRecordRepository.save(record);

        // 6. 构建返回
        result.put("id", record.getId());
        result.put("recommendDepartment", recommendDepartment);
        result.put("recommendDepartmentId", recommendDepartmentId);
        result.put("recommendDoctors", recommendDoctors);
        result.put("analysis", analysis);
        result.put("confidence", confidence);
        result.put("createTime", record.getCreateTime());

        return result;
    }

    private DepartmentMatch normalizeDepartment(String chiefComplaint, String aiDepartment, Long aiDepartmentId) {
        String text = (chiefComplaint == null ? "" : chiefComplaint) + " " + (aiDepartment == null ? "" : aiDepartment);
        if (containsAny(text, "牙", "口腔", "牙龈", "牙痛", "口腔溃疡", "咀嚼", "舌", "嘴")) {
            return new DepartmentMatch("口腔科", 6L);
        }
        if (containsAny(text, "眼", "视力", "流泪", "红眼", "眼痛", "眼干", "白内障")) {
            return new DepartmentMatch("眼科", 4L);
        }
        if (containsAny(text, "耳", "鼻", "喉", "咽", "嗓子", "鼻塞", "流鼻涕", "耳鸣", "听力")) {
            return new DepartmentMatch("耳鼻喉科", 5L);
        }
        if (containsAny(text, "咳", "喘", "胸闷", "气短", "呼吸", "肺", "痰")) {
            return new DepartmentMatch("呼吸内科", 10L);
        }
        if (containsAny(text, "胃", "腹", "肚子", "反酸", "恶心", "呕吐", "腹泻", "便秘", "消化")) {
            return new DepartmentMatch("消化内科", 11L);
        }
        if (containsAny(text, "骨", "关节", "腰", "腿", "肩", "颈", "扭伤", "骨折", "疼痛", "运动")) {
            return new DepartmentMatch("骨科", 12L);
        }
        if (containsAny(text, "皮肤", "皮疹", "瘙痒", "红斑", "痘", "过敏", "脱皮", "湿疹")) {
            return new DepartmentMatch("皮肤科", 7L);
        }
        if (containsAny(text, "头痛", "头晕", "失眠", "麻木", "抽搐", "记忆", "神经")) {
            return new DepartmentMatch("神经内科", 8L);
        }
        if (containsAny(text, "心", "心慌", "胸痛", "血压", "高血压", "心悸", "心率")) {
            return new DepartmentMatch("心血管内科", 9L);
        }
        if (containsAny(text, "儿童", "孩子", "小孩", "宝宝", "婴儿", "幼儿")) {
            return new DepartmentMatch("儿科", 2L);
        }
        if (containsAny(text, "发烧", "发热", "高热", "急", "昏迷", "大出血")) {
            return new DepartmentMatch("急诊科", 14L);
        }
        if (aiDepartmentId != null && aiDepartmentId > 0) {
            return new DepartmentMatch(aiDepartment == null || aiDepartment.isBlank() ? "内科" : aiDepartment, aiDepartmentId);
        }
        return new DepartmentMatch("内科", 20L);
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null) {
            return false;
        }
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private record DepartmentMatch(String name, Long id) {}
    @Override
    public Page<TriageRecord> getPatientList(Long patientId, Integer page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        return triageRecordRepository.findByPatientIdOrderByCreateTimeDesc(patientId, pageRequest);
    }

    @Override
    public TriageRecord getDetail(Long id) {
        return triageRecordRepository.findById(id)
                .orElseThrow(() -> new BusinessException("分诊记录不存在"));
    }
}
