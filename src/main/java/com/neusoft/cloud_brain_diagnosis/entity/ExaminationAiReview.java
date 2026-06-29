package com.neusoft.cloud_brain_diagnosis.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "examination_ai_review")
@Data
public class ExaminationAiReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long examinationId;

    private Long patientId;

    private Long labStaffId;

    @Column(length = 20)
    private String reviewResult;

    private Integer reviewScore;

    @Column(columnDefinition = "TEXT")
    private String abnormalItems;

    @Column(columnDefinition = "TEXT")
    private String logicIssues;

    @Column(columnDefinition = "TEXT")
    private String historyCompare;

    @Column(columnDefinition = "TEXT")
    private String warnings;

    @Column(columnDefinition = "TEXT")
    private String suggestions;

    @Column(columnDefinition = "TEXT")
    private String rawResponse;

    @Column(updatable = false)
    private LocalDateTime reviewTime;

    @PrePersist
    protected void onCreate() {
        reviewTime = LocalDateTime.now();
    }
}
