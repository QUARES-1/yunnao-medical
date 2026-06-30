package com.neusoft.ai.common.util;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Dify API 客户端 — 调用 Dify 在线版的 ChatFlow 和 Workflow 应用
 *
 * 配置示例：
 *   dify.base-url = https://api.dify.ai/v1
 *   dify.apps.chat = app-xxxx
 *   dify.apps.triage = app-xxxx
 */
@Slf4j
@Component
public class DifyClient {

    @Value("${dify.base-url:https://api.dify.ai/v1}")
    private String baseUrl;

    // 8 个应用的 API Key，从配置注入
    @Value("${dify.apps.chat:}")
    private String chatAppKey;

    @Value("${dify.apps.health-consult:}")
    private String healthConsultAppKey;

    @Value("${dify.apps.triage:}")
    private String triageAppKey;

    @Value("${dify.apps.medical-record:}")
    private String medicalRecordAppKey;

    @Value("${dify.apps.prescription-review:}")
    private String prescriptionReviewAppKey;

    @Value("${dify.apps.stock-forecast:}")
    private String stockForecastAppKey;

    @Value("${dify.apps.quality-check:}")
    private String qualityCheckAppKey;

    @Value("${dify.apps.operation-report:}")
    private String operationReportAppKey;

    /**
     * 调用 Dify ChatFlow（对话型应用，阻塞模式）
     * POST /v1/chat-messages
     *
     * @param appKey  Dify 应用的 API Key
     * @param query   用户输入
     * @param user    用户标识
     * @return Dify 返回的回答文本
     */
    public String callChatFlow(String appKey, String query, String user) {
        JSONObject body = new JSONObject();
        body.set("inputs", new JSONObject());
        body.set("query", query);
        body.set("user", user);
        body.set("response_mode", "blocking");

        String result = HttpRequest.post(baseUrl + "/chat-messages")
                .header("Authorization", "Bearer " + appKey)
                .header("Content-Type", "application/json")
                .body(body.toString())
                .timeout(60000)
                .execute()
                .body();

        JSONObject json = JSONUtil.parseObj(result);
        String answer = json.getStr("answer");
        if (answer == null) {
            log.error("Dify ChatFlow 调用异常: {}", result);
            return "{}";
        }
        return answer;
    }

    /**
     * 调用 Dify Workflow（工作流应用，阻塞模式）
     * POST /v1/workflows/run
     *
     * @param appKey Dify 应用的 API Key
     * @param inputs 工作流输入变量（Map）
     * @param user   用户标识
     * @return 工作流输出的 outputs 字段（JSON 字符串）
     */
    public String runWorkflow(String appKey, Map<String, Object> inputs, String user) {
        JSONObject body = new JSONObject();
        body.set("inputs", inputs);
        body.set("user", user);
        body.set("response_mode", "blocking");

        String result = HttpRequest.post(baseUrl + "/workflows/run")
                .header("Authorization", "Bearer " + appKey)
                .header("Content-Type", "application/json")
                .body(body.toString())
                .timeout(120000)
                .execute()
                .body();

        JSONObject json = JSONUtil.parseObj(result);
        JSONObject data = json.getJSONObject("data");
        if (data == null) {
            log.error("Dify Workflow 调用异常: {}", result);
            return "{}";
        }
        String outputs = data.getStr("outputs");
        return outputs != null ? outputs : "{}";
    }

    /**
     * 根据 systemPrompt 关键词自动匹配对应的 Dify 应用 Key
     */
    public String resolveAppKey(String systemPrompt) {
        if (systemPrompt == null) return chatAppKey;
        String sp = systemPrompt.toLowerCase();
        if (sp.contains("分诊") || sp.contains("triage")) return triageAppKey;
        if (sp.contains("处方") || sp.contains("prescription") || sp.contains("药师")) return prescriptionReviewAppKey;
        if (sp.contains("病历") || sp.contains("medical record")) return medicalRecordAppKey;
        if (sp.contains("运营") || sp.contains("operation")) return operationReportAppKey;
        if (sp.contains("质检") || sp.contains("quality")) return qualityCheckAppKey;
        if (sp.contains("库存") || sp.contains("stock")) return stockForecastAppKey;
        if (sp.contains("健康") || sp.contains("健康顾问") || sp.contains("health")) return healthConsultAppKey;
        return chatAppKey;
    }
}
