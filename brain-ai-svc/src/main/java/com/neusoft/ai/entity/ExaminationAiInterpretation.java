package com.neusoft.ai.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "examination_ai_interpretation")
public class ExaminationAiInterpretation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long examinationId;
    private Long patientId;
    private String interpretation;
    private String keyFindings;
    private String suggestions;
    private String rawResponse;
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
