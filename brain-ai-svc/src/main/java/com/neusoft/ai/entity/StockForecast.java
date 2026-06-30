package com.neusoft.ai.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "stock_forecast")
public class StockForecast {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String forecastType;
    private String forecastPeriod;
    @Column(columnDefinition = "TEXT")
    private String forecastData;
    private String purchaseSuggestions;
    private String factors;
    private BigDecimal totalForecastAmount;
    private BigDecimal totalPurchaseAmount;
    private String rawResponse;
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
