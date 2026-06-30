package com.neusoft.ai.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "follow_up_record")
public class FollowUpRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long planId;
    private Long patientId;
    private LocalDateTime followUpTime;
    private String questionnaireJson;
    private String answerJson;
    private String status;
    private Integer abnormalFlag;
    private String aiAnalysis;
    private String doctorRemark;
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
