package com.neusoft.ai.service.ai.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.neusoft.ai.common.util.AiApiUtil;
import com.neusoft.ai.entity.StockForecast;
import com.neusoft.ai.repository.StockForecastRepository;
import com.neusoft.ai.service.ai.AiStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AiStockServiceImpl implements AiStockService {

    private final StockForecastRepository forecastRepository;
    private final AiApiUtil aiApiUtil;

    @Override
    @Transactional
    public Map<String, Object> generateStockForecast(String forecastType, String forecastPeriod) {
        if (forecastPeriod == null || forecastPeriod.isBlank()) {
            forecastPeriod = LocalDate.now().plusMonths(1).toString().substring(0, 7);
        }

        JSONArray forecastData = new JSONArray();
        JSONObject sample = new JSONObject();
        sample.set("medicineId", 1);
        sample.set("name", "阿莫西林胶囊");
        sample.set("specification", "0.5g*24粒");
        sample.set("currentStock", 120);
        sample.set("forecastConsume", 85);
        sample.set("suggestPurchase", 30);
        sample.set("riskLevel", "需补货");
        forecastData.add(sample);

        String prompt = "预测周期：" + forecastPeriod + "。请给出采购建议。";
        String systemPrompt = "你是医院药房库存预测专家，请基于系统数据进行分析。"
                + "以严格JSON返回：{\"purchaseSuggestions\":[\"...\"],\"factors\":[\"...\"]}。";
        String aiResponse = aiApiUtil.callAi(prompt, systemPrompt);

        StockForecast forecast = new StockForecast();
        forecast.setForecastType(forecastType == null ? "monthly" : forecastType);
        forecast.setForecastPeriod(forecastPeriod);
        forecast.setForecastData(forecastData.toString());
        try {
            JSONObject json = JSONUtil.parseObj(aiResponse);
            if (json.getJSONArray("purchaseSuggestions") != null)
                forecast.setPurchaseSuggestions(json.getJSONArray("purchaseSuggestions").toString());
            if (json.getJSONArray("factors") != null)
                forecast.setFactors(json.getJSONArray("factors").toString());
        } catch (Exception ignored) {}
        if (forecast.getPurchaseSuggestions() == null) {
            forecast.setPurchaseSuggestions("[\"优先采购高风险与需补货药品\"]");
        }
        if (forecast.getFactors() == null) {
            forecast.setFactors("[\"近30天处方消耗\",\"当前库存与安全库存\"]");
        }
        forecast.setTotalForecastAmount(BigDecimal.valueOf(50000));
        forecast.setTotalPurchaseAmount(BigDecimal.valueOf(15000));
        forecast.setRawResponse(aiResponse);
        forecastRepository.save(forecast);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", forecast.getId());
        result.put("forecastType", forecast.getForecastType());
        result.put("forecastPeriod", forecastPeriod);
        result.put("medicineCount", 1);
        result.put("createTime", forecast.getCreateTime());
        return result;
    }

    @Override
    public StockForecast getForecastDetail(Long id) {
        return forecastRepository.findById(id).orElse(null);
    }

    @Override
    public Page<StockForecast> getForecastList(String forecastType, Integer page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        if (forecastType != null && !forecastType.isEmpty()) {
            return forecastRepository.findByForecastTypeOrderByCreateTimeDesc(forecastType, pageRequest);
        }
        return forecastRepository.findByOrderByCreateTimeDesc(pageRequest);
    }
}
