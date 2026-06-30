package com.neusoft.ai.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "prescription_ai_review")
public class PrescriptionAiReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long doctorId;
    private Long patientId;
    private Integer patientAge;
    private String patientGender;
    private String drugsJson;
    private String reviewResult;
    private Integer reviewScore;
    private String warnings;
    private String suggestions;
    private String drugInteractions;
    private String allergyRisks;
    private String dosageIssues;
    private String rawResponse;
    private LocalDateTime reviewTime;

    @PrePersist
    protected void onCreate() {
        reviewTime = LocalDateTime.now();
    }
}
