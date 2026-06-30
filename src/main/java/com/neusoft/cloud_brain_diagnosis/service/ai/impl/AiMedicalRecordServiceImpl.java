package com.neusoft.cloud_brain_diagnosis.service.ai.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.common.util.AiApiUtil;
import com.neusoft.cloud_brain_diagnosis.entity.Doctor;
import com.neusoft.cloud_brain_diagnosis.entity.MedicalRecordAiGenerate;
import com.neusoft.cloud_brain_diagnosis.repository.DoctorRepository;
import com.neusoft.cloud_brain_diagnosis.repository.MedicalRecordAiGenerateRepository;
import com.neusoft.cloud_brain_diagnosis.service.ai.AiMedicalRecordService;
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
public class AiMedicalRecordServiceImpl implements AiMedicalRecordService {

    private final MedicalRecordAiGenerateRepository generateRepository;
    private final AiApiUtil aiApiUtil;
    private final PromptTemplateService promptTemplateService;
    private final DoctorRepository doctorRepository;

    @Override
    @Transactional
    public Map<String, Object> generateRecord(Long patientId, String inputText, String inputType, Long doctorId) {
        // 获取医生科室信息，用于选择科室专属提示词模板
        String departmentName = getDoctorDepartmentName(doctorId);

        // 1. 使用科室专属模板生成提示词
        Map<String, String> templates = promptTemplateService.getMedicalRecordTemplate(departmentName, inputText);
        String systemPrompt = templates.get("system");
        String prompt = templates.get("user");

        // 2. 调用AI
        String aiResponse = aiApiUtil.callAi(prompt, systemPrompt);

        // 3. 解析结果
        String chiefComplaint = "";
        String presentIllness = "";
        String pastHistory = "";
        String physicalExamination = "";
        String diagnosis = "";
        String treatment = "";

        try {
            JSONObject json = JSONUtil.parseObj(aiResponse);
            chiefComplaint = json.getStr("chiefComplaint", "");
            presentIllness = json.getStr("presentIllness", "");
            pastHistory = json.getStr("pastHistory", "");
            physicalExamination = json.getStr("physicalExamination", "");
            diagnosis = json.getStr("diagnosis", "");
            treatment = json.getStr("treatment", "");
        } catch (Exception e) {
            // 如果解析失败，将整个返回作为现病史
            presentIllness = aiResponse;
        }

        // 4. 保存生成记录
        MedicalRecordAiGenerate generate = new MedicalRecordAiGenerate();
        generate.setDoctorId(doctorId);
        generate.setPatientId(patientId);
        generate.setInputText(inputText);
        generate.setInputType(inputType);
        generate.setGeneratedChiefComplaint(chiefComplaint);
        generate.setGeneratedPresentIllness(presentIllness);
        generate.setGeneratedPastHistory(pastHistory);
        generate.setGeneratedPhysicalExamination(physicalExamination);
        generate.setGeneratedDiagnosis(diagnosis);
        generate.setGeneratedTreatment(treatment);
        generate.setRawResponse(aiResponse);
        generateRepository.save(generate);

        // 5. 构建返回
        Map<String, Object> result = new HashMap<>();
        result.put("id", generate.getId());
        result.put("chiefComplaint", chiefComplaint);
        result.put("presentIllness", presentIllness);
        result.put("pastHistory", pastHistory);
        result.put("physicalExamination", physicalExamination);
        result.put("diagnosis", diagnosis);
        result.put("treatment", treatment);

        return result;
    }

    @Override
    public Page<MedicalRecordAiGenerate> getGenerateList(Long doctorId, Integer page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "generateTime"));
        return generateRepository.findByDoctorIdOrderByGenerateTimeDesc(doctorId, pageRequest);
    }

    @Override
    public MedicalRecordAiGenerate getGenerateDetail(Long id) {
        return generateRepository.findById(id)
                .orElseThrow(() -> new BusinessException("生成记录不存在"));
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
}
