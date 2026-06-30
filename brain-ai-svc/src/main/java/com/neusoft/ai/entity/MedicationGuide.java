package com.neusoft.ai.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "medication_guide")
public class MedicationGuide {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long prescriptionId;
    private Long patientId;
    private String medications;
    @Column(columnDefinition = "TEXT")
    private String generalAdvice;
    private String aiAnalysis;
    private String rawResponse;
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
