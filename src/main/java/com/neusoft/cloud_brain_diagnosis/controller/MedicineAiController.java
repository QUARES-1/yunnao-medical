package com.neusoft.cloud_brain_diagnosis.controller;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.neusoft.cloud_brain_diagnosis.common.annotation.RequireLogin;
import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import com.neusoft.cloud_brain_diagnosis.feign.AiStockFeignClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/medicine/ai")
@RequiredArgsConstructor
@Tag(name = "AI库存预测", description = "AI药品库存智能预测模块")
public class MedicineAiController {

    private final AiStockFeignClient stockFeignClient;

    /**
     * 生成库存预测
     */
    @PostMapping("/stock-forecast/generate")
    @RequireLogin({RoleEnum.PHARMACY, RoleEnum.ADMIN})
    @Operation(summary = "生成库存预测", description = "生成月度/周度库存预测和采购建议")
    public Result<Map<String, Object>> generateForecast(@RequestBody Map<String, Object> request) {
        return stockFeignClient.generateForecast(request);
    }

    /**
     * 预测详情
     */
    @GetMapping("/stock-forecast/{id}")
    @RequireLogin({RoleEnum.PHARMACY, RoleEnum.ADMIN})
    @Operation(summary = "预测详情", description = "查看库存预测详情")
    public Result<Map<String, Object>> getForecastDetail(@PathVariable Long id) {
        return stockFeignClient.getForecastDetail(id);
    }

    /**
     * 预测列表
     */
    @GetMapping("/stock-forecast/list")
    @RequireLogin({RoleEnum.PHARMACY, RoleEnum.ADMIN})
    @Operation(summary = "预测列表", description = "历史预测列表")
    public Result<Map<String, Object>> getForecastList(
            @RequestParam(required = false) String forecastType,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return stockFeignClient.getForecastList(forecastType, page, size);
    }

    /**
     * 流式生成库存预测报告
     */
    @PostMapping(value = "/stock-forecast/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequireLogin({RoleEnum.PHARMACY, RoleEnum.ADMIN})
    @Operation(summary = "流式生成库存预测报告", description = "通过 SSE 逐字返回库存预测分析内容")
    public SseEmitter streamForecast(@RequestBody Map<String, Object> request) {
        SseEmitter emitter = new SseEmitter(120_000L);
        new Thread(() -> {
            try {
                Map<String, Object> data;
                if (request.get("medicines") != null) {
                    data = request;
                    data.put("forecastData", JSONUtil.toJsonStr(request.get("medicines")));
                } else {
                    Result<Map<String, Object>> generated = stockFeignClient.generateForecast(request);
                    data = generated.getData();
                }
                if (data == null) {
                    emitter.send(SseEmitter.event().name("error").data("库存预测生成失败，请确认 8081 AI 服务已启动"));
                    emitter.complete();
                    return;
                }

                String text = buildStreamForecastText(data);
                for (int i = 0; i < text.length(); i++) {
                    emitter.send(SseEmitter.event().name("delta").data(String.valueOf(text.charAt(i))));
                    Thread.sleep(12L);
                }
                emitter.send(SseEmitter.event().name("done").data(String.valueOf(data.get("id"))));
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage() == null ? "库存预测流式生成失败" : e.getMessage()));
                } catch (Exception ignored) {
                }
                emitter.completeWithError(e);
            }
        }, "ai-stock-forecast-stream").start();
        return emitter;
    }

    private String buildStreamForecastText(Map<String, Object> data) {
        String period = String.valueOf(data.getOrDefault("forecastPeriod", "未来1个月"));
        String forecastData = String.valueOf(data.getOrDefault("forecastData", "[]"));
        JSONArray rows;
        try {
            rows = JSONUtil.parseArray(forecastData);
        } catch (Exception e) {
            rows = new JSONArray();
        }

        int highRisk = 0;
        int needBuy = 0;
        int overStock = 0;
        StringBuilder purchase = new StringBuilder();
        for (Object item : rows) {
            if (!(item instanceof JSONObject row)) {
                continue;
            }
            String risk = row.getStr("riskLevel", "库存合理");
            int suggestPurchase = row.getInt("suggestPurchase", 0);
            if ("高风险".equals(risk)) highRisk++;
            if ("需补货".equals(risk)) needBuy++;
            if ("可能积压".equals(risk)) overStock++;
            if (suggestPurchase > 0) {
                purchase.append("- ")
                        .append(row.getStr("name", "未命名药品"))
                        .append("：建议采购 ")
                        .append(suggestPurchase)
                        .append(row.getStr("unit", ""))
                        .append("，当前库存 ")
                        .append(row.getInt("currentStock", 0))
                        .append(row.getStr("unit", ""))
                        .append("，风险：")
                        .append(risk)
                        .append("\n");
            }
        }

        StringBuilder builder = new StringBuilder();
        builder.append("一、库存预测摘要\n");
        builder.append("预测周期：").append(period).append("。\n");
        builder.append("本次共分析 ").append(rows.size()).append(" 种药品，结合历史处方消耗、当前库存、安全库存和季节性就诊波动生成采购建议。\n\n");
        builder.append("二、风险分布\n");
        builder.append("- 高风险缺货药品：").append(highRisk).append(" 种\n");
        builder.append("- 需要补货药品：").append(needBuy).append(" 种\n");
        builder.append("- 可能积压药品：").append(overStock).append(" 种\n\n");
        builder.append("三、重点采购建议\n");
        builder.append(purchase.length() == 0 ? "- 当前暂无必须采购药品，可维持常规库存巡检。\n" : purchase);
        builder.append("\n四、药房处理建议\n");
        builder.append("优先处理高风险缺货药品；对慢病、呼吸系统、抗感染类药品保留安全库存；对可能积压药品暂停大批量采购，并结合近期门诊量动态复核。");
        return builder.toString();
    }
}
