package com.neusoft.cloud_brain_diagnosis.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "examination_ai_interpretation")
@Data
public class ExaminationAiInterpretation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long examinationId;

    private Long patientId;

    @Column(columnDefinition = "TEXT")
    private String abnormalItems;

    @Column(columnDefinition = "TEXT")
    private String interpretationPro;

    @Column(columnDefinition = "TEXT")
    private String interpretationPatient;

    @Column(columnDefinition = "TEXT")
    private String suggestions;

    @Column(length = 500)
    private String reviewReminder;

    @Column(columnDefinition = "TEXT")
    private String rawResponse;

    @Column(updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
