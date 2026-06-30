package com.neusoft.ai.service.ai;

import com.neusoft.ai.entity.StockForecast;
import org.springframework.data.domain.Page;
import java.util.Map;

public interface AiStockService {
    Map<String, Object> generateStockForecast(String forecastType, String forecastPeriod);
    StockForecast getForecastDetail(Long id);
    Page<StockForecast> getForecastList(String forecastType, Integer page, Integer size);
}
