package com.neusoft.cloud_brain_diagnosis.repository;

import com.neusoft.cloud_brain_diagnosis.entity.StockForecast;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockForecastRepository extends JpaRepository<StockForecast, Long> {
    Page<StockForecast> findByOrderByCreateTimeDesc(Pageable pageable);
    Page<StockForecast> findByForecastTypeOrderByCreateTimeDesc(String forecastType, Pageable pageable);
}
