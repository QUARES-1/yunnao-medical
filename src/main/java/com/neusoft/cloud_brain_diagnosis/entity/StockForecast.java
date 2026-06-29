package com.neusoft.cloud_brain_diagnosis.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_forecast")
@Data
public class StockForecast {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20)
    private String forecastType;

    @Column(length = 50)
    private String forecastPeriod;

    private Long medicineId;

    private Long categoryId;

    @Column(columnDefinition = "TEXT")
    private String forecastData;

    @Column(columnDefinition = "TEXT")
    private String purchaseSuggestions;

    @Column(precision = 12, scale = 2)
    private BigDecimal totalForecastAmount;

    @Column(precision = 12, scale = 2)
    private BigDecimal totalPurchaseAmount;

    @Column(columnDefinition = "TEXT")
    private String factors;

    @Column(columnDefinition = "TEXT")
    private String rawResponse;

    @Column(updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
