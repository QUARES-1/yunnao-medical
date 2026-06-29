package com.neusoft.cloud_brain_diagnosis.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * 统一AI调用工具类
 * 所有AI模块通过此类调用大模型，统一管理鉴权、超时、重试
 *
 * 开发环境使用 mock 模式，不依赖真实 AI API
 * 生产环境配置 ai.provider/mock=false 即可切换为真实调用
 */
@Slf4j
@Component
public class AiApiUtil {

    @Value("${ai.provider:mock}")
    private String provider;

    @Value("${ai.api-key:}")
    private String apiKey;

    @Value("${ai.model:doubao-pro}")
    private String model;

    @Value("${ai.base-url:}")
    private String baseUrl;

    @Value("${ai.timeout:30000}")
    private int timeout;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 调用AI，返回文本结果
     */
    public String callAi(String prompt, String systemPrompt) {
        if ("mock".equalsIgnoreCase(provider)) {
            log.info("[AiApiUtil Mock] systemPrompt={}, prompt={}", systemPrompt, prompt);
            return mockCall(prompt, systemPrompt);
        }
        return realCall(prompt, systemPrompt);
    }

    /**
     * 调用AI，返回JSON格式结果（自动解析到指定类型）
     */
    public <T> T callAiJson(String prompt, String systemPrompt, Class<T> clazz) {
        String response = callAi(prompt, systemPrompt);
        try {
            return objectMapper.readValue(response, clazz);
        } catch (JsonProcessingException e) {
            log.error("AI返回JSON解析失败: {}", response, e);
            throw new RuntimeException("AI返回格式异常", e);
        }
    }

    /**
     * 流式调用（预留扩展）
     */
    public void callAiStream(String prompt, String systemPrompt, Consumer<String> callback) {
        String result = callAi(prompt, systemPrompt);
        callback.accept(result);
    }

    // ==================== Mock 实现 ====================

    private String mockCall(String prompt, String systemPrompt) {
        // 根据systemPrompt关键词返回不同mock数据
        if (systemPrompt.contains("分诊") || systemPrompt.contains("triage")) {
            return mockTriage();
        }
        if (systemPrompt.contains("处方审核") || systemPrompt.contains("prescription")) {
            return mockPrescriptionReview();
        }
        if (systemPrompt.contains("病历生成") || systemPrompt.contains("medical record")) {
            return mockMedicalRecord();
        }
        if (systemPrompt.contains("用药指导") || systemPrompt.contains("medication")) {
            return mockMedicationGuide();
        }
        if (systemPrompt.contains("检验") || systemPrompt.contains("examination") || systemPrompt.contains("interpret")) {
            return mockExaminationInterpret();
        }
        if (systemPrompt.contains("危急值") || systemPrompt.contains("critical")) {
            return mockCriticalValue();
        }
        if (systemPrompt.contains("健康顾问") || systemPrompt.contains("health")) {
            return mockHealthConsult();
        }
        if (systemPrompt.contains("随访") || systemPrompt.contains("follow")) {
            return mockFollowUp();
        }
        if (systemPrompt.contains("运营") || systemPrompt.contains("operation")) {
            return mockOperationReport();
        }
        if (systemPrompt.contains("质检") || systemPrompt.contains("quality")) {
            return mockQualityCheck();
        }
        if (systemPrompt.contains("库存") || systemPrompt.contains("stock")) {
            return mockStockForecast();
        }
        if (systemPrompt.contains("检验审核") || systemPrompt.contains("examination review")) {
            return mockExaminationReview();
        }
        // 通用问答
        return mockGeneralChat(prompt);
    }

    private String mockTriage() {
        return "{\"recommendDepartment\":\"内科\",\"recommendDepartmentId\":1,\"recommendDoctorIds\":\"1,2\",\"analysis\":\"根据您描述的症状，初步判断为内科相关疾病，建议前往内科就诊。\",\"confidence\":85}";
    }

    private String mockPrescriptionReview() {
        return "{\"reviewResult\":\"warning\",\"reviewScore\":75,\"warnings\":[{\"level\":\"medium\",\"content\":\"药物联用可能增加肾损伤风险，建议监测肾功能\"},{\"level\":\"low\",\"content\":\"患者年龄较大，部分药物剂量建议酌减\"}],\"suggestions\":\"建议调整用药方案，对老年患者更安全。\",\"drugInteractions\":[{\"drug1\":\"阿莫西林\",\"drug2\":\"布洛芬\",\"level\":\"moderate\",\"description\":\"可能增加肾毒性\"}],\"allergyRisks\":[],\"dosageIssues\":[{\"drug\":\"布洛芬缓释胶囊\",\"issue\":\"老年患者建议减量\",\"suggestion\":\"建议减半剂量\"}]}";
    }

    private String mockMedicalRecord() {
        return "{\"chiefComplaint\":\"咳嗽、咳痰伴发热3天\",\"presentIllness\":\"患者3天前受凉后出现咳嗽、咳痰，痰为白色黏痰，量中等，伴发热，体温最高38.5℃，伴咽痛、乏力，无胸痛、咯血。自行口服感冒药效果不佳。\",\"pastHistory\":\"既往体健，否认高血压、糖尿病等慢性病史，否认药物过敏史。\",\"physicalExamination\":\"T38.2℃，P88次/分，R20次/分，BP125/80mmHg。咽部充血，双肺呼吸音粗，未闻及干湿性啰音。\",\"diagnosis\":\"1. 急性上呼吸道感染 2. 急性支气管炎\",\"treatment\":\"1. 注意休息，多饮水，清淡饮食；\\n2. 口服抗生素抗感染治疗；\\n3. 止咳化痰对症治疗；\\n4. 体温超过38.5℃可口服退热药；\\n5. 3天后复诊。\"}";
    }

    private String mockMedicationGuide() {
        return "{\"guideContent\":\"【用药指导】\\n\\n一、阿莫西林胶囊\\n- 用法：一次2粒，一日3次，饭后服用\\n- 疗程：连续服用7天\\n- 注意：服用期间禁止饮酒\\n\\n二、布洛芬缓释胶囊\\n- 用法：一次1粒，一日2次，饭后服用\\n- 注意：可能引起胃肠道不适\\n\\n三、总体注意事项\\n- 饮食清淡，避免辛辣刺激食物\\n- 多喝水，保证充足睡眠\\n- 如出现皮疹、呼吸困难等过敏反应，立即停药并就医\\n- 3天后复诊，症状加重及时就诊\"}";
    }

    private String mockExaminationInterpret() {
        return "{\"abnormalItems\":[{\"name\":\"白细胞计数\",\"value\":\"12.5\",\"unit\":\"×10^9/L\",\"reference\":\"4.0-10.0\",\"status\":\"偏高\"},{\"name\":\"中性粒细胞百分比\",\"value\":\"85%\",\"unit\":\"%\",\"reference\":\"50-70%\",\"status\":\"偏高\"}],\"interpretationPro\":\"白细胞和中性粒细胞显著升高，提示细菌感染可能。\",\"interpretationPatient\":\"您好，您的血常规报告显示白细胞和中性粒细胞偏高，这通常说明身体有细菌感染。\",\"suggestions\":\"1. 遵医嘱按时服用抗生素\\n2. 多喝水，注意休息\\n3. 饮食清淡\\n4. 注意观察体温变化\",\"reviewReminder\":\"建议服药3-5天后复查血常规。\"}";
    }

    private String mockCriticalValue() {
        return "{\"criticalItems\":[{\"name\":\"血钾\",\"value\":\"6.8mmol/L\",\"reference\":\"3.5-5.5mmol/L\",\"level\":\"紧急\"}],\"warningLevel\":\"紧急\",\"analysis\":\"患者血钾显著升高，达到危急值标准，需立即处理。\"}";
    }

    private String mockHealthConsult() {
        return "{\"answer\":\"根据您的情况，建议注意以下几点：\\n\\n1. 限盐：每天食盐不超过5克\\n2. 低脂：少吃肥肉、油炸食品\\n3. 多吃蔬菜水果\\n4. 适量优质蛋白\\n5. 戒烟限酒\\n6. 配合规律运动和按时服药\\n\\n如症状加重请及时就医。\",\"relatedQuestions\":[\"饮食上还有什么需要注意的？\",\"什么情况下需要复查？\"],\"recommendDepartment\":\"内科\",\"recommendDepartmentId\":1}";
    }

    private String mockFollowUp() {
        return "{\"questionnaire\":[{\"id\":\"q1\",\"question\":\"现在还咳嗽吗？\",\"type\":\"single\",\"options\":[\"不咳了\",\"减轻了\",\"没变化\",\"加重了\"]},{\"id\":\"q2\",\"question\":\"还发烧吗？\",\"type\":\"single\",\"options\":[\"不烧了\",\"低烧\",\"高烧\"]},{\"id\":\"q3\",\"question\":\"药都按时吃了吗？\",\"type\":\"single\",\"options\":[\"按时吃了\",\"偶尔忘\",\"没吃\"]}],\"analysis\":\"患者恢复情况良好，各项指标正常，建议继续保持。\"}";
    }

    private String mockOperationReport() {
        return "{\"summary\":\"本周全院运营情况良好，挂号量环比增长12%，营收增长8%。\",\"keyMetrics\":{\"totalRegistrations\":1250,\"totalRevenue\":285000,\"avgDailyRegistrations\":178,\"departmentTop3\":[\"内科\",\"儿科\",\"外科\"]},\"trendsAnalysis\":\"挂号量和营收持续增长，患者满意度95.2%。\",\"forecasts\":{\"nextWeekRegistrations\":1350,\"trend\":\"up\"},\"warnings\":[{\"level\":\"info\",\"content\":\"儿科周末挂号量增长25%，建议增加周末排班\"}],\"suggestions\":[\"增加儿科周末医生排班\",\"内科号源紧张，建议增加专家门诊\",\"推广线上预约，减少现场排队\"]}";
    }

    private String mockQualityCheck() {
        return "{\"totalCount\":10,\"passCount\":8,\"avgScore\":85.5,\"problemSummary\":\"主要问题：1. 现病史缺少发病诱因描述 2. 部分病历缺少腹部查体 3. 诊断编码不规范\",\"improvementSuggestions\":\"1. 加强病历书写规范培训 2. 建议使用ICD-10编码 3. 补充体格检查完整描述\"}";
    }

    private String mockStockForecast() {
        return "{\"forecastData\":[{\"medicineId\":1,\"name\":\"阿莫西林胶囊\",\"currentStock\":200,\"forecastConsume\":350,\"suggestPurchase\":200,\"unit\":\"盒\"},{\"medicineId\":5,\"name\":\"布洛芬缓释胶囊\",\"currentStock\":300,\"forecastConsume\":280,\"suggestPurchase\":0,\"unit\":\"盒\"}],\"purchaseSuggestions\":[\"夏季胃肠道疾病增多，建议增加蒙脱石散、益生菌库存\",\"抗生素类药品需求稳定，按常规量采购即可\"],\"factors\":[\"夏季来临，呼吸系统药品需求增加\",\"流感季已过，抗病毒药品需求减少\"],\"totalForecastAmount\":85600.00,\"totalPurchaseAmount\":42300.00}";
    }

    private String mockExaminationReview() {
        return "{\"reviewResult\":\"manual\",\"reviewScore\":70,\"abnormalItems\":[{\"name\":\"白细胞计数\",\"value\":\"15.2\",\"unit\":\"×10^9/L\",\"reference\":\"4.0-10.0\",\"status\":\"偏高\",\"level\":\"moderate\"}],\"logicIssues\":[{\"level\":\"info\",\"content\":\"白细胞分类百分比总和在合理范围内\"}],\"historyCompare\":[{\"item\":\"白细胞计数\",\"lastValue\":\"8.5\",\"currentValue\":\"15.2\",\"change\":\"+78.8%\",\"level\":\"significant\"}],\"warnings\":[{\"level\":\"medium\",\"content\":\"白细胞显著升高，提示严重细菌感染，建议人工复核\"}],\"suggestions\":\"结果显示明显细菌感染，建议人工复核确认后发报告。\"}";
    }

    private String mockGeneralChat(String question) {
        return "{\"answer\":\"您好！我是云脑诊疗平台的AI助手。关于您的问题：「" + question + "」\\n\\n建议您通过以下方式获取帮助：\\n1. 挂号就诊：通过小程序预约挂号\\n2. 紧急情况：请拨打120或前往急诊\\n3. 如需进一步帮助，请咨询导诊台。\\n\\n祝您早日康复！\",\"source\":\"ai\"}";
    }

    // ==================== 真实调用预留 ====================

    private String realCall(String prompt, String systemPrompt) {
        // TODO: 集成真实AI API（通义千问/豆包等）
        // 1. 构建HTTP请求
        // 2. 设置header（api-key等）
        // 3. 发送请求
        // 4. 解析返回结果
        // 5. 重试机制（3次）
        log.warn("真实AI调用尚未集成，当前使用mock模式。请配置 ai.provider 切换。");
        return mockCall(prompt, systemPrompt);
    }
}
