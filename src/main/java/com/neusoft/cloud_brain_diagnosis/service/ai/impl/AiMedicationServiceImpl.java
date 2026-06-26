package com.neusoft.cloud_brain_diagnosis.service.ai.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.common.util.AiApiUtil;
import com.neusoft.cloud_brain_diagnosis.entity.MedicationGuide;
import com.neusoft.cloud_brain_diagnosis.entity.Prescription;
import com.neusoft.cloud_brain_diagnosis.repository.MedicationGuideRepository;
import com.neusoft.cloud_brain_diagnosis.repository.PrescriptionRepository;
import com.neusoft.cloud_brain_diagnosis.service.ai.AiMedicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiMedicationServiceImpl implements AiMedicationService {

    private final MedicationGuideRepository guideRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final AiApiUtil aiApiUtil;

    @Override
    @Transactional
    public Map<String, Object> generateGuide(Long prescriptionId) {
        // 1. 获取处方信息
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new BusinessException("处方不存在"));

        // 2. 组装提示词
        String prompt = "请为以下处方生成用药指导：\n处方ID：" + prescriptionId
                + "\n药品信息：" + prescription.getDrugs()
                + "\n患者：" + prescription.getPatientName();

        String systemPrompt = "你是一名专业药师，请根据处方信息为患者生成一份详细的个性化用药指导。"
                + "包含：用法用量、服用时间、饮食禁忌、不良反应注意事项、漏服处理、复诊提醒。"
                + "请按JSON格式返回：{\"guideContent\":\"详细的用药指导内容\"}";

        // 3. 调用AI
        String aiResponse = aiApiUtil.callAi(prompt, systemPrompt);

        // 4. 解析结果
        String guideContent = aiResponse;
        try {
            JSONObject json = JSONUtil.parseObj(aiResponse);
            guideContent = json.getStr("guideContent", aiResponse);
        } catch (Exception e) {
            // 使用原始返回
        }

        // 5. 保存用药指导
        // 检查是否已有指导记录
        MedicationGuide existing = guideRepository.findByPrescriptionId(prescriptionId).orElse(null);
        MedicationGuide guide;
        if (existing != null) {
            guide = existing;
        } else {
            guide = new MedicationGuide();
        }
        guide.setPrescriptionId(prescriptionId);
        guide.setPatientId(prescription.getPatientId());
        guide.setDrugsJson(prescription.getDrugs());
        guide.setGuideContent(guideContent);
        guide.setRawResponse(aiResponse);
        guideRepository.save(guide);

        Map<String, Object> result = new HashMap<>();
        result.put("id", guide.getId());
        result.put("prescriptionId", prescriptionId);
        result.put("guideContent", guideContent);
        result.put("createTime", guide.getCreateTime());
        return result;
    }

    @Override
    public MedicationGuide getGuide(Long prescriptionId) {
        return guideRepository.findByPrescriptionId(prescriptionId)
                .orElseThrow(() -> new BusinessException("未找到用药指导，请先生成"));
    }

    @Override
    @Transactional
    public String markPrinted(Long id) {
        MedicationGuide guide = guideRepository.findById(id)
                .orElseThrow(() -> new BusinessException("指导记录不存在"));
        guide.setPrintCount(guide.getPrintCount() + 1);
        guideRepository.save(guide);
        return "标记成功";
    }
}
