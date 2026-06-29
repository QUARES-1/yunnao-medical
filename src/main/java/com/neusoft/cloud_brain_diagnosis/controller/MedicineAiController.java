package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.annotation.RequireLogin;
import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import com.neusoft.cloud_brain_diagnosis.entity.StockForecast;
import com.neusoft.cloud_brain_diagnosis.service.ai.AiStockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

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
    public Result<Map<String, Object>> generateForecast(@RequestBody Map<String, Object> request) {
        String forecastType = (String) request.getOrDefault("forecastType", "monthly");
        String forecastPeriod = (String) request.get("forecastPeriod");
        return Result.success(stockService.generateStockForecast(forecastType, forecastPeriod));
    }

    /**
     * 预测详情
     */
    @GetMapping("/stock-forecast/{id}")
    @RequireLogin({RoleEnum.PHARMACY, RoleEnum.ADMIN})
    @Operation(summary = "预测详情", description = "查看库存预测详情")
    public Result<StockForecast> getForecastDetail(@PathVariable Long id) {
        return Result.success(stockService.getForecastDetail(id));
    }

    /**
     * 预测列表
     */
    @GetMapping("/stock-forecast/list")
    @RequireLogin({RoleEnum.PHARMACY, RoleEnum.ADMIN})
    @Operation(summary = "预测列表", description = "历史预测列表")
    public Result<Page<StockForecast>> getForecastList(
            @RequestParam(required = false) String forecastType,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(stockService.getForecastList(forecastType, page, size));
    }
}
