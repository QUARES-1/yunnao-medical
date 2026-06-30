package com.neusoft.ai.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "examination_ai_review")
public class ExaminationAiReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long examinationId;
    private Long doctorId;
    private String reviewResult;
    private Integer reviewScore;
    private String warnings;
    private String suggestions;
    private String rawResponse;
    private String status;
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
