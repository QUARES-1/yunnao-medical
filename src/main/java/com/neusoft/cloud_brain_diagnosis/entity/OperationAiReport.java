package com.neusoft.cloud_brain_diagnosis.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "operation_ai_report")
@Data
public class OperationAiReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String reportType;

    private LocalDate startDate;

    private LocalDate endDate;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String keyMetrics;

    @Column(columnDefinition = "TEXT")
    private String trendsAnalysis;

    @Column(columnDefinition = "TEXT")
    private String forecasts;

    @Column(columnDefinition = "TEXT")
    private String warnings;

    @Column(columnDefinition = "TEXT")
    private String suggestions;

    @Column(columnDefinition = "TEXT")
    private String rawResponse;

    @Column(updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
