package com.neusoft.ai.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "operation_ai_report")
public class OperationAiReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String reportType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String summary;
    @Column(columnDefinition = "TEXT")
    private String keyMetrics;
    private String trendsAnalysis;
    private String forecasts;
    private String warnings;
    private String suggestions;
    private String rawResponse;
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
