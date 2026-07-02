package com.neusoft.ai.repository;

import com.neusoft.ai.entity.StockForecast;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockForecastRepository extends JpaRepository<StockForecast, Long> {
    Page<StockForecast> findByForecastTypeOrderByCreateTimeDesc(String forecastType, Pageable pageable);
    Page<StockForecast> findByOrderByCreateTimeDesc(Pageable pageable);
    Optional<StockForecast> findTopByForecastTypeAndForecastPeriodOrderByCreateTimeDesc(
            String forecastType, String forecastPeriod);
}
