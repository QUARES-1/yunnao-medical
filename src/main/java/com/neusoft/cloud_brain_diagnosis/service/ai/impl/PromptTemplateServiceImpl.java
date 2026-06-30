package com.neusoft.cloud_brain_diagnosis.service.ai.impl;

import com.neusoft.cloud_brain_diagnosis.service.ai.PromptTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 可配置的提示词模板服务 — 从 application.yaml 加载科室专属模板
 * <p>
 * YAML 配置结构:
 * <pre>
 * ai:
 *   prompts:
 *     medical-record:
 *       default:
 *         system: "你是一名经验丰富的临床医生..."
 *         user: "请根据以下信息生成病历...\n{inputText}"
 *       pediatric:
 *         system: "..."
 *         user: "..."
 *     prescription-review:
 *       default:
 *         system: "..."
 *         user: "..."
 * </pre>
 */
@Slf4j
@Service
public class PromptTemplateServiceImpl implements PromptTemplateService {

    // ==================== 病历生成模板 ====================

    @Value("${ai.prompts.medical-record.default.system:}")
    private String mrDefaultSystem;

    @Value("${ai.prompts.medical-record.default.user:}")
    private String mrDefaultUser;

    @Value("${ai.prompts.medical-record.pediatric.system:}")
    private String mrPediatricSystem;

    @Value("${ai.prompts.medical-record.pediatric.user:}")
    private String mrPediatricUser;

    @Value("${ai.prompts.medical-record.cardiology.system:}")
    private String mrCardiologySystem;

    @Value("${ai.prompts.medical-record.cardiology.user:}")
    private String mrCardiologyUser;

    @Value("${ai.prompts.medical-record.respiratory.system:}")
    private String mrRespiratorySystem;

    @Value("${ai.prompts.medical-record.respiratory.user:}")
    private String mrRespiratoryUser;

    @Value("${ai.prompts.medical-record.orthopedics.system:}")
    private String mrOrthopedicsSystem;

    @Value("${ai.prompts.medical-record.orthopedics.user:}")
    private String mrOrthopedicsUser;

    @Value("${ai.prompts.medical-record.ent.system:}")
    private String mrEntSystem;

    @Value("${ai.prompts.medical-record.ent.user:}")
    private String mrEntUser;

    // ==================== 处方审核模板 ====================

    @Value("${ai.prompts.prescription-review.default.system:}")
    private String prDefaultSystem;

    @Value("${ai.prompts.prescription-review.default.user:}")
    private String prDefaultUser;

    @Value("${ai.prompts.prescription-review.pediatric.system:}")
    private String prPediatricSystem;

    @Value("${ai.prompts.prescription-review.pediatric.user:}")
    private String prPediatricUser;

    @Value("${ai.prompts.prescription-review.cardiology.system:}")
    private String prCardiologySystem;

    @Value("${ai.prompts.prescription-review.cardiology.user:}")
    private String prCardiologyUser;

    // ==================== 科室匹配映射 ====================

    private static final Map<String, String> DEPT_KEY_MAP = new LinkedHashMap<>();

    static {
        // 按优先级从高到低匹配
        DEPT_KEY_MAP.put("儿科", "pediatric");
        DEPT_KEY_MAP.put("新生儿科", "pediatric");
        DEPT_KEY_MAP.put("小儿", "pediatric");
        DEPT_KEY_MAP.put("心血管内科", "cardiology");
        DEPT_KEY_MAP.put("心内科", "cardiology");
        DEPT_KEY_MAP.put("心脏", "cardiology");
        DEPT_KEY_MAP.put("呼吸内科", "respiratory");
        DEPT_KEY_MAP.put("呼吸科", "respiratory");
        DEPT_KEY_MAP.put("呼吸", "respiratory");
        DEPT_KEY_MAP.put("骨科", "orthopedics");
        DEPT_KEY_MAP.put("骨伤", "orthopedics");
        DEPT_KEY_MAP.put("耳鼻喉科", "ent");
        DEPT_KEY_MAP.put("耳鼻喉", "ent");
    }

    @Override
    public Map<String, String> getMedicalRecordTemplate(String departmentName, String inputText) {
        String deptKey = resolveDeptKey(departmentName);
        String systemPrompt = loadConfig("medical-record", deptKey, "system", mrDefaultSystem,
                "你是一名经验丰富的临床医生，请根据输入的对话/关键词生成一份规范的病历。"
                        + "病历需包含：主诉、现病史、既往史、体格检查、初步诊断、治疗意见。"
                        + "请严格按以下JSON格式返回："
                        + "{\"chiefComplaint\":\"主诉\",\"presentIllness\":\"现病史\","
                        + "\"pastHistory\":\"既往史\",\"physicalExamination\":\"体格检查\","
                        + "\"diagnosis\":\"诊断\",\"treatment\":\"治疗意见\"}");

        String userPrompt = loadConfig("medical-record", deptKey, "user", mrDefaultUser,
                "请根据以下信息生成一份完整的门（急）诊病历：\n{inputText}");
        userPrompt = userPrompt.replace("{inputText}", inputText != null ? inputText : "");

        log.debug("[PromptTemplate] 病历生成模板 department={} -> deptKey={}", departmentName, deptKey);
        return Map.of("system", systemPrompt, "user", userPrompt);
    }

    @Override
    public Map<String, String> getPrescriptionReviewTemplate(String departmentName, String patientInfo, String drugsDesc) {
        String deptKey = resolveDeptKey(departmentName);
        String systemPrompt = loadConfig("prescription-review", deptKey, "system", prDefaultSystem,
                "你是一名经验丰富的临床药师，请严格审核处方中的药品。"
                        + "检查要点：药物相互作用、配伍禁忌、剂量合理性、过敏风险、重复用药。"
                        + "请按以下JSON格式返回审核结果："
                        + "{\"reviewResult\":\"pass/warning/reject\",\"reviewScore\":0-100,"
                        + "\"warnings\":[{\"level\":\"low/medium/high\",\"content\":\"警告内容\"}],"
                        + "\"suggestions\":\"修改建议\","
                        + "\"drugInteractions\":[{\"drug1\":\"药品A\",\"drug2\":\"药品B\",\"level\":\"low/moderate/high\",\"description\":\"相互作用描述\"}],"
                        + "\"allergyRisks\":[],"
                        + "\"dosageIssues\":[{\"drug\":\"药品名\",\"issue\":\"问题\",\"suggestion\":\"建议\"}]}");

        String userPrompt = loadConfig("prescription-review", deptKey, "user", prDefaultUser,
                "请审核以下处方：\n{patientInfo}\n药品列表：\n{drugsDesc}");
        userPrompt = userPrompt.replace("{patientInfo}", patientInfo != null ? patientInfo : "")
                .replace("{drugsDesc}", drugsDesc != null ? drugsDesc : "");

        log.debug("[PromptTemplate] 处方审核模板 department={} -> deptKey={}", departmentName, deptKey);
        return Map.of("system", systemPrompt, "user", userPrompt);
    }

    // ==================== 内部方法 ====================

    /**
     * 根据科室名称解析出配置键名
     */
    private String resolveDeptKey(String departmentName) {
        if (departmentName == null || departmentName.isBlank()) {
            return "default";
        }
        for (Map.Entry<String, String> entry : DEPT_KEY_MAP.entrySet()) {
            if (departmentName.contains(entry.getKey())) {
                String key = entry.getValue();
                // 检查对应的配置是否确实存在（非空），不存在则降级为 default
                if (hasConfigForDeptKey(key)) {
                    return key;
                }
                return "default";
            }
        }
        return "default";
    }

    /**
     * 检查某个科室键是否配置了自定义模板（至少有一套system+user配置）
     */
    private boolean hasConfigForDeptKey(String deptKey) {
        String system = getSystemByDeptKey("medical-record", deptKey);
        if (system != null && !system.isBlank()) return true;
        system = getSystemByDeptKey("prescription-review", deptKey);
        return system != null && !system.isBlank();
    }

    private String getSystemByDeptKey(String category, String deptKey) {
        return switch (deptKey) {
            case "pediatric" -> "pediatric".equals(deptKey) && "medical-record".equals(category) ? mrPediatricSystem : prPediatricSystem;
            case "cardiology" -> "cardiology".equals(deptKey) && "medical-record".equals(category) ? mrCardiologySystem : prCardiologySystem;
            case "respiratory" -> mrRespiratorySystem;
            case "orthopedics" -> mrOrthopedicsSystem;
            case "ent" -> mrEntSystem;
            default -> null;
        };
    }

    /**
     * 加载配置：优先使用 YAML 配置值，若为空则使用代码内置的 fallback
     */
    private String loadConfig(String category, String deptKey, String field,
                              String configuredValue, String fallback) {
        if (configuredValue != null && !configuredValue.isBlank()) {
            return configuredValue;
        }
        return fallback;
    }
}
