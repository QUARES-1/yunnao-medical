package com.neusoft.cloud_brain_diagnosis.service.ai.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.neusoft.cloud_brain_diagnosis.common.util.AiApiUtil;
import com.neusoft.cloud_brain_diagnosis.entity.StockForecast;
import com.neusoft.cloud_brain_diagnosis.repository.StockForecastRepository;
import com.neusoft.cloud_brain_diagnosis.service.ai.AiStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AiStockServiceImpl implements AiStockService {

    private final StockForecastRepository forecastRepository;
    private final AiApiUtil aiApiUtil;

    @Override
    @Transactional
    public Map<String, Object> generateStockForecast(String forecastType, String forecastPeriod) {
        if (forecastPeriod == null) {
            forecastPeriod = java.time.LocalDate.now().plusMonths(1).toString().substring(0, 7);
        }

        String prompt = "请预测" + forecastPeriod + "的药品消耗量和采购建议";
        String systemPrompt = "你是一名药房库存管理专家，请基于历史数据和季节因素预测药品消耗。"
                + "请按JSON格式返回："
                + "{\"forecastData\":[{\"medicineId\":1,\"name\":\"阿莫西林胶囊\",\"currentStock\":200,\"forecastConsume\":350,\"suggestPurchase\":200,\"unit\":\"盒\"}],"
                + "\"purchaseSuggestions\":[\"建议1\",\"建议2\"],"
                + "\"factors\":[\"因素1\",\"因素2\"],"
                + "\"totalForecastAmount\":85600.00,"
                + "\"totalPurchaseAmount\":42300.00}";

        String aiResponse = aiApiUtil.callAi(prompt, systemPrompt);

        StockForecast forecast = new StockForecast();
        forecast.setForecastType(forecastType);
        forecast.setForecastPeriod(forecastPeriod);

        try {
            JSONObject json = JSONUtil.parseObj(aiResponse);
            if (json.getJSONArray("forecastData") != null) {
                forecast.setForecastData(json.getJSONArray("forecastData").toString());
            }
            if (json.getJSONArray("purchaseSuggestions") != null) {
                forecast.setPurchaseSuggestions(json.getJSONArray("purchaseSuggestions").toString());
            }
            if (json.getJSONArray("factors") != null) {
                forecast.setFactors(json.getJSONArray("factors").toString());
            }
            forecast.setTotalForecastAmount(json.getBigDecimal("totalForecastAmount", BigDecimal.ZERO));
            forecast.setTotalPurchaseAmount(json.getBigDecimal("totalPurchaseAmount", BigDecimal.ZERO));
        } catch (Exception ignored) {}

        forecast.setRawResponse(aiResponse);
        forecastRepository.save(forecast);

        Map<String, Object> result = new HashMap<>();
        result.put("id", forecast.getId());
        result.put("forecastType", forecastType);
        result.put("forecastPeriod", forecastPeriod);
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
