package com.neusoft.ai.service.ai.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.neusoft.ai.common.util.AiApiUtil;
import com.neusoft.ai.entity.MedicationGuide;
import com.neusoft.ai.repository.MedicationGuideRepository;
import com.neusoft.ai.service.ai.AiMedicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AiMedicationServiceImpl implements AiMedicationService {

    private final MedicationGuideRepository guideRepository;
    private final AiApiUtil aiApiUtil;

    @Override
    @Transactional
    public Map<String, Object> getGuide(Long prescriptionId, Long patientId) {
        MedicationGuide existing = guideRepository.findByPrescriptionId(prescriptionId);
        if (existing != null) {
            Map<String, Object> result = new HashMap<>();
            result.put("id", existing.getId());
            result.put("medications", existing.getMedications());
            result.put("generalAdvice", existing.getGeneralAdvice());
            return result;
        }

        String prompt = "请为处方" + prescriptionId + "（患者ID:" + patientId + "）生成用药指导。";
        String systemPrompt = "你是一名临床药师，请生成详细的用药指导。"
                + "请按JSON格式返回：{\"guide\":{\"medications\":[{\"name\":\"药品名\",\"dosage\":\"剂量\",\"frequency\":\"频率\",\"duration\":\"疗程\",\"notes\":\"注意事项\"}],\"generalAdvice\":[\"通用建议1\"]}}";
        String aiResponse = aiApiUtil.callAi(prompt, systemPrompt);

        MedicationGuide guide = new MedicationGuide();
        guide.setPrescriptionId(prescriptionId);
        guide.setPatientId(patientId);
        guide.setRawResponse(aiResponse);

        try {
            JSONObject json = JSONUtil.parseObj(aiResponse);
            JSONObject guideJson = json.getJSONObject("guide");
            if (guideJson != null) {
                guide.setMedications(guideJson.getJSONArray("medications") != null ? guideJson.getJSONArray("medications").toString() : "[]");
                guide.setGeneralAdvice(guideJson.getJSONArray("generalAdvice") != null ? guideJson.getJSONArray("generalAdvice").toString() : "[]");
            }
        } catch (Exception e) {
            guide.setMedications("[]");
            guide.setGeneralAdvice("[]");
        }
        guideRepository.save(guide);

        Map<String, Object> result = new HashMap<>();
        result.put("id", guide.getId());
        try {
            if (guide.getMedications() != null) result.put("medications", JSONUtil.parseArray(guide.getMedications()));
            if (guide.getGeneralAdvice() != null) result.put("generalAdvice", JSONUtil.parseArray(guide.getGeneralAdvice()));
        } catch (Exception ignored) {}
        return result;
    }
}
