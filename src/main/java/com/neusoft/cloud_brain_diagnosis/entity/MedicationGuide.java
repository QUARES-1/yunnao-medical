package com.neusoft.cloud_brain_diagnosis.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "medication_guide")
@Data
public class MedicationGuide {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long prescriptionId;

    private Long patientId;

    private Integer patientAge;

    @Column(length = 10)
    private String patientGender;

    @Column(columnDefinition = "TEXT")
    private String drugsJson;

    @Column(columnDefinition = "TEXT")
    private String guideContent;

    private Integer printCount;

    @Column(columnDefinition = "TEXT")
    private String rawResponse;

    @Column(updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        if (printCount == null) printCount = 0;
    }
}
