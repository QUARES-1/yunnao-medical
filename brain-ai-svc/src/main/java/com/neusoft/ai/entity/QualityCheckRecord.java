package com.neusoft.ai.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "quality_check_record")
public class QualityCheckRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String checkType;
    private LocalDateTime checkDate;
    private Integer totalCount;
    private Integer passCount;
    private java.math.BigDecimal avgScore;
    private String problemSummary;
    private String improvementSuggestions;
    private String checkerType;
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
