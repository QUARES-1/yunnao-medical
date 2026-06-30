package com.neusoft.ai.service.ai.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.neusoft.ai.common.exception.BusinessException;
import com.neusoft.ai.common.util.AiApiUtil;
import com.neusoft.ai.entity.*;
import com.neusoft.ai.feign.DoctorClient;
import com.neusoft.ai.repository.*;
import com.neusoft.ai.service.ai.AiMedicalRecordService;
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
    private final PromptTemplateServiceImpl promptTemplateService;
    private final DoctorClient doctorClient;

    @Override
    @Transactional
    public Map<String, Object> generateRecord(Long patientId, String inputText, String inputType, Long doctorId) {
        String departmentName = getDoctorDepartmentName(doctorId);

        Map<String, String> templates = promptTemplateService.getMedicalRecordTemplate(departmentName, inputText);
        String systemPrompt = templates.get("system");
        String prompt = templates.get("user");

        String aiResponse = aiApiUtil.callAi(prompt, systemPrompt);

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
            presentIllness = aiResponse;
        }

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

    private String getDoctorDepartmentName(Long doctorId) {
        if (doctorId == null) return null;
        try {
            var result = doctorClient.getDoctor(doctorId);
            if (result != null && result.getData() != null) {
                return (String) result.getData().get("departmentName");
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }
}
