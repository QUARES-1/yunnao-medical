package com.neusoft.cloud_brain_diagnosis.service.ai.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.neusoft.cloud_brain_diagnosis.common.context.UserContext;
import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.exception.BusinessException;
import com.neusoft.cloud_brain_diagnosis.common.util.AiApiUtil;
import com.neusoft.cloud_brain_diagnosis.entity.MedicalRecord;
import com.neusoft.cloud_brain_diagnosis.entity.MedicationGuide;
import com.neusoft.cloud_brain_diagnosis.entity.Patient;
import com.neusoft.cloud_brain_diagnosis.entity.Prescription;
import com.neusoft.cloud_brain_diagnosis.repository.MedicalRecordRepository;
import com.neusoft.cloud_brain_diagnosis.repository.MedicationGuideRepository;
import com.neusoft.cloud_brain_diagnosis.repository.PatientRepository;
import com.neusoft.cloud_brain_diagnosis.repository.PrescriptionRepository;
import com.neusoft.cloud_brain_diagnosis.service.ai.AiMedicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AiMedicationServiceImpl implements AiMedicationService {

    private final MedicationGuideRepository guideRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PatientRepository patientRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final AiApiUtil aiApiUtil;

    @Override
    @Transactional
    public Map<String, Object> generateGuide(Long prescriptionId) {
        if (prescriptionId == null) {
            throw new BusinessException("处方编号不能为空");
        }

        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new BusinessException("处方不存在"));
        assertPatientOwnsPrescription(prescription);
        Patient patient = prescription.getPatientId() == null
                ? null : patientRepository.findById(prescription.getPatientId()).orElse(null);
        String diagnosis = resolveDiagnosis(prescription);
        List<Map<String, Object>> medications = parseMedications(prescription.getDrugs());
        if (medications.isEmpty()) {
            throw new BusinessException("该处方没有药品明细，暂时无法生成用药指导");
        }

        String prompt = "请为以下患者和处方生成个性化用药指导："
                + "\n患者：" + safe(prescription.getPatientName(), "未填写")
                + "\n年龄：" + (patient == null || patient.getAge() == null ? "未知" : patient.getAge())
                + "\n性别：" + (patient == null ? "未知" : safe(patient.getGender(), "未知"))
                + "\n过敏史：" + (patient == null ? "未记录" : safe(patient.getAllergyHistory(), "无已知药物过敏史"))
                + "\n诊断：" + diagnosis
                + "\n药品信息：" + prescription.getDrugs();

        String systemPrompt = """
                你是一名专业临床药师。请根据患者年龄、性别、诊断、过敏史和处方，
                实时生成准确、易懂的个性化用药说明。不得修改医生处方中的药名、规格和用法用量。
                每一种药都必须单独给出：服用时间、饮食禁忌、常见不良反应、重要注意事项、漏服处理。
                同时给出3条总体建议和1条复诊提醒。
                只返回合法JSON，不要使用Markdown代码块。格式必须为：
                {
                  "summary":"本次指导摘要",
                  "medications":[
                    {
                      "name":"必须与处方药名一致",
                      "takingTime":"服用时间",
                      "dietRestrictions":"饮食禁忌",
                      "adverseReactions":"常见不良反应",
                      "precautions":"重要注意事项",
                      "missedDose":"漏服处理"
                    }
                  ],
                  "generalAdvice":["建议1","建议2","建议3"],
                  "followUpAdvice":"复诊提醒",
                  "guideContent":"完整用药指导文本"
                }
                """;

        String aiResponse;
        AiPayload aiPayload;
        try {
            aiResponse = aiApiUtil.callAi(prompt, systemPrompt);
            aiPayload = parseAiPayload(aiResponse, medications, diagnosis);
        } catch (Exception exception) {
            aiPayload = fallbackPayload(medications, diagnosis);
            aiResponse = JSONUtil.createObj()
                    .set("source", "safe-rule-fallback")
                    .set("guideContent", aiPayload.guideContent())
                    .toString();
        }

        // 旧测试数据中同一处方可能存在多条指导。重新生成时只保留最新的一条。
        guideRepository.deleteByPrescriptionId(prescriptionId);

        MedicationGuide guide = new MedicationGuide();
        guide.setPrescriptionId(prescriptionId);
        guide.setPatientId(prescription.getPatientId());
        guide.setPatientAge(patient == null ? null : patient.getAge());
        guide.setPatientGender(patient == null ? null : patient.getGender());
        guide.setDrugsJson(prescription.getDrugs());
        guide.setGuideContent(aiPayload.guideContent());
        guide.setRawResponse(aiResponse);
        guide = guideRepository.save(guide);

        return buildResult(guide, prescription, patient, diagnosis, aiPayload);
    }

    @Override
    @Transactional
    public Map<String, Object> getGuide(Long prescriptionId) {
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new BusinessException("处方不存在"));
        assertPatientOwnsPrescription(prescription);
        List<MedicationGuide> guides =
                guideRepository.findAllByPrescriptionIdOrderByIdDesc(prescriptionId);

        if (guides.isEmpty()
                || guides.size() > 1
                || !Objects.equals(guides.get(0).getDrugsJson(), prescription.getDrugs())
                || !Objects.equals(guides.get(0).getPatientId(), prescription.getPatientId())) {
            return generateGuide(prescriptionId);
        }

        MedicationGuide guide = guides.get(0);
        Patient patient = prescription.getPatientId() == null
                ? null : patientRepository.findById(prescription.getPatientId()).orElse(null);
        String diagnosis = resolveDiagnosis(prescription);
        List<Map<String, Object>> medications = parseMedications(prescription.getDrugs());
        AiPayload aiPayload;
        try {
            aiPayload = parseAiPayload(guide.getRawResponse(), medications, diagnosis);
        } catch (Exception exception) {
            aiPayload = fallbackPayload(medications, diagnosis);
        }
        return buildResult(guide, prescription, patient, diagnosis, aiPayload);
    }

    @Override
    @Transactional
    public String markPrinted(Long id) {
        MedicationGuide guide = guideRepository.findById(id)
                .orElseThrow(() -> new BusinessException("指导记录不存在"));
        if (RoleEnum.PATIENT.getCode().equals(UserContext.getRole())
                && !Objects.equals(guide.getPatientId(), UserContext.getUserId())) {
            throw new BusinessException("无权打印其他患者的用药指导");
        }
        guide.setPrintCount((guide.getPrintCount() == null ? 0 : guide.getPrintCount()) + 1);
        guideRepository.save(guide);
        return "打印记录已保存";
    }

    private Map<String, Object> buildResult(
            MedicationGuide guide,
            Prescription prescription,
            Patient patient,
            String diagnosis,
            AiPayload aiPayload) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", guide.getId());
        result.put("prescriptionId", prescription.getId());
        result.put("patientId", prescription.getPatientId());
        result.put("patientName", prescription.getPatientName());
        result.put("patientAge", patient == null ? guide.getPatientAge() : patient.getAge());
        result.put("patientGender", patient == null ? guide.getPatientGender() : patient.getGender());
        result.put("allergyHistory",
                patient == null ? "未记录" : safe(patient.getAllergyHistory(), "无已知药物过敏史"));
        result.put("diagnosis", diagnosis);
        result.put("doctorName", prescription.getDoctorName());
        result.put("prescriptionStatus", prescription.getStatus());
        result.put("medications", aiPayload.medications());
        result.put("generalAdvice", aiPayload.generalAdvice());
        result.put("followUpAdvice", aiPayload.followUpAdvice());
        result.put("guideContent", aiPayload.guideContent());
        result.put("aiGenerated", aiPayload.aiGenerated());
        result.put("source", aiPayload.aiGenerated() ? "DeepSeek AI 实时生成" : "安全规则兜底");
        result.put("printCount", guide.getPrintCount() == null ? 0 : guide.getPrintCount());
        result.put("createTime", guide.getCreateTime());
        return result;
    }

    private AiPayload parseAiPayload(
            String aiResponse,
            List<Map<String, Object>> medications,
            String diagnosis) {
        if (aiResponse == null || aiResponse.isBlank()) {
            throw new IllegalArgumentException("AI返回为空");
        }

        String cleanJson = aiResponse.trim()
                .replaceFirst("^```(?:json)?\\s*", "")
                .replaceFirst("\\s*```$", "")
                .trim();
        JSONObject root = JSONUtil.parseObj(cleanJson);
        JSONArray aiMedications = root.getJSONArray("medications");
        if (aiMedications == null || aiMedications.isEmpty()) {
            throw new IllegalArgumentException("AI未返回结构化药品指导");
        }

        int matched = 0;
        for (Map<String, Object> medication : medications) {
            String prescriptionName = String.valueOf(medication.get("name"));
            JSONObject matchedAdvice = null;
            for (Object value : aiMedications) {
                JSONObject advice = JSONUtil.parseObj(value);
                String aiName = safe(advice.getStr("name"), "");
                if (!aiName.isBlank() && (prescriptionName.equals(aiName)
                        || prescriptionName.contains(aiName)
                        || aiName.contains(prescriptionName))) {
                    matchedAdvice = advice;
                    break;
                }
            }
            if (matchedAdvice == null) continue;

            putIfPresent(medication, "takingTime", matchedAdvice.getStr("takingTime"));
            putIfPresent(medication, "dietRestrictions", matchedAdvice.getStr("dietRestrictions"));
            putIfPresent(medication, "adverseReactions", matchedAdvice.getStr("adverseReactions"));
            putIfPresent(medication, "precautions", matchedAdvice.getStr("precautions"));
            putIfPresent(medication, "missedDose", matchedAdvice.getStr("missedDose"));
            matched++;
        }
        if (matched == 0) {
            throw new IllegalArgumentException("AI药名与处方不匹配");
        }

        List<String> generalAdvice = new ArrayList<>();
        JSONArray adviceArray = root.getJSONArray("generalAdvice");
        if (adviceArray != null) {
            for (Object item : adviceArray) {
                if (item != null && !String.valueOf(item).isBlank()) {
                    generalAdvice.add(String.valueOf(item));
                }
            }
        }
        if (generalAdvice.isEmpty()) {
            generalAdvice = defaultGeneralAdvice();
        }

        String followUpAdvice = safe(
                root.getStr("followUpAdvice"),
                "症状持续加重或出现明显不适时，请及时复诊。");
        String guideContent = safe(
                root.getStr("guideContent"),
                buildReadableGuide(medications, diagnosis));
        return new AiPayload(
                medications, generalAdvice, followUpAdvice, guideContent, true);
    }

    private void assertPatientOwnsPrescription(Prescription prescription) {
        if (RoleEnum.PATIENT.getCode().equals(UserContext.getRole())
                && !Objects.equals(prescription.getPatientId(), UserContext.getUserId())) {
            throw new BusinessException("无权查看其他患者的处方");
        }
    }

    private AiPayload fallbackPayload(
            List<Map<String, Object>> medications,
            String diagnosis) {
        return new AiPayload(
                medications,
                defaultGeneralAdvice(),
                "症状持续加重、连续发热超过3天或出现明显不适时，请及时复诊。",
                buildReadableGuide(medications, diagnosis),
                false);
    }

    private List<String> defaultGeneralAdvice() {
        return List.of(
                "严格按照医生处方剂量服用，不自行加量、减量或停药。",
                "服药期间饮食清淡、保证饮水和休息，避免饮酒。",
                "如出现呼吸困难、面唇肿胀、全身皮疹等严重反应，立即停药并就医。");
    }

    private void putIfPresent(Map<String, Object> target, String key, String value) {
        if (value != null && !value.isBlank()) {
            target.put(key, value);
        }
    }

    private List<Map<String, Object>> parseMedications(String drugsJson) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (drugsJson == null || drugsJson.isBlank()) return result;

        try {
            JSONArray drugs = JSONUtil.parseArray(drugsJson);
            for (Object value : drugs) {
                JSONObject drug = JSONUtil.parseObj(value);
                String name = safe(drug.getStr("name"), "未命名药品");
                Advice advice = adviceFor(name);

                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name", name);
                item.put("specification", safe(drug.getStr("specification"), "规格未填写"));
                item.put("quantity", drug.getInt("quantity", 1));
                item.put("unit", safe(drug.getStr("unit"), "盒"));
                item.put("usage", safe(drug.getStr("usage"), "请按医嘱使用"));
                item.put("takingTime", advice.takingTime());
                item.put("dietRestrictions", advice.dietRestrictions());
                item.put("adverseReactions", advice.adverseReactions());
                item.put("precautions", advice.precautions());
                item.put("missedDose", "想起后尽快补服；若已接近下一次服药时间则跳过，不要一次服用双倍剂量。");
                result.add(item);
            }
        } catch (Exception exception) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", "处方药品");
            item.put("specification", "详见处方");
            item.put("quantity", 1);
            item.put("unit", "份");
            item.put("usage", drugsJson);
            item.put("takingTime", "请按医生处方规定时间服用");
            item.put("dietRestrictions", "服药期间避免饮酒，饮食清淡");
            item.put("adverseReactions", "如出现明显不适，请及时咨询医生或药师");
            item.put("precautions", "不自行调整剂量，不与他人共用药物");
            item.put("missedDose", "不要补服双倍剂量");
            result.add(item);
        }
        return result;
    }

    private Advice adviceFor(String name) {
        if (name.contains("阿奇霉素")) {
            return new Advice(
                    "每日固定时间服用；胃部不适者可在饭后服用，并与含铝、镁的制酸药间隔至少2小时。",
                    "避免饮酒；少吃辛辣、油腻食物，不要与制酸药同时服用。",
                    "可能出现恶心、腹痛、腹泻、头晕或皮疹。",
                    "对大环内酯类药物过敏、严重肝功能异常或有心律失常史者应提前告知医生。");
        }
        if (name.contains("氨溴索")) {
            return new Advice(
                    "建议饭后服用，并适量饮温水，有助于痰液稀释和排出。",
                    "避免饮酒及过甜、过冷、辛辣刺激食物。",
                    "少数人可能出现胃部不适、恶心、口干或皮疹。",
                    "服药后如出现严重皮疹、黏膜损伤或呼吸困难，应立即停药就医。");
        }
        if (name.contains("对乙酰氨基酚")) {
            return new Advice(
                    "发热或疼痛时按医嘱服用，两次用药应保持处方规定间隔，建议饭后服用。",
                    "严禁饮酒；避免同时服用其他含对乙酰氨基酚的感冒药。",
                    "偶见恶心、皮疹；过量可能造成严重肝损伤。",
                    "不要超过医生规定的每日总量，肝病患者及长期饮酒者应特别谨慎。");
        }
        if (name.contains("头孢") || name.contains("阿莫西林")) {
            return new Advice(
                    "按固定间隔服用，胃部不适者可饭后服用；抗菌药应按医嘱完成疗程。",
                    "用药期间及停药后至少72小时避免饮酒。",
                    "可能出现恶心、腹泻、皮疹或过敏反应。",
                    "有青霉素或头孢菌素过敏史者必须提前告知医生。");
        }
        if (name.contains("布洛芬") || name.contains("阿司匹林")) {
            return new Advice(
                    "建议随餐或饭后服用，以减少胃肠道刺激。",
                    "避免饮酒及辛辣刺激食物，不与其他解热镇痛药自行合用。",
                    "可能出现胃痛、反酸、恶心、头晕，少数人有消化道出血风险。",
                    "有消化道溃疡、出血倾向、肾功能异常或正在使用抗凝药者应咨询医生。");
        }
        if (name.contains("左氧氟沙星")) {
            return new Advice(
                    "每日固定时间服用，并与含钙、铁、镁的补充剂间隔至少2小时。",
                    "避免饮酒和过度日晒，服药前后不要同时大量饮用奶制品。",
                    "可能出现胃肠不适、头晕、失眠或肌腱疼痛。",
                    "如出现肌腱疼痛、麻木、心悸等症状，应停药并及时就医。");
        }
        if (name.contains("奥美拉唑")) {
            return new Advice(
                    "通常建议早餐前30分钟空腹服用，整粒吞服。",
                    "减少咖啡、浓茶、酒精及辛辣油腻食物。",
                    "可能出现头痛、腹胀、腹泻或便秘。",
                    "不要自行长期连续使用；症状反复时应复诊评估。");
        }
        if (name.contains("多潘立酮")) {
            return new Advice(
                    "一般在饭前15至30分钟服用，按医嘱控制次数。",
                    "避免饮酒和高脂饮食，少量多餐。",
                    "可能出现口干、腹部痉挛、头晕，少数人出现心悸。",
                    "有心律失常或正在服用影响心律药物者应提前咨询医生。");
        }
        if (name.contains("阿托伐他汀")) {
            return new Advice(
                    "每日固定时间服用，通常可在晚间服用，是否随餐均可。",
                    "避免大量饮酒和葡萄柚；控制高脂饮食。",
                    "可能出现肌肉酸痛、乏力或肝功能指标异常。",
                    "如出现持续肌痛、深色尿或明显乏力，应立即就医。");
        }
        return new Advice(
                "请按照处方规定的固定时间服用，胃部不适者可咨询药师是否改为饭后服用。",
                "服药期间避免饮酒，饮食清淡，不与成分不明的保健品同服。",
                "可能出现胃肠不适、头晕或皮疹等反应。",
                "不自行改变剂量；如出现严重或持续不适，请及时咨询医生。");
    }

    private String resolveDiagnosis(Prescription prescription) {
        if (prescription.getRegistrationId() != null) {
            MedicalRecord record = medicalRecordRepository
                    .findByRegistrationId(prescription.getRegistrationId()).orElse(null);
            if (record != null
                    && Objects.equals(record.getPatientId(), prescription.getPatientId())
                    && record.getDiagnosis() != null
                    && !record.getDiagnosis().isBlank()) {
                return record.getDiagnosis();
            }
        }

        String drugs = safe(prescription.getDrugs(), "");
        if (drugs.contains("阿奇霉素") || drugs.contains("氨溴索")) return "呼吸道感染伴咳嗽、咳痰";
        if (drugs.contains("奥美拉唑") || drugs.contains("多潘立酮")) return "消化系统不适";
        if (drugs.contains("氨氯地平") || drugs.contains("美托洛尔")) return "心血管慢性病";
        return "处方相关疾病";
    }

    private String buildReadableGuide(List<Map<String, Object>> medications, String diagnosis) {
        StringBuilder content = new StringBuilder("【AI个性化用药指导】\n");
        content.append("诊断：").append(diagnosis).append("\n\n");
        for (int i = 0; i < medications.size(); i++) {
            Map<String, Object> medication = medications.get(i);
            content.append(i + 1).append(". ").append(medication.get("name")).append("\n")
                    .append("用法：").append(medication.get("usage")).append("\n")
                    .append("服用时间：").append(medication.get("takingTime")).append("\n")
                    .append("饮食禁忌：").append(medication.get("dietRestrictions")).append("\n")
                    .append("不良反应：").append(medication.get("adverseReactions")).append("\n")
                    .append("注意事项：").append(medication.get("precautions")).append("\n\n");
        }
        content.append("请严格遵医嘱用药。如出现严重过敏反应、呼吸困难或症状明显加重，请立即就医。");
        return content.toString();
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private record Advice(
            String takingTime,
            String dietRestrictions,
            String adverseReactions,
            String precautions) {}

    private record AiPayload(
            List<Map<String, Object>> medications,
            List<String> generalAdvice,
            String followUpAdvice,
            String guideContent,
            boolean aiGenerated) {}
}
