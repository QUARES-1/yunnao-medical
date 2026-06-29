package com.neusoft.cloud_brain_diagnosis.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "follow_up_record")
@Data
public class FollowUpRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long planId;

    private Long patientId;

    private LocalDateTime followUpTime;

    @Column(columnDefinition = "TEXT")
    private String questionnaireJson;

    @Column(columnDefinition = "TEXT")
    private String answerJson;

    @Column(columnDefinition = "TEXT")
    private String aiAnalysis;

    @Column(length = 20)
    private String status;

    private Integer abnormalFlag;

    @Column(length = 500)
    private String doctorRemark;

    @Column(updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        if (status == null) status = "pending";
        if (abnormalFlag == null) abnormalFlag = 0;
    }
}
