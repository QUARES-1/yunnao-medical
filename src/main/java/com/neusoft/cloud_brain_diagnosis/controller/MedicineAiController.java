package com.neusoft.cloud_brain_diagnosis.controller;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.neusoft.cloud_brain_diagnosis.common.annotation.RequireLogin;
import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import com.neusoft.cloud_brain_diagnosis.entity.StockForecast;
import com.neusoft.cloud_brain_diagnosis.service.ai.AiStockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/medicine/ai")
@RequiredArgsConstructor
@Tag(name = "AI库存预测", description = "AI药品库存智能预测模块")
public class MedicineAiController {

    private final AiStockService stockService;

    /**
     * 生成库存预测
     */
    @PostMapping("/stock-forecast/generate")
    @RequireLogin({RoleEnum.PHARMACY, RoleEnum.ADMIN})
    @Operation(summary = "生成库存预测", description = "生成月度/周度库存预测和采购建议")
    public Result<Map<String, Object>> generateForecast(
            @RequestBody(required = false) Map<String, Object> request,
            @RequestParam(required = false) String forecastType,
            @RequestParam(required = false) String forecastPeriod) {
        Map<String, Object> body = request == null ? Map.of() : request;
        String type = forecastType != null ? forecastType : String.valueOf(body.getOrDefault("forecastType", "monthly"));
        String period = forecastPeriod != null ? forecastPeriod : (body.get("forecastPeriod") == null ? null : String.valueOf(body.get("forecastPeriod")));
        return Result.success(stockService.generateStockForecast(type, period));
    }

    /**
     * 预测详情
     */
    @GetMapping("/stock-forecast/{id}")
    @RequireLogin({RoleEnum.PHARMACY, RoleEnum.ADMIN})
    @Operation(summary = "预测详情", description = "查看库存预测详情")
    public Result<Map<String, Object>> getForecastDetail(@PathVariable Long id) {
        StockForecast forecast = stockService.getForecastDetail(id);
        if (forecast == null) {
            return Result.error("库存预测记录不存在");
        }
        return Result.success(toForecastMap(forecast));
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
        return Result.success(toPageMap(stockService.getForecastList(forecastType, page, size)));
    }

    /**
     * 流式生成库存预测报告
     */
    @PostMapping(value = "/stock-forecast/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequireLogin({RoleEnum.PHARMACY, RoleEnum.ADMIN})
    @Operation(summary = "流式生成库存预测报告", description = "通过 SSE 逐字返回库存预测分析内容")
    public SseEmitter streamForecast(
            @RequestBody(required = false) Map<String, Object> request,
            @RequestParam(required = false) String forecastType,
            @RequestParam(required = false) String forecastPeriod) {
        SseEmitter emitter = new SseEmitter(120_000L);
        new Thread(() -> {
            try {
                Map<String, Object> body = request == null ? Map.of() : request;
                Map<String, Object> data;
                if (body.get("medicines") != null) {
                    data = new LinkedHashMap<>(body);
                    data.put("forecastData", JSONUtil.toJsonStr(body.get("medicines")));
                } else {
                    String type = forecastType != null ? forecastType : String.valueOf(body.getOrDefault("forecastType", "monthly"));
                    String period = forecastPeriod != null ? forecastPeriod : (body.get("forecastPeriod") == null ? null : String.valueOf(body.get("forecastPeriod")));
                    data = stockService.generateStockForecast(type, period);
                }
                if (data == null) {
                    emitter.send(SseEmitter.event().name("error").data("库存预测生成失败，请检查药品和处方数据"));
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

    private Map<String, Object> toPageMap(Page<StockForecast> page) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", page.getContent().stream().map(this::toForecastMap).toList());
        result.put("records", page.getContent().stream().map(this::toForecastMap).toList());
        result.put("list", page.getContent().stream().map(this::toForecastMap).toList());
        result.put("total", page.getTotalElements());
        result.put("page", page.getNumber() + 1);
        result.put("size", page.getSize());
        result.put("pages", page.getTotalPages());
        return result;
    }

    private Map<String, Object> toForecastMap(StockForecast forecast) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", forecast.getId());
        map.put("forecastType", forecast.getForecastType());
        map.put("forecastPeriod", forecast.getForecastPeriod());
        map.put("medicineId", forecast.getMedicineId());
        map.put("categoryId", forecast.getCategoryId());
        map.put("forecastData", forecast.getForecastData());
        map.put("purchaseSuggestions", forecast.getPurchaseSuggestions());
        map.put("totalForecastAmount", forecast.getTotalForecastAmount());
        map.put("totalPurchaseAmount", forecast.getTotalPurchaseAmount());
        map.put("factors", forecast.getFactors());
        map.put("rawResponse", forecast.getRawResponse());
        map.put("createTime", forecast.getCreateTime());
        return map;
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
