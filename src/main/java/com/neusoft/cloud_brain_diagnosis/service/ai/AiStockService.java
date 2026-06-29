package com.neusoft.cloud_brain_diagnosis.service.ai;

import com.neusoft.cloud_brain_diagnosis.entity.StockForecast;
import org.springframework.data.domain.Page;

import java.util.Map;

public interface AiStockService {
    /**
     * 生成库存预测
     */
    Map<String, Object> generateStockForecast(String forecastType, String forecastPeriod);

    /**
     * 预测详情
     */
    StockForecast getForecastDetail(Long id);

    /**
     * 预测列表
     */
    Page<StockForecast> getForecastList(String forecastType, Integer page, Integer size);
}
