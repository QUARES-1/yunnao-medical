package com.neusoft.cloud_brain_diagnosis.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "prescription_ai_review")
@Data
public class PrescriptionAiReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long prescriptionId;

    private Long doctorId;

    private Long patientId;

    private Integer patientAge;

    @Column(length = 10)
    private String patientGender;

    @Column(precision = 5, scale = 2)
    private BigDecimal patientWeight;

    @Column(columnDefinition = "TEXT")
    private String drugsJson;

    @Column(length = 20)
    private String reviewResult;

    private Integer reviewScore;

    @Column(columnDefinition = "TEXT")
    private String warnings;

    @Column(columnDefinition = "TEXT")
    private String suggestions;

    @Column(columnDefinition = "TEXT")
    private String drugInteractions;

    @Column(columnDefinition = "TEXT")
    private String allergyRisks;

    @Column(columnDefinition = "TEXT")
    private String dosageIssues;

    @Column(columnDefinition = "TEXT")
    private String rawResponse;

    @Column(updatable = false)
    private LocalDateTime reviewTime;

    @PrePersist
    protected void onCreate() {
        reviewTime = LocalDateTime.now();
    }
}
