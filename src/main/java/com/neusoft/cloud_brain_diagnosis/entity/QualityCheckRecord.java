package com.neusoft.cloud_brain_diagnosis.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "quality_check_record")
@Data
public class QualityCheckRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20)
    private String checkType;

    private LocalDateTime checkDate;

    private Integer totalCount;

    private Integer passCount;

    @Column(precision = 5, scale = 2)
    private BigDecimal avgScore;

    @Column(columnDefinition = "TEXT")
    private String problemSummary;

    @Column(columnDefinition = "TEXT")
    private String improvementSuggestions;

    @Column(length = 20)
    private String checkerType;

    @Column(updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        if (checkerType == null) checkerType = "ai";
    }
}
