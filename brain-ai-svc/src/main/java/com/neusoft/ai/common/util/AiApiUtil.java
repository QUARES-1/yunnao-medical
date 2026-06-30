package com.neusoft.ai.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * AI API 调用工具 — 支持 mock / deepseek / doubao / 通义千问 / dify
 */
@Slf4j
@Component
public class AiApiUtil {

    @Value("${ai.provider:mock}")
    private String provider;

    @Value("${ai.api-key:}")
    private String apiKey;

    @Value("${ai.model:deepseek-chat}")
    private String model;

    @Value("${ai.base-url:}")
    private String baseUrl;

    @Value("${ai.timeout:30000}")
    private int timeout;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired(required = false)
    private DifyClient difyClient;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public String callAi(String prompt, String systemPrompt) {
        if ("mock".equalsIgnoreCase(provider)) {
            log.info("[AiApiUtil Mock] systemPrompt={}, prompt={}", systemPrompt, prompt);
            return mockCall(prompt, systemPrompt);
        }
        if ("dify".equalsIgnoreCase(provider)) {
            return callViaDify(prompt, systemPrompt);
        }
        return realCall(prompt, systemPrompt);
    }

    /**
     * 通过 Dify 平台调用 AI
     * ChatFlow 用于对话类（智能客服/健康咨询/分诊）
     * Workflow 用于流程类（病历生成/处方审核/库存预测/质检/运营报告）
     */
    private String callViaDify(String prompt, String systemPrompt) {
        if (difyClient == null) {
            log.warn("DifyClient 未注入，回退 mock 模式");
            return mockCall(prompt, systemPrompt);
        }
        String appKey = difyClient.resolveAppKey(systemPrompt);
        if (appKey == null || appKey.isBlank()) {
            log.warn("Dify appKey 未配置，回退 mock 模式");
            return mockCall(prompt, systemPrompt);
        }

        // ChatFlow 类应用：systemPrompt 不含复杂结构化要求时走 ChatFlow
        if (isChatFlowApp(systemPrompt)) {
            String query = prompt;
            // 把 systemPrompt 信息拼入 query，让 Dify 侧的 system prompt 生效
            return difyClient.callChatFlow(appKey, query, "system");
        }

        // Workflow 类应用：将 prompt 和 systemPrompt 作为输入参数传给 Workflow
        Map<String, Object> inputs = Map.of(
                "inputText", prompt,
                "systemPrompt", systemPrompt
        );
        return difyClient.runWorkflow(appKey, inputs, "system");
    }

    /**
     * 判断是否使用 ChatFlow（对话型）而非 Workflow（工作流型）
     */
    private boolean isChatFlowApp(String systemPrompt) {
        if (systemPrompt == null) return true;
        String sp = systemPrompt.toLowerCase();
        // ChatFlow 类关键词
        if (sp.contains("分诊") || sp.contains("triage")) return true;
        if (sp.contains("健康顾问") || sp.contains("健康建议") || sp.contains("health")) return true;
        // 其余走 Workflow
        return false;
    }

    public <T> T callAiJson(String prompt, String systemPrompt, Class<T> clazz) {
        String response = callAi(prompt, systemPrompt);
        try {
            return objectMapper.readValue(response, clazz);
        } catch (JsonProcessingException e) {
            log.error("AI返回JSON解析失败: {}", response, e);
            throw new RuntimeException("AI返回格式异常", e);
        }
    }

    public void callAiStream(String prompt, String systemPrompt, Consumer<String> callback) {
        String result = callAi(prompt, systemPrompt);
        callback.accept(result);
    }

    // ==================== Mock 实现 ====================

    private String mockCall(String prompt, String systemPrompt) {
        if (systemPrompt.contains("分诊") || systemPrompt.contains("triage")) {
            return mockTriage(prompt);
        }
        if (systemPrompt.contains("处方") || systemPrompt.contains("prescription")) {
            return mockPrescriptionReview();
        }
        if (systemPrompt.contains("病历") || systemPrompt.contains("medical record")) {
            return mockMedicalRecord();
        }
        if (systemPrompt.contains("用药") || systemPrompt.contains("medication")) {
            return mockMedicationGuide();
        }
        if (systemPrompt.contains("检验") || systemPrompt.contains("examination")) {
            return mockExaminationInterpret();
        }
        if (systemPrompt.contains("危急") || systemPrompt.contains("critical")) {
            return mockCriticalValue();
        }
        if (systemPrompt.contains("随访") || systemPrompt.contains("follow-up")) {
            return mockFollowUp();
        }
        if (systemPrompt.contains("健康顾问") || systemPrompt.contains("健康建议")) {
            return mockHealthConsult();
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
        if (systemPrompt.contains("审核") || systemPrompt.contains("review")) {
            return mockExaminationReview();
        }
        return mockGeneralChat(prompt);
    }

    private String mockTriage(String prompt) {
        String text = prompt.toLowerCase();
        if (containsAny(text, "牙", "口腔", "牙痛")) {
            return "{\"recommendDepartment\":\"口腔科\",\"recommendDepartmentId\":6,\"recommendDoctorIds\":\"\",\"analysis\":\"根据患者症状，建议前往口腔科就诊。\",\"confidence\":85}";
        }
        if (containsAny(text, "眼", "视力", "眼痛")) {
            return "{\"recommendDepartment\":\"眼科\",\"recommendDepartmentId\":4,\"recommendDoctorIds\":\"\",\"analysis\":\"根据患者症状，建议前往眼科就诊。\",\"confidence\":90}";
        }
        if (containsAny(text, "咳", "喘", "呼吸", "肺")) {
            return "{\"recommendDepartment\":\"呼吸内科\",\"recommendDepartmentId\":10,\"recommendDoctorIds\":\"\",\"analysis\":\"根据患者症状，建议前往呼吸内科就诊。\",\"confidence\":88}";
        }
        if (containsAny(text, "胃", "腹", "肚子", "恶心", "呕吐", "腹泻")) {
            return "{\"recommendDepartment\":\"消化内科\",\"recommendDepartmentId\":11,\"recommendDoctorIds\":\"\",\"analysis\":\"根据患者症状，建议前往消化内科就诊。\",\"confidence\":87}";
        }
        if (containsAny(text, "骨", "关节", "腰", "腿", "扭伤", "骨折")) {
            return "{\"recommendDepartment\":\"骨科\",\"recommendDepartmentId\":12,\"recommendDoctorIds\":\"\",\"analysis\":\"根据患者症状，建议前往骨科就诊。\",\"confidence\":85}";
        }
        if (containsAny(text, "心", "胸痛", "心慌", "血压")) {
            return "{\"recommendDepartment\":\"心血管内科\",\"recommendDepartmentId\":9,\"recommendDoctorIds\":\"\",\"analysis\":\"根据患者症状，建议前往心血管内科就诊。\",\"confidence\":88}";
        }
        if (containsAny(text, "儿童", "孩子", "宝宝")) {
            return "{\"recommendDepartment\":\"儿科\",\"recommendDepartmentId\":2,\"recommendDoctorIds\":\"\",\"analysis\":\"根据患者年龄特点，建议前往儿科就诊。\",\"confidence\":92}";
        }
        if (containsAny(text, "发烧", "发热", "高热")) {
            return "{\"recommendDepartment\":\"急诊科\",\"recommendDepartmentId\":14,\"recommendDoctorIds\":\"\",\"analysis\":\"患者有发热症状，建议急诊就诊。\",\"confidence\":90}";
        }
        return "{\"recommendDepartment\":\"内科\",\"recommendDepartmentId\":20,\"recommendDoctorIds\":\"\",\"analysis\":\"根据患者症状，建议前往内科就诊。\",\"confidence\":75}";
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null) return false;
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }

    private String mockPrescriptionReview() {
        return "{\"reviewResult\":\"warning\",\"reviewScore\":75,\"warnings\":[{\"level\":\"medium\",\"content\":\"两种药物可能存在相互作用风险，建议间隔2小时服用\"}],\"suggestions\":\"建议增加胃黏膜保护剂\",\"drugInteractions\":[{\"drug1\":\"阿莫西林\",\"drug2\":\"布洛芬\",\"level\":\"moderate\",\"description\":\"可能增加胃肠道出血风险\"}],\"allergyRisks\":[],\"dosageIssues\":[]}";
    }

    private String mockMedicalRecord() {
        return "{\"chiefComplaint\":\"咳嗽、咳痰3天，伴发热1天\",\"presentIllness\":\"患者3天前受凉后出现咳嗽，咳白色黏痰，量中等，易咳出。1天前出现发热，体温最高38.5℃，伴畏寒，无寒战。\"既往史：既往体健，否认高血压、糖尿病等慢性病史。\",\"pastHistory\":\"既往体健，否认高血压、糖尿病等慢性病史。否认药物过敏史。\",\"physicalExamination\":\"T:38.2℃，P:88次/分，R:20次/分，BP:120/80mmHg。咽部充血，扁桃体I°肿大。双肺呼吸音粗，未闻及干湿性啰音。\",\"diagnosis\":\"急性上呼吸道感染\",\"treatment\":\"1. 注意休息，多饮水；2. 布洛芬混悬液 10ml 发热时口服；3. 氨溴索口服液 10ml tid；4. 如有加重及时就诊。\"}";
    }

    private String mockMedicationGuide() {
        return "{\"guide\":{\"medications\":[{\"name\":\"阿莫西林\",\"dosage\":\"0.5g\",\"frequency\":\"每日3次\",\"duration\":\"7天\",\"notes\":\"餐后服用\"}],\"generalAdvice\":[\"按时服药，不可自行停药\",\"用药期间避免饮酒\"]}}";
    }

    private String mockExaminationInterpret() {
        return "{\"interpretation\":\"各项指标在正常范围内，未见明显异常。\",\"keyFindings\":[],\"suggestions\":[\"定期复查\"]}";
    }

    private String mockCriticalValue() {
        return "{\"hasCriticalValue\":false,\"criticalItems\":[],\"analysis\":\"本次检查未发现危急值。\"}";
    }

    private String mockHealthConsult() {
        return "{\"answer\":\"根据您的描述，建议适当休息，多饮水。如果症状持续超过3天，建议前往医院就诊。\",\"relatedQuestions\":[\"什么情况下需要就医？\",\"如何缓解症状？\"],\"recommendDepartment\":\"呼吸内科\",\"recommendDepartmentId\":10}";
    }

    private String mockFollowUp() {
        return "{\"analysis\":\"患者恢复情况良好，各项指标正常，建议继续保持当前治疗方案。\",\"abnormal\":false}";
    }

    private String mockOperationReport() {
        return "{\"summary\":\"本周门诊量与上周基本持平，儿科和内科患者较多。\",\"keyMetrics\":{\"totalRegistrations\":1250,\"totalRevenue\":285000,\"avgDailyRegistrations\":178,\"departmentTop3\":[\"内科\",\"儿科\",\"外科\"]},\"trendsAnalysis\":\"门诊量呈现缓慢上升趋势，预计下周会略有增加。\",\"forecasts\":{\"nextWeekRegistrations\":1350,\"trend\":\"up\"},\"warnings\":[{\"level\":\"info\",\"content\":\"药品库存预警：部分感冒药库存低于安全线\"}],\"suggestions\":[\"建议增加儿科的排班人数\",\"建议提前采购感冒类药物\"]}";
    }

    private String mockQualityCheck() {
        return "{\"totalCount\":10,\"passCount\":8,\"avgScore\":85.5,\"problemSummary\":\"主要问题集中在诊断描述不够规范\",\"improvementSuggestions\":\"建议统一诊断书写规范\",\"details\":[{\"index\":1,\"score\":85,\"problems\":[{\"level\":\"minor\",\"field\":\"现病史\",\"content\":\"缺少发病诱因\"}],\"suggestions\":\"建议补充\"}]}";
    }

    private String mockStockForecast() {
        return "{\"purchaseSuggestions\":[\"优先采购高风险与需补货药品，建议分两批到货\"],\"factors\":[\"近30天与近90天处方消耗\",\"当前库存与安全库存\",\"季节性疾病和近期处方量趋势\"]}";
    }

    private String mockExaminationReview() {
        return "{\"reviewResult\":\"pass\",\"reviewScore\":90,\"suggestions\":\"检验结果与临床诊断一致，建议定期复查。\",\"warnings\":[]}";
    }

    private String mockGeneralChat(String question) {
        return "{\"answer\":\"您好！我是云脑诊疗平台的智能助手。关于'" + question + "'，建议您：1. 如症状轻微可先观察；2. 如症状持续建议及时就医。请问您还有其他问题吗？\",\"relatedQuestions\":[\"应该挂什么科？\",\"需要带什么材料？\"]}";
    }

    // ==================== 真实调用 ====================

    private String realCall(String prompt, String systemPrompt) {
        try {
            String url = buildChatCompletionsUrl();
            String requestBody = buildRequestBody(prompt, systemPrompt);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofMillis(timeout))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("AI API 调用失败, status={}, body={}", response.statusCode(), response.body());
                return "{}";
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode content = root.path("choices").get(0).path("message").path("content");

            String result = content.asText();
            return normalizeJsonContent(result);

        } catch (Exception e) {
            log.error("AI API 调用异常", e);
            return "{}";
        }
    }

    private String buildChatCompletionsUrl() {
        if (baseUrl != null && !baseUrl.isBlank()) {
            return baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";
        }
        return "https://api.deepseek.com/v1/chat/completions";
    }

    private String buildRequestBody(String prompt, String systemPrompt) {
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", prompt)
                    ),
                    "temperature", 0.3,
                    "max_tokens", 4096
            );
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("构建请求体失败", e);
        }
    }

    /**
     * 归一化 AI 返回的 JSON 内容：移除 markdown 代码块包裹
     */
    private String normalizeJsonContent(String content) {
        if (content == null) return "{}";
        String trimmed = content.trim();
        if (trimmed.startsWith("```json")) {
            int start = trimmed.indexOf('\n') + 1;
            int end = trimmed.lastIndexOf("```");
            if (end > start) {
                return trimmed.substring(start, end).trim();
            }
        } else if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('\n') + 1;
            int end = trimmed.lastIndexOf("```");
            if (end > start) {
                return trimmed.substring(start, end).trim();
            }
        }
        return trimmed;
    }
}
